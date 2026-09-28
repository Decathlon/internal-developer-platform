package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;

public class WebhookAuthUnauthorizedException extends WebhookAuthenticationException {

  public WebhookAuthUnauthorizedException(String message) {
    super(message);
  }
  public WebhookAuthUnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }
}
