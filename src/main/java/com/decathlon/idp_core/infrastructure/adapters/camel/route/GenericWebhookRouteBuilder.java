package com.decathlon.idp_core.infrastructure.adapters.camel.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;
import com.decathlon.idp_core.infrastructure.adapters.camel.service.IngestionService;
import com.decathlon.idp_core.infrastructure.adapters.camel.service.SecurityService;

import lombok.RequiredArgsConstructor;

/// Routes generic webhook ingestion requests through the configuration lookup
/// and domain processing pipeline.
///
/// Receives webhook identifiers from the REST endpoint (configured in
/// CamelRestConfiguration), fetches the persisted connector configuration via
/// the domain service, and orchestrates downstream ingestion. Exception handling
/// (WebhookConnectorNotFoundException -> 404, etc.) is centralized
/// in WebhookExceptionRouteBuilder.
@Component
@RequiredArgsConstructor
public class GenericWebhookRouteBuilder extends RouteBuilder {

  /// Domain service resolving the persisted webhook connector configuration
  /// by identifier.
  private final WebhookConnectorService webhookConnectorService;
  private final SecurityService securityService;
  private final IngestionService ingestionService;

  @Override
  public void configure() throws Exception {

    from("direct:generic-route")
        .log(LoggingLevel.INFO,
            "Received generic webhook request for identifier: ${header.webhookIdentifier}")
        .setProperty("webhookId", header("webhookIdentifier")).to("direct:process-webhook");

    // Main pipeline configuration
    from("direct:process-webhook").routeId("webhook-pipeline")
        .setProperty("webhookIdentifier", header("webhookIdentifier"))

        // Step A: Load Webhook Configuration
        .to("direct:fetch-configuration")

        .to("direct:validate-security")

        // Step C: Map and ingest-payload
        .to("direct:ingest-payload")

        // Step E: Return HTTP 202 Accepted Response
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setBody(constant(
            "{\"status\": \"ACCEPTED\", \"message\": \"Webhook processed and saved successfully.\"}"));

    // Pipeline steps

    // --- Step A: Fetch Configuration ---
    from("direct:fetch-configuration").routeId("fetch-webhook-config")
        .log(LoggingLevel.DEBUG,
            "Fetching configuration for webhook ID: ${exchangeProperty.webhookIdentifier}")
        .bean(webhookConnectorService, "getWebhookConnector(${exchangeProperty.webhookIdentifier})")
        .setProperty("webhookConfig", body()); // Store config object in Exchange property

    // --- Step B: Security Validation ---
    from("direct:validate-security").routeId("validate-webhook-security")
        .log(LoggingLevel.DEBUG,
            "Applying security strategy for webhook: ${exchangeProperty.webhookIdentifier}")
        // Passes body (raw payload), headers (e.g. HMAC signatures/tokens), and config
        // bean to validator
        .bean(securityService, "validate(${body}, ${headers}, ${exchangeProperty.webhookConfig})");

    // --- Step C: Entity Mapping ---
    from("direct:ingest-payload").routeId("ingest-payload")
        .log(LoggingLevel.DEBUG, "Transforming payload to entity using configuration rules...")
        .bean(ingestionService, "ingest(${exchangeProperty.webhookConfig}, ${body})")
        .setProperty("mappedEntity", body()); // Store domain entity in Exchange property
  }
}
