package com.decathlon.idp_core.infrastructure.adapters.camel.configuration;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/// Centralizes REST endpoint configuration for the webhook ingestion pipeline.
///
/// Separates HTTP binding concerns (REST DSL, path parameters, descriptions)
/// from route logic (transformations, service invocations, error handling). This
/// follows the Single Responsibility Principle and makes it easier to modify API
/// contracts without touching domain route wiring.
@Component
public class CamelRestConfiguration extends RouteBuilder {

  @Override
  public void configure() throws Exception {

    restConfiguration()
        // Use platform-http instead of servlet for better Spring Boot integration
        .component("platform-http").bindingMode(org.apache.camel.model.rest.RestBindingMode.off);

    rest("/webhooks").post("/{webhookIdentifier}").description("Generic webhook ingestion endpoint")
        .to("direct:generic-route");
  }
}
