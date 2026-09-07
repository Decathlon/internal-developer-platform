package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/// Configuration properties for inbound webhook ingestion.
///
/// Allows externalizing the webhook base path and identifier path to avoid hardcoded endpoints.
@ConfigurationProperties(prefix = "app.ingestion.webhook")
public record IngestionProperties(
    @DefaultValue("/webhooks") @Pattern(regexp = "^/.*", message = "basePath must start with /") String basePath,
    @DefaultValue("/{webhookIdentifier}") @Pattern(regexp = "^/.*", message = "basePath must start with /") String identifierPath) {
}
