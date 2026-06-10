package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// Mapping rule request for inbound webhook transformation.
public record InboundWebhookMappingDtoIn(
    @NotBlank(message = "Webhook mapping template is mandatory") String template,
    @NotBlank(message = "Webhook mapping filter is mandatory") String filter,
    @NotNull(message = "Webhook mapping entity section is mandatory") @Valid InboundWebhookEntityMappingDtoIn entity) {
}
