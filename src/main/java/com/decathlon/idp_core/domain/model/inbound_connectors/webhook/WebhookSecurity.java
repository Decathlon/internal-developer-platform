package com.decathlon.idp_core.domain.model.inbound_connectors.webhook;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_SECURITY_CONFIG_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_SECURITY_TYPE_MANDATORY;

import java.util.Map;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

public record WebhookSecurity(WebhookSecurityType type, Map<String, String> config) {

  public WebhookSecurity {
    if (type == null) {
      throw new WebhookSecurityConfigurationException(WEBHOOK_CONNECTOR_SECURITY_TYPE_MANDATORY);
    }
    if (config == null) {
      throw new WebhookSecurityConfigurationException(WEBHOOK_CONNECTOR_SECURITY_CONFIG_MANDATORY);
    }
    config = Map.copyOf(config);
  }
}
