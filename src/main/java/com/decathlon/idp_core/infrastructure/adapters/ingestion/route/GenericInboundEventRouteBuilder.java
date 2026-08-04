package com.decathlon.idp_core.infrastructure.adapters.ingestion.route;

import java.util.zip.ZipException;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.DecodingProcessor;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.IngestionProcessor;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.SecurityProcessor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GenericInboundEventRouteBuilder extends RouteBuilder {

  /// Domain service resolving the persisted webhook connector configuration
  /// by identifier.
  private final WebhookConnectorService webhookConnectorService;
  private final SecurityProcessor securityProcessor;
  private final IngestionProcessor ingestionProcessor;
  private final DecodingProcessor decodingProcessor;

  @Override
  public void configure() throws Exception {

    // ---------------------------------------------------------------------------
    // Exception handlers — must be declared before any from() route in the same
    // RouteBuilder instance so that Camel registers them on the routes below.
    // ---------------------------------------------------------------------------

    onException(WebhookConnectorNotFoundException.class).handled(true)
        .log(LoggingLevel.WARN,
            "No webhook connector found for identifier: ${exchangeProperty.connectorIdentifier}")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .setBody(constant("{\"error\": \"Webhook configuration not found\"}"));

    onException(ZipException.class).handled(true)
        .log(LoggingLevel.WARN, "Error occurred while processing ZIP payload: ${exception.message}")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
        .setBody(constant("{\"error\": \"Invalid ZIP payload\"}"));

    onException(Exception.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .log(LoggingLevel.ERROR, "Error occurred: ${exception.message}")
        .setBody(constant("{\"error\": \"Internal server error processing ingestion payload\"}"));

    // Main pipeline configuration
    from("direct:process-event").routeId("webhook-pipeline").setProperty("rawPayloadBody", body())
        // Step A: Load Webhook Configuration
        .to("direct:fetch-configuration")
        // Step B: Validate Security (HMAC, JWT, etc.) using the loaded configuration
        .to("direct:validate-security")
        // Step C: decode payload if necessary (e.g., gzip) using the header information
        .to("direct:decode-payload")
        // Step D: Map and ingest-payload
        .to("direct:ingest-payload")

        // Step E: Return HTTP 202 Accepted Response
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setBody(constant(
            "{\"status\": \"SUCCESS\", \"message\": \"Webhook processed and saved successfully.\"}"));

    // Pipeline steps

    // --- Step A: Fetch Configuration ---
    from("direct:fetch-configuration").routeId("fetch-webhook-config")
        .log(LoggingLevel.DEBUG,
            "Fetching configuration for webhook ID: ${exchangeProperty.connectorIdentifier}")
        .bean(webhookConnectorService,
            "getWebhookConnector(${exchangeProperty.connectorIdentifier})")
        // Evaluates bean without overwriting exchange body
        .setProperty("webhookConfig", method(webhookConnectorService,
            "getWebhookConnector(${exchangeProperty.connectorIdentifier})"));

    // --- Step B: Security Validation ---
    from("direct:validate-security").routeId("validate-webhook-security")
        .log(LoggingLevel.DEBUG,
            "Applying security strategy for webhook: ${exchangeProperty.connectorIdentifier}")
        // Passes headers (e.g. HMAC signatures/tokens), and config to validator
        .bean(securityProcessor, "validate(${headers}, ${exchangeProperty.webhookConfig})");

    // --- Step C: Decode payload if necessary---
    from("direct:decode-payload").routeId("decode-payload")
        .log(LoggingLevel.DEBUG,
            "Decoding payload for webhook: ${exchangeProperty.connectorIdentifier}")
        // Passes body (raw payload), headers (e.g. HMAC signatures/tokens)
        .bean(decodingProcessor, "decode(${exchangeProperty.rawPayloadBody}, ${headers})")
        .setProperty("decodedPayloadBody", body());

    // --- Step D: Entity ingestion ---
    from("direct:ingest-payload").routeId("ingest-payload")
        .log(LoggingLevel.DEBUG, "Transforming payload to entity using configuration rules...")
        .bean(ingestionProcessor,
            "ingest(${exchangeProperty.decodedPayloadBody}, ${exchangeProperty.webhookConfig})")
        .setProperty("mappedEntity", body()); // Store domain entity in Exchange property
  }
}