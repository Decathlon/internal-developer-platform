package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_MAPPINGS_MANDATORY;

/**
 * Request payload used to create an inbound webhook connector configuration.
 */
public record InboundWebhookCreateDtoIn(
        @NotBlank(message = WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY)
        String identifier,
        @NotBlank(message = "Webhook title is mandatory")
        String title,
        String description,
        boolean enabled,
        @NotEmpty(message = WEBHOOK_CONNECTOR_MAPPINGS_MANDATORY)
        List<@Valid InboundWebhookMappingDtoIn> mappings,
        @Valid InboundWebhookSecurityContractDtoIn security
) {
}
