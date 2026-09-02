package com.decathlon.idp_core.domain.exception.webhook;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_DISABLED_EXCEPTION_MESSAGE;

/// Thrown when a webhook connector is disabled and cannot process events.
///
/// This exception indicates that an incoming webhook event was received for a
/// connector that has been explicitly disabled and should not process events.
public class WebhookDisabledException extends RuntimeException {

  public WebhookDisabledException(String connectorIdentifier) {
    super(String.format(WEBHOOK_DISABLED_EXCEPTION_MESSAGE, connectorIdentifier));
  }

}
