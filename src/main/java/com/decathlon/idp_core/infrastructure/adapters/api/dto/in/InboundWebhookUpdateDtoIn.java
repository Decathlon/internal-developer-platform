package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_NAME_MAX_LENGTH;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InboundWebhookUpdateDtoIn(
    @NotBlank(message = WEBHOOK_CONNECTOR_NAME_MANDATORY) @Size(max = 255, message = WEBHOOK_CONNECTOR_NAME_MAX_LENGTH) String name,
    String description, boolean enabled, List<String> mappingIdentifiers,
    @Valid InboundWebhookSecurityContractDtoIn security) {

  public InboundWebhookUpdateDtoIn {
    mappingIdentifiers = mappingIdentifiers != null ? List.copyOf(mappingIdentifiers) : List.of();
  }
}
