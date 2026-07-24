package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping;

import java.util.List;
import java.util.Map;

/// Mapping rule returned by the inbound webhook management API.
public record EntityDynamicMappingDtoOut(String identifier, String entityTemplateIdentifier,
    String filter, String name, String description, InboundWebhookEntityMappingDtoOut entity) {
  /// Entity projection details exposed in webhook mapping responses.
  public static record InboundWebhookEntityMappingDtoOut(String identifier, String name,
      Map<String, String> properties, List<RelationMappingDtoOut> relations) {

    public InboundWebhookEntityMappingDtoOut {
      properties = properties != null ? Map.copyOf(properties) : null;
      // Relations are stored as an ordered list; null is normalized to empty list.
      relations = relations != null ? List.copyOf(relations) : List.of();
    }
  }
}
