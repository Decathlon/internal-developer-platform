package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;

import lombok.NoArgsConstructor;

/// JWT Bearer security strategy for webhooks.
///
/// Validates JWT Bearer configuration at creation time and authenticates incoming
/// webhook requests by verifying the JWT token against a JWKS endpoint.
@Component
@NoArgsConstructor
public class JwtBearerSecurityValidator implements WebhookSecurityStrategy {

  @Override
  public boolean supports(String securityType) {
    return "JWT_BEARER".equalsIgnoreCase(securityType);
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    String jwksUriValue = WebhookSecurityConfigurationUtils.required(config, "jwks_uri", "jwksUri");
    if (jwksUriValue.isBlank()) {
      throw new WebhookSecurityConfigurationException("Invalid jwks_uri for JWT_BEARER security");
    }

    if (WebhookSecurityConfigurationUtils.isEnvironmentReference(jwksUriValue)) {
      WebhookSecurityConfigurationUtils.validateSecretAliasFormat(jwksUriValue);
    }
  }
}
