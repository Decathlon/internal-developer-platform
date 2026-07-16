package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;

/// Static Token security strategy for webhooks.
///
/// Validates static token configuration at creation time and authenticates incoming
@Component
public class StaticTokenSecurityValidator implements WebhookSecurityStrategy {

  @Override
  public boolean supports(WebhookSecurityType securityType) {
    return WebhookSecurityType.STATIC_TOKEN == securityType;
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    WebhookSecurityConfigurationUtils.required(config, "header_name", "headerName");
    String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias",
        "secretAlias");
    WebhookSecurityConfigurationUtils.validateSecretAliasFormat(alias);
  }

}
