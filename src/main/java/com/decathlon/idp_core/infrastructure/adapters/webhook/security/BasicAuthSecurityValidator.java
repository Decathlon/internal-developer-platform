package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;

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

  @Override
  public boolean supports(WebhookSecurityType securityType) {
    return WebhookSecurityType.BASIC_AUTH == securityType;
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    WebhookSecurityConfigurationUtils.required(config, "username");
    String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias",
        "secretAlias");
    WebhookSecurityConfigurationUtils.validateSecretAliasFormat(alias);
  }

  @Override
  public boolean validateRequest(Map<String, Object> headers, byte[] rawPayload,
      Map<String, String> config) {
    try {
      // Resolve configuration and secrets (may throw WebhookAuthenticationException)
      String expectedUsername = WebhookSecurityConfigurationUtils.required(config, "username");
      String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias",
          "secretAlias");
      String expectedPassword = WebhookSecurityConfigurationUtils.resolveRuntimeSecret(alias);

      // Extract Authorization header (may throw WebhookAuthenticationException)
      String authorization = WebhookSecurityConfigurationUtils.requiredHeader(headers, "Authorization");

      if (!authorization.startsWith("Basic ")) {
        log.debug("Basic Auth validation failed: Authorization header does not start with 'Basic '");
        return false;
      }

      String encodedCredentials = authorization.substring("Basic ".length()).trim();
      String decodedCredentials;
      try {
        decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials),
            StandardCharsets.UTF_8);
      } catch (IllegalArgumentException exception) {
        log.debug("Basic Auth validation failed: Invalid Base64 encoding in Authorization header");
        return false;
      }

      int separatorIndex = decodedCredentials.indexOf(':');
      if (separatorIndex <= 0) {
        log.debug("Basic Auth validation failed: No ':' separator found in decoded credentials");
        return false;
      }

      String actualUsername = decodedCredentials.substring(0, separatorIndex);
      String actualPassword = decodedCredentials.substring(separatorIndex + 1);

      boolean usernameMatches = WebhookSecurityConfigurationUtils.constantTimeEquals(expectedUsername, actualUsername);
      boolean passwordMatches = WebhookSecurityConfigurationUtils.constantTimeEquals(expectedPassword, actualPassword);

      if (!usernameMatches) {
        log.debug("Basic Auth validation failed: username mismatch");
        return false;
      }

      if (!passwordMatches) {
        log.debug("Basic Auth validation failed: password mismatch");
        return false;
      }

      log.debug("Basic Auth validation successful for username '{}'", expectedUsername);
      return true;

    } catch (Exception e) {
      log.debug("Basic Auth validation failed: {}", e.getMessage());
      return false;
    }
  }
}
