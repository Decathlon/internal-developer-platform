package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.util.Map;

import org.springframework.stereotype.Component;

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
  public boolean supports(String securityType) {
    return "BASIC_AUTH".equalsIgnoreCase(securityType);
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    WebhookSecurityConfigurationUtils.required(config, "username");
    String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias",
        "secretAlias");
    WebhookSecurityConfigurationUtils.validateSecretAliasFormat(alias);
  }
}
