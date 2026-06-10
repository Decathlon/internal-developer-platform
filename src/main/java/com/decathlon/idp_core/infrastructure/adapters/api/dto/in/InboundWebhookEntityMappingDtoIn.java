package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// Entity projection section for an inbound webhook mapping.
public record InboundWebhookEntityMappingDtoIn(
    @NotBlank(message = "Webhook entity identifier expression is mandatory") String identifier,
    @NotBlank(message = "Webhook entity title expression is mandatory") String title,
    @NotNull(message = "Webhook entity properties section is mandatory") Map<String, String> properties,
    @NotNull(message = "Webhook entity relations section is mandatory") Map<String, String> relations) {

  public InboundWebhookEntityMappingDtoIn {
    properties = properties != null ? Map.copyOf(properties) : null;
    relations = relations != null ? Map.copyOf(relations) : null;
  }
}
