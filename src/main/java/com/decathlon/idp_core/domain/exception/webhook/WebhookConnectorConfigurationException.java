package com.decathlon.idp_core.domain.exception.webhook;

public class WebhookConnectorConfigurationException extends RuntimeException {
  public WebhookConnectorConfigurationException(String message) {
    super(message);
  }
}
