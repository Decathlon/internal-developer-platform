package com.decathlon.idp_core.infrastructure.adapters.camel.configuration;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/// Centralizes REST endpoint configuration for the webhook ingestion pipeline.
///
/// Separates HTTP binding concerns (REST DSL, path parameters, descriptions) from
/// route logic (transformations, service invocations, error handling). This follows
/// the Single Responsibility Principle and makes it easier to modify API contracts
/// without touching domain route wiring.
@Component
public class CamelRestConfiguration extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    rest("/api/v1/webhooks")
        .post("/{webhookIdentifier}")
            .description("Generic webhook ingestion endpoint")
            .routeId("webhook-inbound-api")
            .to("direct:generic-route");
  }
}
