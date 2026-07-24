package com.decathlon.idp_core.infrastructure.adapters.camel.handler;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;

public class IngestionRouteExceptionHandler extends RouteBuilder {

  @Override
  public void configure() throws Exception {

    // Dedicated handling for an unknown webhook identifier: the domain service
    // throws WebhookConnectorNotFoundException instead of returning null, so it
    // must be mapped to a 404 here (mirrors ApiExceptionHandler's role for the
    // REST adapter, applied at the Camel boundary).
    onException(WebhookConnectorNotFoundException.class).handled(true)
        .log(LoggingLevel.WARN,
            "No webhook connector found for identifier: ${exchangeProperty.webhookId}")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .setBody(constant("{\"error\": \"Webhook configuration not found\"}"));

    // Catch-all for the webhook pipeline: any other unhandled exception is
    // reported as a 500 instead of leaking a stack trace to the caller.
    onException(Exception.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .setBody(constant("{\"error\": \"Internal server error processing ingestion payload\"}"));
  }
}
