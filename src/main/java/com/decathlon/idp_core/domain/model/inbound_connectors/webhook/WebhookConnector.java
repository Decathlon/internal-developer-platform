package com.decathlon.idp_core.domain.model.inbound_connectors.webhook;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_MAX_SIZE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY;

import java.util.List;
import java.util.UUID;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorConfigurationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;

public record WebhookConnector(UUID id, String identifier, String title, String description,
    boolean enabled, List<EntityDynamicMapping> mappings, WebhookSecurity security) {
  public WebhookConnector {
    mappings = mappings == null ? List.of() : List.copyOf(mappings);

    if (security == null) {
      throw new WebhookSecurityConfigurationException("Webhook security type is mandatory");
    }

    if (isBlank(identifier)) {
      throw new WebhookConnectorConfigurationException(WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY);
    }
    if (identifier.length() > 255) {
      throw new WebhookConnectorConfigurationException("Webhook title is too long");
    }

    if (isBlank(title)) {
      throw new WebhookConnectorConfigurationException(
          "Webhook identifier must not exceed 255 characters");
    }
    if (title.length() > 255) {
      throw new WebhookConnectorConfigurationException(TEMPLATE_NAME_MAX_SIZE);
    }
  }

  /// Creates a copy of this connector with the specified enabled state.
  ///
  /// @param enabled the new enabled state
  /// @return a new WebhookConnector instance with the updated enabled field
  public WebhookConnector withEnabled(boolean enabled) {
    return new WebhookConnector(id, identifier, title, description, enabled, mappings, security);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
