package com.decathlon.idp_core.infrastructure.adapters.camel.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;

@Component
public class GenericWebhookRouteBuilder extends RouteBuilder {

  // Domain service resolving the persisted webhook connector configuration by
  // identifier. Injected via constructor (never field injection) per the
  // project's Spring Boot conventions.
  private final WebhookConnectorService webhookConnectorService;

  public GenericWebhookRouteBuilder(WebhookConnectorService webhookConnectorService) {
    this.webhookConnectorService = webhookConnectorService;
  }

  @Override
  public void configure() throws Exception {

    // Global Exception Handling for the Webhook Pipeline
    onException(Exception.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .setBody(constant("{\"error\": \"Internal server error processing ingestion payload\"}"));

    // Dedicated handling for an unknown webhook identifier: the domain service
    // throws WebhookConnectorNotFoundException instead of returning null, so it
    // must be mapped to a 404 here (mirrors ApiExceptionHandler's role for the
    // REST adapter, applied at the Camel boundary).
    onException(WebhookConnectorNotFoundException.class).handled(true)
        .log(LoggingLevel.WARN,
            "No webhook connector found for identifier: ${exchangeProperty.webhookId}")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .setBody(constant("{\"error\": \"Webhook configuration not found\"}"));

    // 1. Expose Generic HTTP Inbound Endpoint
    rest("/api/v1/webhooks").post("/{webhookIdentifier}")
        .description("Generic webhook ingestion endpoint").routeId("webhook-inbound-api")
        .to("direct:process-generic-webhook");

    // 2. Processing and Inbound Configuration Lookup Route
    from("direct:process-generic-webhook").routeId("webhook-config-lookup-pipeline")
        .log(LoggingLevel.INFO,
            "Received generic webhook request for identifier: ${header.webhookIdentifier}")

        // Extract the path parameter directly into a header variable
        .setProperty("webhookId", header("webhookIdentifier"))

        .log(LoggingLevel.INFO,
            "Fetching webhook connector configuration for identifier: ${exchangeProperty.webhookId}")

        // Invoke the domain service to look up the persisted connector
        // configuration. Throws WebhookConnectorNotFoundException (handled
        // above) when the identifier does not match any existing connector.
        .bean(webhookConnectorService, "getWebhookConnector(${exchangeProperty.webhookId})")

        // Store configuration for the downstream transformations/ingestion steps
        .setProperty("webhookConfiguration", body())
        .log(LoggingLevel.INFO, "Loaded configuration successfully. Proceeding to ingestion route.")

        // Route to your generic ingestion adapter processing logic
        .to("direct:execute-generic-ingestion");

    // 3. Downstream Ingestion Routing Placeholder
    from("direct:execute-generic-ingestion").routeId("generic-ingestion-executor")
        // Add transformation steps or pass directly to domain services here
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202))
        .setBody(constant("{\"status\": \"Payload accepted for processing\"}"));
  }
}
