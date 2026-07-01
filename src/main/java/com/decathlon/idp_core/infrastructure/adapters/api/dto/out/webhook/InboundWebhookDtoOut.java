package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook;

import java.util.List;

import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDtoOut;

/// Response payload for created inbound webhook connector.
public record InboundWebhookDtoOut(String identifier, String title, String description,
    boolean enabled, List<EntityDynamicMappingDtoOut> mappings,
    InboundWebhookSecurityDtoOut security) {

  public InboundWebhookDtoOut {
    mappings = mappings != null ? List.copyOf(mappings) : null;
  }
}
