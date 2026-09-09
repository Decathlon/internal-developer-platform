package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception;

public class WebhookAuthForbiddenException extends RuntimeException {
  public WebhookAuthForbiddenException(String message) {
    super(message);
  }

  public WebhookAuthForbiddenException(String message, Throwable cause) {
    super(message, cause);
  }
}
