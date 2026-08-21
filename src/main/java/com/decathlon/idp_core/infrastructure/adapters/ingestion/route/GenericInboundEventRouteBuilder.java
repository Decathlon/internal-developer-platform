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
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler.WebhookExceptionRouteBuilder;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.IngestionProcessor;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.decoder.DecodingProcessor;

import lombok.RequiredArgsConstructor;

/// Generic Camel Route pipeline handling inbound webhook fetching, status validation, and payload decoding.
@Component
@RequiredArgsConstructor
public class GenericInboundEventRouteBuilder extends RouteBuilder {

  private final WebhookConnectorService webhookConnectorService;
  private final DecodingProcessor decodingProcessor;
  private final IngestionProcessor ingestionProcessor;
  private final WebhookExceptionRouteBuilder webhookExceptionRouteBuilder;

  @Override
  public void configure() throws Exception {
    webhookExceptionRouteBuilder.configureExceptions(this);

    from(DIRECT_PROCESS_EVENT).routeId(ROUTE_ID_WEBHOOK_PIPELINE)
        .log(LoggingLevel.INFO, "Incoming webhook headers: ${headers}")
        .setProperty(RAW_PAYLOAD_BODY_PROPERTY, body()).to(DIRECT_FETCH_CONFIGURATION)
        .to(DIRECT_VALIDATE_ENABLED).to(DIRECT_DECODE_PAYLOAD).to(DIRECT_INGEST_PAYLOAD)
        .removeHeaders("*") // Clear all accumulated incoming and internal headers
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HTTP_CREATED))
        .setHeader(Exchange.CONTENT_TYPE, constant(APPLICATION_JSON))
        .setBody(constant(SUCCESS_WEBHOOK_EVENT_PROCESSED));

    // --- Step A: Fetch Configuration ---
    from(DIRECT_FETCH_CONFIGURATION).routeId(ROUTE_ID_FETCH_WEBHOOK_CONFIG)
        .log(LoggingLevel.DEBUG,
            "Fetching configuration for webhook ID: ${exchangeProperty.connectorIdentifier}")
        .process(exchange -> {
          String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY,
              String.class);
          WebhookConnector webhookConnector = webhookConnectorService
              .getWebhookConnector(connectorIdentifier);
          exchange.setProperty(WEBHOOK_CONFIG_PROPERTY, webhookConnector);
        });

    // --- Step A.1: Validate Webhook Status ---
    from(DIRECT_VALIDATE_ENABLED).routeId(ROUTE_ID_VALIDATE_WEBHOOK_ENABLED)
        .log(LoggingLevel.DEBUG,
            "Validating webhook availability for ID: ${exchangeProperty.connectorIdentifier}")
        .process(exchange -> {
          WebhookConnector config = exchange.getProperty(WEBHOOK_CONFIG_PROPERTY,
              WebhookConnector.class);
          if (config == null) {
            String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY,
                String.class);
            throw new WebhookConfigurationMissingException(connectorIdentifier);
          }
          if (!config.enabled()) {
            throw new WebhookDisabledException(config.identifier());
          }
        });

    // --- Step B: Decode Payload ---
    from(DIRECT_DECODE_PAYLOAD).routeId(ROUTE_ID_DECODE_PAYLOAD)
        .log(LoggingLevel.DEBUG,
            "Decoding payload for webhook ID: ${exchangeProperty.connectorIdentifier}")
        .process(exchange -> {
          Object rawPayload = exchange.getProperty(RAW_PAYLOAD_BODY_PROPERTY);
          String decodedPayload = decodingProcessor.decode(rawPayload,
              exchange.getIn().getHeaders());
          exchange.getIn().setBody(decodedPayload);
          exchange.getIn().removeHeader(CONTENT_ENCODING_HEADER);
        });

    // --- Step C: Ingest Payload ---
    from(DIRECT_INGEST_PAYLOAD).routeId(ROUTE_ID_INGEST_PAYLOAD)
        .log(LoggingLevel.DEBUG,
            "Ingesting payload for webhook ID: ${exchangeProperty.connectorIdentifier}")
        .process(exchange -> {
          String decodedPayload = exchange.getIn().getBody(String.class);
          WebhookConnector config = exchange.getProperty(WEBHOOK_CONFIG_PROPERTY,
              WebhookConnector.class);
          ingestionProcessor.ingest(decodedPayload, config);
        });

  }
}
