package com.decathlon.idp_core.infrastructure.adapters.ingestion.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.service.IngestionService;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.service.SecurityService;

import lombok.RequiredArgsConstructor;

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
            "Received generic request for identifier: ${header.webhookIdentifier}")
        .setProperty("webhookId", header("webhookIdentifier")).to("direct:process-event");

    // Main pipeline configuration
    from("direct:process-event").routeId("webhook-pipeline")
        .setProperty("webhookIdentifier", header("webhookIdentifier"))
        .setProperty("rawJsonPayload", body())

        // Step A: Load Webhook Configuration
        .to("direct:fetch-configuration")

        // Step A: Validate Security (HMAC, JWT, etc.) using the loaded configuration
        .to("direct:validate-security")

        // Step C: Map and ingest-payload
        .to("direct:ingest-payload")

        // Step E: Return HTTP 202 Accepted Response
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setBody(constant(
            "{\"status\": \"SUCCESS\", \"message\": \"Webhook processed and saved successfully.\"}"));

    // Pipeline steps

    // --- Step A: Fetch Configuration ---
    from("direct:fetch-configuration").routeId("fetch-webhook-config")
        .log(LoggingLevel.DEBUG,
            "Fetching configuration for webhook ID: ${exchangeProperty.webhookIdentifier}")
        .bean(webhookConnectorService, "getWebhookConnector(${exchangeProperty.webhookIdentifier})")
        // Evaluates bean without overwriting exchange body
        .setProperty("webhookConfig", method(webhookConnectorService,
            "getWebhookConnector(${exchangeProperty.webhookIdentifier})"));

    // --- Step B: Security Validation ---
    from("direct:validate-security").routeId("validate-webhook-security")
        .log(LoggingLevel.DEBUG,
            "Applying security strategy for webhook: ${exchangeProperty.webhookIdentifier}")
        // Passes body (raw payload), headers (e.g. HMAC signatures/tokens), and config
        // bean to validator
        .bean(securityService,
            "validate(${exchangeProperty.rawJsonPayload}, ${headers}, ${exchangeProperty.webhookConfig})");

    // --- Step C: Entity Mapping ---
    from("direct:ingest-payload").routeId("ingest-payload")
        .log(LoggingLevel.DEBUG, "Transforming payload to entity using configuration rules...")
        .bean(ingestionService,
            "ingest(${exchangeProperty.rawJsonPayload}, ${exchangeProperty.webhookConfig})")
        .setProperty("mappedEntity", body()); // Store domain entity in Exchange property
  }
}