package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook;

/**
 * Security strategy returned for webhook configuration responses.
 *
 * <p>Only returns the strategy type to avoid exposing technical secret references.
 */
public record InboundWebhookSecurityDtoOut(
        String type
) {
}
