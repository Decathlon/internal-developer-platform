package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Entity projection section for an inbound webhook mapping.
 */
public record InboundWebhookEntityMappingDtoIn(
        @NotBlank(message = "Webhook entity identifier expression is mandatory")
        String identifier,
        @NotBlank(message = "Webhook entity title expression is mandatory")
        String title,
        @NotNull(message = "Webhook entity properties section is mandatory")
        Map<String, String> properties,
        @NotNull(message = "Webhook entity relations section is mandatory")
        Map<String, String> relations
) {
}
