package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/// Configuration properties for inbound webhook ingestion.
///
/// Allows externalizing the webhook base path and identifier path to avoid hardcoded endpoints.
@ConfigurationProperties(prefix = "app.ingestion.webhook")
public record IngestionProperties(@DefaultValue("/webhooks") String basePath,
    @DefaultValue("/{webhookIdentifier}") String identifierPath) {
}
