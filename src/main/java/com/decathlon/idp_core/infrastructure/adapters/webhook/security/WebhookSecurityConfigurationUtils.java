package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.util.Map;
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
  /// @throws WebhookAuthenticationException if no value is found (at runtime)
  public static String required(Map<String, String> config, String... keys) {
    return required(config, false, keys);
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
  /// (at creation time)
  public static void validateSecretAliasFormat(String alias) {
    String normalizedAlias = normalizeEnvironmentAlias(alias);
    if (!ENV_ALIAS.matcher(normalizedAlias).matches()) {
      throw new WebhookSecurityConfigurationException(
          "Invalid 'secret_alias'. Use UPPER_SNAKE_CASE or an environment reference (${MY_SECRET} or env:MY_SECRET)");
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

  private static String required(Map<String, String> config, boolean isRuntime, String... keys) {
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
    if (isRuntime) {
      throw new WebhookAuthenticationException(
          "Missing security config key. Expected one of: " + keysStr);
    } else {
      throw new WebhookSecurityConfigurationException(
          "Missing required security config key. Expected one of: " + keysStr);
    }
  }
}
