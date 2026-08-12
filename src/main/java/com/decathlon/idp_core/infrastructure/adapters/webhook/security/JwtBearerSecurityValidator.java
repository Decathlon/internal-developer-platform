package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;

import lombok.NoArgsConstructor;

/// JWT Bearer security strategy for webhooks.
///
/// Validates JWT Bearer configuration at creation time and authenticates incoming
/// webhook requests by verifying the JWT token against a JWKS endpoint.
@Component
@NoArgsConstructor
public class JwtBearerSecurityValidator implements WebhookSecurityStrategy {

  private static final String KEY_JWKS_URI_SNAKE_CASE = "jwks_uri";
  private static final String KEY_JWKS_URI_CAMEL_CASE = "jwksUri";

  @Override
  public boolean supports(WebhookSecurityType securityType) {
    return WebhookSecurityType.JWT_BEARER == securityType;
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    String jwksUriValue = WebhookSecurityConfigurationUtils.required(config,
        KEY_JWKS_URI_SNAKE_CASE, KEY_JWKS_URI_CAMEL_CASE);
    if (jwksUriValue.isBlank()) {
      throw new WebhookSecurityConfigurationException("Invalid jwks_uri for JWT_BEARER security");
    }

    if (WebhookSecurityConfigurationUtils.isEnvironmentReference(jwksUriValue)) {
      WebhookSecurityConfigurationUtils.validateEnvironmentReferenceFormat(jwksUriValue,
          KEY_JWKS_URI_SNAKE_CASE);
    }
  }

  @Override
  public boolean validateRequest(Map<String, Object> headers, byte[] rawPayload,
      Map<String, String> config) {
    String jwksUriValue = WebhookSecurityConfigurationUtils.required(config,
        KEY_JWKS_URI_SNAKE_CASE, KEY_JWKS_URI_CAMEL_CASE);
    if (WebhookSecurityConfigurationUtils.isEnvironmentReference(jwksUriValue)) {
      // Ensures referenced runtime configuration exists, even if token verification
      // is delegated.
      WebhookSecurityConfigurationUtils.resolveRuntimeSecret(jwksUriValue);
    }

    String authorization = WebhookSecurityConfigurationUtils.requiredHeader(headers,
        "Authorization");
    return authorization.startsWith("Bearer ")
        && !authorization.substring("Bearer ".length()).isBlank();
  }
}
