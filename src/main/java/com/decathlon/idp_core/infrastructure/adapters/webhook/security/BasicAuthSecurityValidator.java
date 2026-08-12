package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;

import lombok.NoArgsConstructor;

/// Basic Authentication security strategy for webhooks.
///
/// Validates Basic Auth credentials at both creation time (configuration validation)
/// and runtime (request authentication).
@Component
@NoArgsConstructor
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
    String expectedUsername = WebhookSecurityConfigurationUtils.required(config, "username");
    String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias",
        "secretAlias");
    String expectedPassword = WebhookSecurityConfigurationUtils.resolveRuntimeSecret(alias);

    String authorization = WebhookSecurityConfigurationUtils.requiredHeader(headers,
        "Authorization");
    if (!authorization.startsWith("Basic ")) {
      return false;
    }

    String encodedCredentials = authorization.substring("Basic ".length()).trim();
    String decodedCredentials;
    try {
      decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials),
          StandardCharsets.UTF_8);
    } catch (IllegalArgumentException exception) {
      return false;
    }

    int separatorIndex = decodedCredentials.indexOf(':');
    if (separatorIndex <= 0) {
      return false;
    }

    String actualUsername = decodedCredentials.substring(0, separatorIndex);
    String actualPassword = decodedCredentials.substring(separatorIndex + 1);

    return WebhookSecurityConfigurationUtils.constantTimeEquals(expectedUsername, actualUsername)
        && WebhookSecurityConfigurationUtils.constantTimeEquals(expectedPassword, actualPassword);
  }
}
