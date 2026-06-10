package com.decathlon.idp_core.domain.model.inbound_connectors.webhook;

import java.util.Map;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

public record WebhookSecurity(WebhookSecurityType type, Map<String, String> config) {

  public WebhookSecurity {
    if (type == null) {
      throw new WebhookSecurityConfigurationException("Webhook security type is mandatory");
    }
    if (config == null) {
      throw new WebhookSecurityConfigurationException(
          "Webhook security config section is mandatory");
    }
    config = Map.copyOf(config);
  }
}
