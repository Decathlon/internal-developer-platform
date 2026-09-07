package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception;

/// Exception raised when an inbound webhook payload cannot be decoded or decompressed.
public class WebhookDecodingException extends RuntimeException {

  public WebhookDecodingException(String message) {
    super(message);
  }

  public WebhookDecodingException(String message, Throwable cause) {
    super(message, cause);
  }
}
