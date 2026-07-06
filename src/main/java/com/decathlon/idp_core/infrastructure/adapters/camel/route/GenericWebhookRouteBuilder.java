package com.decathlon.idp_core.infrastructure.adapters.camel.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;

import lombok.RequiredArgsConstructor;

/// Routes generic webhook ingestion requests through the configuration lookup and
/// domain processing pipeline.
///
/// Receives webhook identifiers from the REST endpoint (configured in
/// CamelRestConfiguration), fetches the persisted connector configuration via the
/// domain service, and orchestrates downstream ingestion. Exception handling
/// (WebhookConnectorNotFoundException -> 404, etc.) is centralized in
/// WebhookExceptionRouteBuilder.
@Component
@RequiredArgsConstructor
public class GenericWebhookRouteBuilder extends RouteBuilder {

  /// Domain service resolving the persisted webhook connector configuration by
  /// identifier.
  private final WebhookConnectorService webhookConnectorService;

  @Override
  public void configure() throws Exception {

    from("direct:generic-route")
        .routeId("webhook-config-lookup-pipeline")
        .log(LoggingLevel.INFO,
            "Received generic webhook request for identifier: ${header.webhookIdentifier}")

        .setProperty("webhookId", header("webhookIdentifier"))

        .log(LoggingLevel.INFO,
            "Fetching webhook connector configuration for identifier: ${exchangeProperty.webhookId}")

        .bean(webhookConnectorService,
            "getWebhookConnector(${exchangeProperty.webhookId})")

        .setProperty("webhookConfiguration", body())
        .log(LoggingLevel.INFO, "Loaded configuration successfully. Proceeding to ingestion route.")

        .to("direct:execute-generic-ingestion");

    from("direct:execute-generic-ingestion")
        .routeId("generic-ingestion-executor")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202))
        .setBody(constant("{\"status\": \"Payload accepted for processing\"}"));
  }
}
