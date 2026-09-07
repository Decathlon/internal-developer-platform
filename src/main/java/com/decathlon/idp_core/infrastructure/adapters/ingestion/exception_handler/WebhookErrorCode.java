package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler;

import org.apache.camel.LoggingLevel;
import org.springframework.http.HttpStatus;

/**
 * Webhook ingestion error mapping for HTTP response and structured logging.
 */
public enum WebhookErrorCode {

  CONNECTOR_NOT_FOUND("webhook_connector_not_found", HttpStatus.NOT_FOUND, LoggingLevel.WARN,
      "Webhook configuration not found"),

  CONNECTOR_DISABLED("webhook_connector_disabled", HttpStatus.FORBIDDEN, LoggingLevel.WARN,
      "Webhook connector is disabled"),

  INVALID_ENCODED_PAYLOAD("invalid_encoded_payload", HttpStatus.BAD_REQUEST, LoggingLevel.WARN,
      "Invalid, unsupported, or oversized compressed payload"),

  CONFIGURATION_MISSING("webhook_configuration_missing", HttpStatus.INTERNAL_SERVER_ERROR,
      LoggingLevel.ERROR, "Webhook configuration unavailable"),

  UNEXPECTED_ERROR("webhook_ingestion_unexpected_error", HttpStatus.INTERNAL_SERVER_ERROR,
      LoggingLevel.ERROR, "Internal server error processing ingestion payload"),

  ENTITY_INGESTION_ERROR("webhook_ingestion_entity_error", HttpStatus.UNPROCESSABLE_CONTENT,
      LoggingLevel.ERROR, "Error while ingesting the entity");

  private final String code;
  private final HttpStatus httpStatus;
  private final LoggingLevel logLevel;
  private final String description;

  WebhookErrorCode(String code, HttpStatus httpStatus, LoggingLevel logLevel, String description) {
    this.code = code;
    this.httpStatus = httpStatus;
    this.logLevel = logLevel;
    this.description = description;
  }

  public String code() {
    return code;
  }

  public HttpStatus httpStatus() {
    return httpStatus;
  }

  public LoggingLevel logLevel() {
    return logLevel;
  }

  public String description() {
    return description;
  }
}
