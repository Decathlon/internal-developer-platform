package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// Basic Authentication security strategy for webhooks.
///
/// Validates Basic Auth credentials at both creation time (configuration validation)
/// and runtime (request authentication).
@Component
@NoArgsConstructor
@Slf4j
public class BasicAuthSecurityValidator implements WebhookSecurityStrategy {

  private static final String USERNAME_KEY = "username";
  private static final String SECRET_ALIAS_KEY_SNAKE_CASE = "secret_alias";
  private static final String SECRET_ALIAS_KEY_CAMEL_CASE = "secretAlias";

  @Override
  public boolean supports(WebhookSecurityType securityType) {
    return WebhookSecurityType.BASIC_AUTH == securityType;
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    String configuredUsername = WebhookSecurityConfigurationUtils.required(config, USERNAME_KEY);
    if (WebhookSecurityConfigurationUtils.isEnvironmentReference(configuredUsername)) {
      WebhookSecurityConfigurationUtils.validateSecretAliasExists(configuredUsername);
    }

    String alias = WebhookSecurityConfigurationUtils.required(config, SECRET_ALIAS_KEY_SNAKE_CASE,
        SECRET_ALIAS_KEY_CAMEL_CASE);
    WebhookSecurityConfigurationUtils.validateSecretAliasFormat(alias);
    WebhookSecurityConfigurationUtils.validateSecretAliasExists(alias);
  }

  @Override
  public void validateRequest(Map<String, Object> headers, byte[] rawPayload,
      Map<String, String> config) {
    String expectedUsername = resolveExpectedUsername(config);
    String expectedPassword = WebhookSecurityConfigurationUtils.resolveRequiredRuntimeValue(config,
        SECRET_ALIAS_KEY_SNAKE_CASE, SECRET_ALIAS_KEY_CAMEL_CASE);

    // Extract Authorization header (may throw WebhookAuthenticationException)
    String authorization = WebhookSecurityConfigurationUtils.requiredHeader(headers,
        "Authorization");

    if (!authorization.startsWith("Basic ")) {
      log.debug("Basic Auth validation failed: Authorization header does not start with 'Basic '");
      throw new WebhookAuthUnauthorizedException(
          "Authorization header must use Basic authentication scheme");
    }

    String encodedCredentials = authorization.substring("Basic ".length()).trim();
    String decodedCredentials;
    try {
      decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials),
          StandardCharsets.UTF_8);
    } catch (IllegalArgumentException exception) {
      log.debug("Basic Auth validation failed: Invalid Base64 encoding in Authorization header");
      throw new WebhookAuthUnauthorizedException(
          "Authorization header contains invalid Basic credentials", exception);
    }

    int separatorIndex = decodedCredentials.indexOf(':');
    if (separatorIndex <= 0) {
      log.debug("Basic Auth validation failed: No ':' separator found in decoded credentials");
      throw new WebhookAuthUnauthorizedException(
          "Authorization header contains invalid Basic credentials format");
    }

    String actualUsername = decodedCredentials.substring(0, separatorIndex);
    String actualPassword = decodedCredentials.substring(separatorIndex + 1);

    boolean usernameMatches = WebhookSecurityConfigurationUtils.constantTimeEquals(expectedUsername,
        actualUsername);
    boolean passwordMatches = WebhookSecurityConfigurationUtils.constantTimeEquals(expectedPassword,
        actualPassword);

    if (!usernameMatches) {
      log.debug("Basic Auth validation failed: username mismatch");
      throw new WebhookAuthForbiddenException("Basic credentials were rejected");
    }

    if (!passwordMatches) {
      log.debug("Basic Auth validation failed: password mismatch");
      throw new WebhookAuthForbiddenException("Basic credentials were rejected");
    }

    log.debug("Basic Auth validation successful for username '{}'", expectedUsername);
  }

  private String resolveExpectedUsername(Map<String, String> config) {
    String configuredUsername = WebhookSecurityConfigurationUtils.required(config, USERNAME_KEY);
    if (WebhookSecurityConfigurationUtils.isEnvironmentReference(configuredUsername)) {
      return WebhookSecurityConfigurationUtils.resolveRuntimeSecret(configuredUsername);
    }

    return configuredUsername;
  }
}
