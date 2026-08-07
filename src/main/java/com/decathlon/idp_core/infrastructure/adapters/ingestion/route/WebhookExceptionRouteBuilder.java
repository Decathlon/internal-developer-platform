package com.decathlon.idp_core.infrastructure.adapters.ingestion.route;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.APPLICATION_JSON;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONNECTOR_IDENTIFIER_PROPERTY;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.ERROR_DESCRIPTION_INTERNAL_SERVER_ERROR;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.ERROR_DESCRIPTION_WEBHOOK_CONFIGURATION_MISSING;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.ERROR_DESCRIPTION_WEBHOOK_DISABLED;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.ERROR_DESCRIPTION_WEBHOOK_NOT_FOUND;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.UNKNOWN_VALUE;

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
import com.decathlon.idp_core.infrastructure.adapters.common.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebhookExceptionRouteBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebhookExceptionRouteBuilder.class);

  private final ObjectMapper objectMapper;

  public void configureExceptions(RouteBuilder routeBuilder) {
    routeBuilder.onException(WebhookConnectorNotFoundException.class).handled(true)
        .process(exchange -> setJsonErrorResponse(exchange, HttpStatus.NOT_FOUND,
            ERROR_DESCRIPTION_WEBHOOK_NOT_FOUND))
        .process(exchange -> logHandledException(exchange, LoggingLevel.WARN,
            "webhook_connector_not_found", HttpStatus.NOT_FOUND.value()));

    routeBuilder.onException(WebhookDisabledException.class).handled(true)
        .process(exchange -> setJsonErrorResponse(exchange, HttpStatus.FORBIDDEN,
            ERROR_DESCRIPTION_WEBHOOK_DISABLED))
        .process(exchange -> logHandledException(exchange, LoggingLevel.WARN,
            "webhook_connector_disabled", HttpStatus.FORBIDDEN.value()));

    routeBuilder.onException(WebhookConfigurationMissingException.class).handled(true)
        .process(exchange -> setJsonErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
            ERROR_DESCRIPTION_WEBHOOK_CONFIGURATION_MISSING))
        .process(exchange -> logHandledException(exchange, LoggingLevel.ERROR,
            "webhook_configuration_missing", HttpStatus.INTERNAL_SERVER_ERROR.value()));

    routeBuilder.onException(Exception.class).handled(true)
        .process(exchange -> setJsonErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
            ERROR_DESCRIPTION_INTERNAL_SERVER_ERROR))
        .process(exchange -> logHandledException(exchange, LoggingLevel.ERROR,
            "webhook_ingestion_unexpected_error", HttpStatus.INTERNAL_SERVER_ERROR.value()));
  }

  private void setJsonErrorResponse(Exchange exchange, HttpStatus httpStatus,
      String errorDescription) throws Exception {
    ErrorResponse errorResponse = new ErrorResponse(httpStatus.name(), errorDescription);

    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, httpStatus.value());
    exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, APPLICATION_JSON);

    // Serialize with Jackson to guarantee valid JSON escaping and field encoding.
    exchange.getMessage().setBody(objectMapper.writeValueAsString(errorResponse));
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
