package com.decathlon.idp_core.domain.model.inbound_connectors.webhook;

/// Domain policy controlling which webhook security configurations are allowed.
///
/// @param allowNone whether connectors may be created without webhook authentication
public record WebhookSecurityValidationPolicy(boolean allowNone) {
}
