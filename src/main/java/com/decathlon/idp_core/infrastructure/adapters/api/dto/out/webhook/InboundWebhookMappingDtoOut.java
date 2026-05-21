package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook;

/**
 * Mapping rule returned by the inbound webhook management API.
 */
public record InboundWebhookMappingDtoOut(
        String template,
        String filter,
        InboundWebhookEntityMappingDtoOut entity
) {
}
