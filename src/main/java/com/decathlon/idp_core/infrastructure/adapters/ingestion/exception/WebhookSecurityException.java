package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception;

public class WebhookSecurityException extends RuntimeException {

  public WebhookSecurityException(String message) {
    super(message);
  }

  public WebhookSecurityException(String message, Throwable cause) {
    super(message, cause);
  }
}
