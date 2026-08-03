package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;

import lombok.NoArgsConstructor;

/// HMAC SHA256 security strategy for webhooks.
///
/// Validates HMAC SHA256 signature configuration at creation time and authenticates
/// incoming webhook requests by verifying the signature against a stored secret.
@Component
@NoArgsConstructor
public class HmacSha256SecurityValidator implements WebhookSecurityStrategy {

  @Override
  public boolean supports(WebhookSecurityType securityType) {
    return WebhookSecurityType.HMAC_SHA256 == securityType;
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    WebhookSecurityConfigurationUtils.required(config, "header_name", "headerName");
    String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias",
        "secretAlias");
    WebhookSecurityConfigurationUtils.validateSecretAliasFormat(alias);
  }

}
