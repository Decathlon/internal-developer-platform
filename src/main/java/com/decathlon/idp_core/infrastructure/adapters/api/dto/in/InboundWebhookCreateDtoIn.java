package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/// Request payload used to create an inbound webhook connector configuration.
public record InboundWebhookCreateDtoIn(
    @NotBlank(message = WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY) String identifier,
    @NotBlank(message = "Webhook title is mandatory") String title, String description,
    boolean enabled, List<@Valid InboundWebhookMappingDtoIn> mappings,
    @Valid InboundWebhookSecurityContractDtoIn security) {

  public InboundWebhookCreateDtoIn {
    mappings = mappings != null ? List.copyOf(mappings) : null;
  }
}
