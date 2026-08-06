package com.decathlon.idp_core.infrastructure.adapters.ingestion.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookDisabledException;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.WebhookIngestionTracingProcessor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GenericInboundEventRouteBuilder extends RouteBuilder {

  /// Domain service resolving the persisted webhook connector configuration
  /// by identifier.
  private final WebhookConnectorService webhookConnectorService;
  private final WebhookIngestionTracingProcessor tracingProcessor;

  @Override
  public void configure() throws Exception {

    // ---------------------------------------------------------------------------
    // Exception handlers — must be declared before any from() route in the same
    // RouteBuilder instance so that Camel registers them on the routes below.
    // ---------------------------------------------------------------------------

    onException(WebhookConnectorNotFoundException.class).handled(true)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .bean(tracingProcessor, "traceFailure")
        .log(LoggingLevel.WARN,
            "No webhook connector found for identifier: ${exchangeProperty.connectorIdentifier}")
        .setBody(constant("{\"error\": \"Webhook configuration not found\"}"));

    onException(WebhookDisabledException.class).handled(true)
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(403))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .bean(tracingProcessor, "traceFailure")
        .log(LoggingLevel.WARN,
            "Webhook connector is disabled: ${exchangeProperty.connectorIdentifier}")
        .setBody(constant("{\"error\": \"Webhook connector is disabled\"}"));

    onException(Exception.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        .bean(tracingProcessor, "traceFailure")
        .log(LoggingLevel.ERROR, "Error occurred: ${exception.message}")
        .setBody(constant("{\"error\": \"Internal server error processing ingestion payload\"}"));

    // Main pipeline configuration
    from("direct:process-event").routeId("webhook-pipeline").setProperty("rawPayloadBody", body())
        // Step A: Load Webhook Configuration
        .to("direct:fetch-configuration")
        // Step A.1: Validate webhook is enabled
        .to("direct:validate-enabled").bean(tracingProcessor, "traceEnabledValidationPassed")
        // Return 200 once configuration is found and enabled.
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).setBody(constant(
            "{\"status\": \"SUCCESS\", \"message\": \"Webhook configuration loaded and enabled.\"}"));

    // --- Step A: Fetch Configuration ---
    from("direct:fetch-configuration").routeId("fetch-webhook-config")
        .log(LoggingLevel.DEBUG,
            "Fetching configuration for webhook ID: ${exchangeProperty.connectorIdentifier}")
        .setProperty("webhookConfig",
            method(webhookConnectorService,
                "getWebhookConnector(${exchangeProperty.connectorIdentifier})"))
        .bean(tracingProcessor, "traceConfigurationLoaded");

    // --- Step A.1: Validate Webhook is Enabled ---
    from("direct:validate-enabled").routeId("validate-webhook-enabled")
        .log(LoggingLevel.DEBUG,
            "Validating webhook is enabled: ${exchangeProperty.connectorIdentifier}")
        .process(exchange -> {
          var config = exchange.getProperty("webhookConfig", WebhookConnector.class);
          if (!config.enabled()) {
            throw new WebhookDisabledException(config.identifier());
          }
        });
    //
    // // --- Step B: Security Validation ---
    // from("direct:validate-security").routeId("validate-webhook-security")
    // .log(LoggingLevel.DEBUG,
    // "Applying security strategy for webhook:
    // ${exchangeProperty.connectorIdentifier}")
    // // Passes headers (e.g. HMAC signatures/tokens), and config to validator
    // .bean(securityProcessor, "validate(${headers},
    // ${exchangeProperty.webhookConfig})");
    //
    // // --- Step C: Decode payload if necessary---
    // from("direct:decode-payload").routeId("decode-payload")
    // .log(LoggingLevel.DEBUG,
    // "Decoding payload for webhook: ${exchangeProperty.connectorIdentifier}")
    // // Passes body (raw payload), headers (e.g. HMAC signatures/tokens)
    // .bean(decodingProcessor, "decode(${exchangeProperty.rawPayloadBody},
    // ${headers})")
    // .setProperty("decodedPayloadBody", body());
    //
    // // --- Step D: Entity ingestion ---
    // from("direct:ingest-payload").routeId("ingest-payload")
    // .log(LoggingLevel.DEBUG, "Transforming payload to entity using configuration
    // rules...")
    // .bean(ingestionProcessor,
    // "ingest(${exchangeProperty.decodedPayloadBody},
    // ${exchangeProperty.webhookConfig})")
    // .setProperty("mappedEntity", body()); // Store domain entity in Exchange
    // property
  }
}
