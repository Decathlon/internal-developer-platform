package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.*;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InboundWebhookCreateDtoIn(
    @NotBlank(message = WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY) @Size(max = 255, message = WEBHOOK_CONNECTOR_IDENTIFIER_MAX_LENGTH) String identifier,
    @NotBlank(message = WEBHOOK_CONNECTOR_NAME_MANDATORY) @Size(max = 255, message = WEBHOOK_CONNECTOR_NAME_MAX_LENGTH) String name,
    String description, boolean enabled, List<String> mappingIdentifiers,
    @Valid InboundWebhookSecurityContractDtoIn security) {

  public InboundWebhookCreateDtoIn {
    mappingIdentifiers = mappingIdentifiers != null ? List.copyOf(mappingIdentifiers) : List.of();
  }
}
