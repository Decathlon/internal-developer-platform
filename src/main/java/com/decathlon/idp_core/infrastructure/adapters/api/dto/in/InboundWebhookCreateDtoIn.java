package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/// Request payload used to create an inbound webhook connector configuration.
///
/// Mappings are no longer embedded in the connector payload. They are created
/// independently through the `/api/v1/inbound-dynamic-mapping` endpoint and
/// referenced here by their identifiers. Each referenced mapping existence is
/// validated in the domain layer before the connector is persisted.
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InboundWebhookCreateDtoIn(
    @NotBlank(message = WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY) String identifier,
    @NotBlank(message = "Webhook title is mandatory") String title, String description,
    boolean enabled, List<String> mappingIdentifiers,
    @Valid InboundWebhookSecurityContractDtoIn security) {

  public InboundWebhookCreateDtoIn {
    mappingIdentifiers = mappingIdentifiers != null ? List.copyOf(mappingIdentifiers) : List.of();
  }
}
