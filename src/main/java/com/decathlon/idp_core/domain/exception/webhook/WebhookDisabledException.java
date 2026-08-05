package com.decathlon.idp_core.domain.exception.webhook;

/// Thrown when a webhook connector is disabled and cannot process events.
///
/// This exception indicates that an incoming webhook event was received for a
/// connector that has been explicitly disabled and should not process events.
public class WebhookDisabledException extends RuntimeException {

  private final String connectorIdentifier;

  public WebhookDisabledException(String connectorIdentifier) {
    super(String.format("Webhook connector '%s' is disabled and cannot process events",
        connectorIdentifier));
    this.connectorIdentifier = connectorIdentifier;
  }

  public String getConnectorIdentifier() {
    return connectorIdentifier;
  }
}
