package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;

/// Common utilities for webhook security validation.
///
/// Provides shared methods for extracting and validating configuration keys across all security strategies.
/// This eliminates duplication between creation-time and runtime validation logic.
public final class WebhookSecurityConfigurationUtils {

  private static final Pattern BRACED_ENV_REFERENCE = Pattern.compile("^\\$\\{([A-Z0-9_]+)}$");
  private static final Pattern ENV_ALIAS = Pattern.compile("^[A-Z0-9_]+$");

  private WebhookSecurityConfigurationUtils() {
  }

  /// Retrieves a required configuration value, checking multiple key variants
  /// (snake_case and camelCase).
  ///
  /// @param config the configuration map
  /// @param keys the keys to check in order (e.g., "secret_alias", "secretAlias")
  /// @return the first non-blank value found
  /// @throws WebhookSecurityConfigurationException if no value is found (at
  /// creation time)
  public static String required(Map<String, String> config, String... keys) {
    return requiredInternal(config, keys);
  }

  /// Retrieves an optional configuration value, returning a default if not found.
  ///
  /// @param config the configuration map
  /// @param key the key to look up
  /// @param defaultValue the value to return if key is not found
  /// @return the configuration value or the default
  public static String optional(Map<String, String> config, String key, String defaultValue) {
    String value = config.get(key);
    return value == null ? defaultValue : value;
  }

  /// Validates that a secret alias follows the UPPER_SNAKE_CASE convention.
  ///
  /// @param alias the alias to validate
  /// @throws WebhookSecurityConfigurationException if the alias format is invalid
  public static void validateSecretAliasFormat(String alias) {
    validateEnvironmentReferenceFormat(alias, "secret_alias");
  }

  /// Validates that the environment variable referenced by the alias actually
  /// exists
  /// at connector creation time. Rejects the configuration if the variable is
  /// absent
  /// or blank, preventing silent failures at runtime.
  ///
  /// @param alias the secret alias (plain `MY_VAR`, `env:MY_VAR`, or `${MY_VAR}`)
  /// @throws WebhookSecurityConfigurationException if the environment variable is
  /// missing or blank
  public static void validateSecretAliasExists(String alias) {
    String envKey = normalizeEnvironmentAlias(alias);
    if (!StringUtils.hasText(envKey)) {
      throw new WebhookSecurityConfigurationException("Secret alias is missing or blank");
    }

    String resolved = System.getenv(envKey);
    if (!StringUtils.hasText(resolved)) {
      resolved = System.getProperty(envKey);
    }
    if (!StringUtils.hasText(resolved)) {
      throw new WebhookSecurityConfigurationException(
          ("Environment variable '%s' referenced by secret_alias is not set. "
              + "Ensure the variable is defined before creating this connector.")
                  .formatted(envKey));
    }
  }

  /// Validates that an environment reference follows the UPPER_SNAKE_CASE
  /// convention.
  ///
  /// @param value the value to validate
  /// @param fieldName the field name to include in error messages for clarity
  /// @throws WebhookSecurityConfigurationException if the format is invalid
  public static void validateEnvironmentReferenceFormat(String value, String fieldName) {
    String normalizedAlias = normalizeEnvironmentAlias(value);
    if (!ENV_ALIAS.matcher(normalizedAlias).matches()) {
      throw new WebhookSecurityConfigurationException(
          "Invalid '%s'. Use UPPER_SNAKE_CASE".formatted(fieldName));
    }
  }

  /// Determines whether a configuration value references an environment variable.
  ///
  /// Supported formats: `${MY_VAR}` and `env:MY_VAR`.
  ///
  /// @param value configuration value
  /// @return true when value is an environment variable reference
  public static boolean isEnvironmentReference(String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    String trimmed = value.trim();
    return trimmed.startsWith("env:") || BRACED_ENV_REFERENCE.matcher(trimmed).matches();
  }

