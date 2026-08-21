package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.APPLICATION_JSON;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONNECTOR_IDENTIFIER_PROPERTY;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.UNKNOWN_VALUE;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.decathlon.idp_core.infrastructure.adapters.common.model.ErrorResponse;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// Helper component providing reusable exception mapping and structured logging for Camel routes.
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookExceptionHandlerHelper {

  private final ObjectMapper objectMapper;

  /// Binds an exception type to a standardized HTTP error response and structured
  /// log.
  public <T extends Throwable> void registerHandler(RouteBuilder routeBuilder,
      Class<T> exceptionType, WebhookErrorCode error) {

    routeBuilder.onException(exceptionType).handled(true)
        .process(exchange -> setJsonErrorResponse(exchange, error.code(), error.httpStatus(),
            resolveErrorDescription(exchange, error.description())))
        .process(exchange -> logHandledException(exchange, error.logLevel(), error.code(),
            error.httpStatus().value()));
  }

  private void setJsonErrorResponse(Exchange exchange, String errorCode, HttpStatus httpStatus,
      String errorDescription) throws JsonProcessingException {
    ErrorResponse errorResponse = new ErrorResponse(errorCode, errorDescription);

    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, httpStatus.value());
    exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, APPLICATION_JSON);
    exchange.getMessage().setBody(objectMapper.writeValueAsString(errorResponse));
  }

  private String resolveErrorDescription(Exchange exchange, String fallbackDescription) {
    Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
    if ((throwable instanceof WebhookAuthUnauthorizedException
        || throwable instanceof WebhookAuthForbiddenException)
        && StringUtils.hasText(throwable.getMessage())) {
      return throwable.getMessage();
    }
    return fallbackDescription;
  }

  private void logHandledException(Exchange exchange, LoggingLevel level, String errorCode,
      int statusCode) {
    Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
    String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY, String.class);
    String targetIdentifier = connectorIdentifier == null ? UNKNOWN_VALUE : connectorIdentifier;
    String exceptionType = throwable == null ? UNKNOWN_VALUE : throwable.getClass().getName();
    String exceptionMessage = throwable == null ? "no exception captured" : throwable.getMessage();

    String structuredMessage = "webhook_ingestion_error code={} status={} connector_identifier={} exception_type={} message={}";

    if (level == LoggingLevel.ERROR) {
      log.error(structuredMessage, errorCode, statusCode, targetIdentifier, exceptionType,
          exceptionMessage, throwable);
      return;
    }

    if (log.isDebugEnabled()) {
      log.warn(structuredMessage, errorCode, statusCode, targetIdentifier, exceptionType,
          exceptionMessage, throwable);
    } else {
      log.warn(structuredMessage, errorCode, statusCode, targetIdentifier, exceptionType,
          exceptionMessage);
    }
  }
}
