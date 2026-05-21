package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook;

import java.util.Map;

/**
 * Entity projection details exposed in webhook mapping responses.
 */
public record InboundWebhookEntityMappingDtoOut(
        String identifier,
        String title,
        Map<String, String> properties,
        Map<String, String> relations
) {
}
