package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.*;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.common.model.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// Helper component providing reusable exception mapping and structured logging
/// for Camel routes.
///
/// Supports binding single or multiple exception types to standardized HTTP
/// error responses with structured logging. Exception messages can optionally be
/// exposed to clients when safe.
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookExceptionHandlerHelper {

  private final ObjectMapper objectMapper;

  /// Binds an exception type to a standardized HTTP error response and
  /// structured log.
  public <T extends Throwable> void registerHandler(RouteBuilder routeBuilder,
      Class<T> exceptionType, WebhookErrorCode error) {
    registerHandler(routeBuilder, exceptionType, error, false);
  }

  /// Binds an exception type and optionally exposes the exception message in the
  /// HTTP response when the exception message is controlled and safe for clients.
  public <T extends Throwable> void registerHandler(RouteBuilder routeBuilder,
      Class<T> exceptionType, WebhookErrorCode error, boolean exposeExceptionMessage) {

    routeBuilder.onException(exceptionType).handled(true).removeHeaders("*")
        .process(exchange -> setJsonErrorResponse(exchange, error, exposeExceptionMessage))
        .process(exchange -> logHandledException(exchange, error.logLevel(), error.code(),
            error.httpStatus().value()));
  }

  /// Binds multiple exception types to a standardized HTTP error response and
  /// structured log. All provided exception classes are registered with the same
  /// error code, not exposing the exception message.
  @SuppressWarnings("unchecked")
  public void registerHandlers(RouteBuilder routeBuilder, WebhookErrorCode error,
      Class<? extends Throwable>... exceptionTypes) {
    registerHandlers(routeBuilder, error, false, exceptionTypes);
  }

  /// Binds multiple exception types and optionally exposes exception messages in
  /// HTTP responses.
  ///
  /// Useful for grouping semantically related exceptions that should be handled
  /// identically. For example: register all JSLT validation errors with
  /// ENTITY_INGESTION_ERROR code.
  @SuppressWarnings("unchecked")
  public void registerHandlers(RouteBuilder routeBuilder, WebhookErrorCode error,
      boolean exposeExceptionMessage, Class<? extends Throwable>... exceptionTypes) {

    for (var exceptionType : exceptionTypes) {
      registerHandler(routeBuilder, exceptionType, error, exposeExceptionMessage);
    }
  }

  private void setJsonErrorResponse(Exchange exchange, WebhookErrorCode error,
      boolean exposeExceptionMessage) throws JsonProcessingException {
    HttpStatus httpStatus = error.httpStatus();
    String errorDescription = resolveErrorDescription(exchange, error, exposeExceptionMessage);
    ErrorResponse errorResponse = new ErrorResponse(httpStatus.name(), errorDescription);
    Message message = exchange.getMessage();
    message.setHeader(Exchange.HTTP_RESPONSE_CODE, httpStatus.value());
    message.setHeader(Exchange.CONTENT_TYPE, APPLICATION_JSON);
    message.setBody(objectMapper.writeValueAsString(errorResponse));
  }

  private String resolveErrorDescription(Exchange exchange, WebhookErrorCode error,
      boolean exposeExceptionMessage) {
    if (!exposeExceptionMessage) {
      return error.description();
    }

    Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
    if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
      return error.description();
    }
    return throwable.getMessage();
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
