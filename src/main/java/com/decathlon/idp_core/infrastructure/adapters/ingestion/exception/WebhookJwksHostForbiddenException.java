package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;

/// Thrown when a JWKS host is rejected by the explicit allow-list policy.
public class WebhookJwksHostForbiddenException extends WebhookAuthenticationException {

  public WebhookJwksHostForbiddenException(String host) {
    super("JWKS host is not allow-listed: " + host);
  }
}
