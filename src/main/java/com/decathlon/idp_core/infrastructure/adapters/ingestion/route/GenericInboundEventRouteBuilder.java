package com.decathlon.idp_core.infrastructure.adapters.ingestion.route;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.*;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConfigurationMissingException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookDisabledException;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GenericInboundEventRouteBuilder extends RouteBuilder {

  private final WebhookConnectorService webhookConnectorService;
  private final WebhookExceptionRouteBuilder webhookExceptionRouteBuilder;

  @Override
  public void configure() throws Exception {
    webhookExceptionRouteBuilder.configureExceptions(this);

    // Main pipeline configuration
    from(DIRECT_PROCESS_EVENT).routeId(ROUTE_ID_WEBHOOK_PIPELINE)
        .setProperty("rawPayloadBody", body())
        // Step A: Load Webhook Configuration
        .to(DIRECT_FETCH_CONFIGURATION)
        // Step A.1: Validate webhook is enabled
        .to(DIRECT_VALIDATE_ENABLED)
        // Return 200 once configuration is found and enabled.
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HTTP_OK))
        .setHeader(Exchange.CONTENT_TYPE, constant(APPLICATION_JSON))
        .setBody(constant(SUCCESS_BODY_CONFIGURATION_LOADED));

    // --- Step A: Fetch Configuration ---
    from(DIRECT_FETCH_CONFIGURATION).routeId(ROUTE_ID_FETCH_WEBHOOK_CONFIG)
        .log(LoggingLevel.DEBUG,
            "Fetching configuration for webhook ID: ${exchangeProperty.connectorIdentifier}")
        .setProperty(WEBHOOK_CONFIG_PROPERTY, method(webhookConnectorService,
            "getWebhookConnector(${exchangeProperty.connectorIdentifier})"));

    // --- Step A.1: Validate Webhook is Enabled ---
    from(DIRECT_VALIDATE_ENABLED).routeId(ROUTE_ID_VALIDATE_WEBHOOK_ENABLED)
        .log(LoggingLevel.DEBUG,
            "Validating webhook is enabled: ${exchangeProperty.connectorIdentifier}")
        .process(exchange -> {
          var config = exchange.getProperty(WEBHOOK_CONFIG_PROPERTY, WebhookConnector.class);
          if (config == null) {
            String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY,
                String.class);
            throw new WebhookConfigurationMissingException(connectorIdentifier);
          }
          if (!config.enabled()) {
            throw new WebhookDisabledException(config.identifier());
          }
        });
  }
}
