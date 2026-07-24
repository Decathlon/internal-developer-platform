package com.decathlon.idp_core.infrastructure.adapters.camel.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;

/// Centralizes exception-to-HTTP mapping for the webhook ingestion pipeline.
///
/// `onException` clauses register on the shared `CamelContext` regardless of
/// which `RouteBuilder` declares them, so extracting them into this dedicated
/// class does not change their scope: they still apply to every route in the
/// context, including `GenericWebhookRouteBuilder`. Keeping them here mirrors
/// `ApiExceptionHandler`'s role for the REST adapter, separating cross-cutting
/// error handling from route wiring.
@Component
public class WebhookExceptionRouteBuilder extends RouteBuilder {

  @Override
  public void configure() throws Exception {

    onException(WebhookConnectorNotFoundException.class).handled(true)
        .log(LoggingLevel.WARN,
            "No webhook connector found for identifier: ${exchangeProperty.webhookId}")
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
        .setBody(constant("{\"error\": \"Webhook configuration not found\"}"));

    onException(Exception.class).handled(true).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
        .setBody(constant("{\"error\": \"Internal server error processing ingestion payload\"}"));
  }
}
