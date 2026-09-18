package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;

public class WebhookAuthForbiddenException extends WebhookAuthenticationException {

  public WebhookAuthForbiddenException(String message) {
    super(message);
  }

  public WebhookAuthForbiddenException(String message, Throwable cause) {
    super(message, cause);
  }
}
