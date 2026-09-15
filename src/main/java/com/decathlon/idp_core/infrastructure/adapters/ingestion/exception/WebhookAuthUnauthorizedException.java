package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception;

public class WebhookAuthUnauthorizedException extends RuntimeException {
  public WebhookAuthUnauthorizedException(String message) {
    super(message);
  }
  public WebhookAuthUnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }
}