  /// Resolves a configured alias/reference to a runtime secret value from
  /// environment variables.
  ///
  /// @param aliasOrReference alias format supported: `MY_VAR`, `${MY_VAR}`,
  /// `env:MY_VAR`
  /// @return resolved secret value
  /// @throws WebhookAuthenticationException if the environment variable is
  /// missing or blank
  public static String resolveRuntimeSecret(String aliasOrReference) {
    String envKey = normalizeEnvironmentAlias(aliasOrReference);
    if (!StringUtils.hasText(envKey)) {
      throw new WebhookAuthenticationException("Security secret alias is missing");
    }

    String resolved = System.getenv(envKey);
    if (!StringUtils.hasText(resolved)) {
      // Test-friendly fallback: allows setting runtime secrets through JVM system
      // properties.
      resolved = System.getProperty(envKey);
    }
    if (!StringUtils.hasText(resolved)) {
      throw new WebhookAuthenticationException(
          "Environment variable or system property '%s' is missing or blank for webhook authentication"
              .formatted(envKey));
    }
    return resolved;
  }

  /// Reads an inbound HTTP header value in a case-insensitive way.
  ///
  /// HTTP/1.1 headers are case-insensitive (RFC 7230). Gateways and proxies may
  /// normalize header names to lowercase before forwarding, so case-insensitive
  /// matching is required for reliable interoperability.
  ///
  /// @param headers inbound headers
  /// @param headerName target header name
  /// @return header value as string
  /// @throws WebhookAuthenticationException when header is missing
  public static String requiredHeader(Map<String, Object> headers, String headerName) {
    if (headers == null || headers.isEmpty()) {
      throw new WebhookAuthenticationException(
          "Missing required header '%s' for webhook authentication".formatted(headerName));
    }

    return headers.entrySet().stream()
        .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName))
        .map(Map.Entry::getValue).filter(Objects::nonNull).map(Object::toString)
        .filter(StringUtils::hasText).findFirst()
        .orElseThrow(() -> new WebhookAuthenticationException(
            "Missing required header '%s' for webhook authentication".formatted(headerName)));
  }

  /// Resolves a required runtime value from configuration.
  ///
  /// This helper combines required key lookup (snake/camel + _env/Env aliases)
  /// and runtime environment resolution in one call.
  public static String resolveRequiredRuntimeValue(Map<String, String> config, String... keys) {
    String aliasOrReference = required(config, keys);
    return resolveRuntimeSecret(aliasOrReference);
  }

  /// Constant-time string comparison to reduce timing attack signal.
  public static boolean constantTimeEquals(String left, String right) {
    if (left == null || right == null) {
      return false;
    }
    return java.security.MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),
        right.getBytes(StandardCharsets.UTF_8));
  }

  private static String normalizeEnvironmentAlias(String aliasOrReference) {
    if (!StringUtils.hasText(aliasOrReference)) {
      return aliasOrReference;
    }

    String trimmed = aliasOrReference.trim();
    if (trimmed.startsWith("env:")) {
      return trimmed.substring("env:".length()).trim();
    }

    Matcher matcher = BRACED_ENV_REFERENCE.matcher(trimmed);
    if (matcher.matches()) {
      return matcher.group(1);
    }

    return trimmed;
  }

  private static String requiredInternal(Map<String, String> config, String... keys) {
    for (String key : keys) {
      String value = config.get(key);
      if (StringUtils.hasText(value)) {
        return value;
      }

      // Priority 2: Environment suffix support (_env or Env)
      // Example: if key is "secret_alias", we also check for "secret_alias_env" or
      // "secret_aliasEnv"
      String envValue = config.get(key + "_env");
      if (!StringUtils.hasText(envValue)) {
        envValue = config.get(key + "Env");
      }

      if (StringUtils.hasText(envValue)) {
        return "env:" + envValue;
      }
    }

    String keysStr = String.join(", ", keys);
    throw new WebhookSecurityConfigurationException(
        "Missing required security config key. Expected one of: " + keysStr);
  }
}
