package com.decathlon.idp_core.infrastructure.adapters.ingestion.route;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.*;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.SPAN_ATTRIBUTE_WEBHOOK_IDENTIFIER;

import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConfigurationMissingException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookDisabledException;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler.WebhookExceptionRouteBuilder;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.metrics.IngestionObservabilitySupport;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.IngestionProcessor;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.PayloadValidationProcessor;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.SecurityProcessor;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.decoder.DecodingProcessor;

import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;

/// Generic Camel Route pipeline handling inbound webhook fetching, status validation, and payload decoding.
@Component
@Slf4j
public class GenericInboundEventRouteBuilder extends RouteBuilder {

  private final WebhookConnectorService webhookConnectorService;
  private final DecodingProcessor decodingProcessor;
  private final SecurityProcessor securityProcessor;
  private final IngestionProcessor ingestionProcessor;
  private final PayloadValidationProcessor payloadValidationProcessor;
  private final WebhookExceptionRouteBuilder webhookExceptionRouteBuilder;

  private final boolean tracingDiagnosticEnabled;

  public GenericInboundEventRouteBuilder(WebhookConnectorService webhookConnectorService,
      DecodingProcessor decodingProcessor, SecurityProcessor securityProcessor,
      IngestionProcessor ingestionProcessor, PayloadValidationProcessor payloadValidationProcessor,
      WebhookExceptionRouteBuilder webhookExceptionRouteBuilder,
      @Value("${app.ingestion.tracing-diagnostic-enabled:false}") boolean tracingDiagnosticEnabled) {
    this.webhookConnectorService = webhookConnectorService;
    this.decodingProcessor = decodingProcessor;
    this.securityProcessor = securityProcessor;
    this.ingestionProcessor = ingestionProcessor;
    this.payloadValidationProcessor = payloadValidationProcessor;
    this.webhookExceptionRouteBuilder = webhookExceptionRouteBuilder;
    this.tracingDiagnosticEnabled = tracingDiagnosticEnabled;
    log.info("GenericInboundEventRouteBuilder initialized with tracingDiagnosticEnabled={}",
        tracingDiagnosticEnabled);
  }

  @Override
  public void configure() throws Exception {
    webhookExceptionRouteBuilder.configureExceptions(this);

    // Main Pipeline
    from(DIRECT_PROCESS_EVENT).routeId(ROUTE_ID_WEBHOOK_PIPELINE)
        .process(exchange -> logTracingDiagnostics(exchange, "direct-process-event-entry"))
        .to(DIRECT_FETCH_CONFIGURATION).to(DIRECT_VALIDATE_ENABLED)
        .process(payloadValidationProcessor::validate).to(DIRECT_VALIDATE_SECURITY)
        .to(DIRECT_DECODE_PAYLOAD).to(DIRECT_INGEST_PAYLOAD).removeHeaders("*")
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
          if (webhookConnector == null) {
            throw new WebhookConfigurationMissingException(connectorIdentifier);
          }
          IngestionObservabilitySupport.setSpanAttribute(Span.current(), SPAN_ATTRIBUTE_WEBHOOK_IDENTIFIER, webhookConnector.identifier());
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

    // --- Step A.2: Validate Webhook Security ---
    from(DIRECT_VALIDATE_SECURITY).routeId(ROUTE_ID_VALIDATE_WEBHOOK_SECURITY)
        .log(LoggingLevel.DEBUG,
            "Validating webhook security for ID: ${exchangeProperty.connectorIdentifier}")
        .process(exchange -> {
          WebhookConnector config = exchange.getProperty(WEBHOOK_CONFIG_PROPERTY,
              WebhookConnector.class);
          if (config == null) {
            String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY,
                String.class);
            throw new WebhookConfigurationMissingException(connectorIdentifier);
          }
          byte[] rawPayload = exchange.getProperty(RAW_PAYLOAD_BODY_PROPERTY, byte[].class);
          securityProcessor.validate(exchange.getIn().getHeaders(), rawPayload, config);
        });

    // --- Step B: Decode Payload ---
    from(DIRECT_DECODE_PAYLOAD).routeId(ROUTE_ID_DECODE_PAYLOAD)
        .log(LoggingLevel.DEBUG,
            "Decoding payload for webhook ID: ${exchangeProperty.connectorIdentifier}")
        .process(exchange -> {
          byte[] rawPayload = exchange.getProperty(RAW_PAYLOAD_BODY_PROPERTY, byte[].class);
          String decodedPayload = decodingProcessor.decode(rawPayload,
              exchange.getIn().getHeaders());
          exchange.getIn().setBody(decodedPayload);
          exchange.getIn().removeHeader(CONTENT_ENCODING_HEADER);
        });

    // --- Step C: Ingest Payload ---
    from(DIRECT_INGEST_PAYLOAD).routeId(ROUTE_ID_INGEST_PAYLOAD)
        .log(LoggingLevel.DEBUG,
            "Ingesting payload for webhook ID: ${exchangeProperty.connectorIdentifier}")
        .process(exchange -> logTracingDiagnostics(exchange, "direct-ingest-payload"))
        .process(exchange -> {
          String decodedPayload = exchange.getIn().getBody(String.class);
          WebhookConnector config = exchange.getProperty(WEBHOOK_CONFIG_PROPERTY,
              WebhookConnector.class);
          ingestionProcessor.ingest(decodedPayload, config);
        });
  }

  private void logTracingDiagnostics(Exchange exchange, String stage) {
    if (!tracingDiagnosticEnabled || !log.isInfoEnabled()) {
      return;
    }

    Span currentSpan = Span.current();
    String traceId = currentSpan.getSpanContext().getTraceId();
    String spanId = currentSpan.getSpanContext().getSpanId();
    boolean validSpan = currentSpan.getSpanContext().isValid();

    String interestingProperties = exchange.getProperties().entrySet().stream()
        .filter(entry -> isTelemetryKey(entry.getKey()))
        .map(entry -> entry.getKey() + "="
            + (entry.getValue() == null ? "null" : entry.getValue().getClass().getSimpleName()))
        .sorted().collect(Collectors.joining(", "));

    log.info(
        "[OTEL CAMEL DIAG] stage={} routeId={} exchangeId={} spanValid={} traceId={} spanId={} traceparent={} b3={} telemetryProperties=[{}]",
        stage, exchange.getFromRouteId(), exchange.getExchangeId(), validSpan, traceId, spanId,
        exchange.getIn().getHeader("traceparent"), exchange.getIn().getHeader("b3"),
        interestingProperties);
  }

  private boolean isTelemetryKey(String key) {
    String normalized = key == null ? "" : key.toLowerCase();
    return normalized.contains("telemetry") || normalized.contains("trace")
        || normalized.contains("span") || normalized.contains("baggage")
        || normalized.startsWith("camel");
  }
}
