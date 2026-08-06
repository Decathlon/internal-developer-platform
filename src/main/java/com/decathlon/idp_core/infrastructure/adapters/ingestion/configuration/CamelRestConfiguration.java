package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.WebhookIngestionTracingProcessor;

import lombok.RequiredArgsConstructor;

/**
 * Centralizes REST endpoint configuration for the webhook ingestion pipeline.
 *
 * Separates HTTP binding concerns (REST DSL, path parameters, descriptions)
 * from route logic (transformations, service invocations, error handling). This
 * follows the Single Responsibility Principle and makes it easier to modify API
 * contracts without touching domain route wiring.
 */
@Component
@RequiredArgsConstructor
public class CamelRestConfiguration extends RouteBuilder {

  private final WebhookIngestionTracingProcessor tracingProcessor;

  @Override
  public void configure() throws Exception {

    restConfiguration()
        // Use platform-http instead of servlet for better Spring Boot integration
        .component("platform-http").bindingMode(org.apache.camel.model.rest.RestBindingMode.off);

    rest("/webhooks").post("/{webhookIdentifier}").description("Generic webhook ingestion endpoint")
        .to("direct:webhook-ingestion");

    from("direct:webhook-ingestion").routeId("generic-webhook-entrypoint")
        .log(LoggingLevel.INFO,
            "Received generic request for identifier: ${header.webhookIdentifier}")
        .setProperty("connectorIdentifier", header("webhookIdentifier"))
        .bean(tracingProcessor, "logInboundRequest").to("direct:process-event");

  }
}
