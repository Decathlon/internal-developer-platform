package com.decathlon.idp_core.infrastructure.adapters.ingestion.route;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.APPLICATION_JSON;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONNECTOR_IDENTIFIER_PROPERTY;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.DIRECT_FETCH_CONFIGURATION;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.DIRECT_PROCESS_EVENT;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.DIRECT_VALIDATE_ENABLED;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.ROUTE_ID_FETCH_WEBHOOK_CONFIG;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.ROUTE_ID_VALIDATE_WEBHOOK_ENABLED;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.ROUTE_ID_WEBHOOK_PIPELINE;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.TRACE_METHOD_CONFIGURATION_LOADED;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.TRACE_METHOD_ENABLED_VALIDATION_PASSED;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.TRACE_METHOD_FAILURE;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.UNKNOWN_VALUE;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.WEBHOOK_CONFIG_PROPERTY;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConfigurationMissingException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookDisabledException;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;
import com.decathlon.idp_core.infrastructure.adapters.common.model.ErrorResponse;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.WebhookIngestionTracingProcessor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GenericInboundEventRouteBuilder extends RouteBuilder {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(GenericInboundEventRouteBuilder.class);
  private static final int HTTP_OK = 200;
  private static final String ERROR_DESCRIPTION_WEBHOOK_NOT_FOUND = "Webhook configuration not found";
  private static final String ERROR_DESCRIPTION_WEBHOOK_DISABLED = "Webhook connector is disabled";
  private static final String ERROR_DESCRIPTION_WEBHOOK_CONFIGURATION_MISSING = "Webhook configuration unavailable";
  private static final String ERROR_DESCRIPTION_INTERNAL_SERVER_ERROR = "Internal server error processing ingestion payload";
  private static final String SUCCESS_BODY_CONFIGURATION_LOADED = "{\"status\": \"SUCCESS\", \"message\": \"Webhook configuration loaded and enabled.\"}";

  /**
   * Domain service resolving the persisted webhook connector configuration by
   * identifier.
   */
  private final WebhookConnectorService webhookConnectorService;
  private final WebhookIngestionTracingProcessor tracingProcessor;

  @Override
  public void configure() throws Exception {

    // ---------------------------------------------------------------------------
    // Exception handlers — must be declared before any from() route in the same
    // RouteBuilder instance so that Camel registers them on the routes below.
    // ---------------------------------------------------------------------------

    onException(WebhookConnectorNotFoundException.class).handled(true)
        .process(exchange -> setJsonErrorResponse(exchange, HttpStatus.NOT_FOUND,
            ERROR_DESCRIPTION_WEBHOOK_NOT_FOUND))
        .process(exchange -> logHandledException(exchange, LoggingLevel.WARN,
            "webhook_connector_not_found", HttpStatus.NOT_FOUND.value()))
        .bean(tracingProcessor, TRACE_METHOD_FAILURE);

    onException(WebhookDisabledException.class).handled(true)
        .process(exchange -> setJsonErrorResponse(exchange, HttpStatus.FORBIDDEN,
            ERROR_DESCRIPTION_WEBHOOK_DISABLED))
        .process(exchange -> logHandledException(exchange, LoggingLevel.WARN,
            "webhook_connector_disabled", HttpStatus.FORBIDDEN.value()))
        .bean(tracingProcessor, TRACE_METHOD_FAILURE);

    onException(WebhookConfigurationMissingException.class).handled(true)
        .process(exchange -> setJsonErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
            ERROR_DESCRIPTION_WEBHOOK_CONFIGURATION_MISSING))
        .process(exchange -> logHandledException(exchange, LoggingLevel.ERROR,
            "webhook_configuration_missing", HttpStatus.INTERNAL_SERVER_ERROR.value()))
        .bean(tracingProcessor, TRACE_METHOD_FAILURE);

    onException(Exception.class).handled(true)
        .process(exchange -> setJsonErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
            ERROR_DESCRIPTION_INTERNAL_SERVER_ERROR))
        .process(exchange -> logHandledException(exchange, LoggingLevel.ERROR,
            "webhook_ingestion_unexpected_error", HttpStatus.INTERNAL_SERVER_ERROR.value()))
        .bean(tracingProcessor, TRACE_METHOD_FAILURE);

    // Main pipeline configuration
    from(DIRECT_PROCESS_EVENT).routeId(ROUTE_ID_WEBHOOK_PIPELINE)
        .setProperty("rawPayloadBody", body())
        // Step A: Load Webhook Configuration
        .to(DIRECT_FETCH_CONFIGURATION)
        // Step A.1: Validate webhook is enabled
        .to(DIRECT_VALIDATE_ENABLED).bean(tracingProcessor, TRACE_METHOD_ENABLED_VALIDATION_PASSED)
        // Return 200 once configuration is found and enabled.
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HTTP_OK))
        .setHeader(Exchange.CONTENT_TYPE, constant(APPLICATION_JSON))
        .setBody(constant(SUCCESS_BODY_CONFIGURATION_LOADED));

    // --- Step A: Fetch Configuration ---
    from(DIRECT_FETCH_CONFIGURATION).routeId(ROUTE_ID_FETCH_WEBHOOK_CONFIG)
        .log(LoggingLevel.DEBUG,
            "Fetching configuration for webhook ID: ${exchangeProperty.connectorIdentifier}")
        .setProperty(WEBHOOK_CONFIG_PROPERTY,
            method(webhookConnectorService,
                "getWebhookConnector(${exchangeProperty.connectorIdentifier})"))
        .bean(tracingProcessor, TRACE_METHOD_CONFIGURATION_LOADED);

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
    //
    // // --- Step B: Security Validation ---
    // from("direct:validate-security").routeId("validate-webhook-security")
    // .log(LoggingLevel.DEBUG,
    // "Applying security strategy for webhook:
    // ${exchangeProperty.connectorIdentifier}")
    // // Passes headers (e.g. HMAC signatures/tokens), and config to validator
    // .bean(securityProcessor, "validate(${headers},
    // ${exchangeProperty.webhookConfig})");
    //
    // // --- Step C: Decode payload if necessary---
    // from("direct:decode-payload").routeId("decode-payload")
    // .log(LoggingLevel.DEBUG,
    // "Decoding payload for webhook: ${exchangeProperty.connectorIdentifier}")
    // // Passes body (raw payload), headers (e.g. HMAC signatures/tokens)
    // .bean(decodingProcessor, "decode(${exchangeProperty.rawPayloadBody},
    // ${headers})")
    // .setProperty("decodedPayloadBody", body());
    //
    // // --- Step D: Entity ingestion ---
    // from("direct:ingest-payload").routeId("ingest-payload")
    // .log(LoggingLevel.DEBUG, "Transforming payload to entity using configuration
    // rules...")
    // .bean(ingestionProcessor,
    // "ingest(${exchangeProperty.decodedPayloadBody},
    // ${exchangeProperty.webhookConfig})")
    // .setProperty("mappedEntity", body()); // Store domain entity in Exchange
    // property
  }

  private static void setJsonErrorResponse(Exchange exchange, HttpStatus httpStatus,
      String errorDescription) {
    ErrorResponse errorResponse = new ErrorResponse(httpStatus.name(), errorDescription);
    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, httpStatus.value());
    exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, APPLICATION_JSON);
    exchange.getMessage().setBody(serializeErrorResponse(errorResponse));
  }

  private static String serializeErrorResponse(ErrorResponse errorResponse) {
    return "{\"error\":\"" + escapeJson(errorResponse.getError()) + "\",\"errorDescription\":\""
        + escapeJson(errorResponse.getErrorDescription()) + "\"}";
  }

  private static String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private void logHandledException(Exchange exchange, LoggingLevel level, String errorCode,
      int statusCode) {
    Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
    String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY, String.class);
    String exceptionType = throwable == null ? UNKNOWN_VALUE : throwable.getClass().getName();
    String exceptionMessage = throwable == null ? "no exception captured" : throwable.getMessage();

    String structuredMessage = "webhook_ingestion_error code={} status={} connector_identifier={} exception_type={} message={}";
    if (level == LoggingLevel.ERROR) {
      LOGGER.error(structuredMessage, errorCode, statusCode,
          connectorIdentifier == null ? UNKNOWN_VALUE : connectorIdentifier, exceptionType,
          exceptionMessage, throwable);
      return;
    }
    LOGGER.warn(structuredMessage, errorCode, statusCode,
        connectorIdentifier == null ? UNKNOWN_VALUE : connectorIdentifier, exceptionType,
        exceptionMessage, throwable);
  }
}
