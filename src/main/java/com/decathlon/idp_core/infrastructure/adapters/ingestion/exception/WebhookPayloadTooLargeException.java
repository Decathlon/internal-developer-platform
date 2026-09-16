package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/// Thrown when a webhook request body exceeds the maximum allowed size.
/// This prevents DoS attacks via arbitrarily large payloads.
@ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
public class WebhookPayloadTooLargeException extends RuntimeException {

  public WebhookPayloadTooLargeException(String message) {
    super(message);
  }
}
