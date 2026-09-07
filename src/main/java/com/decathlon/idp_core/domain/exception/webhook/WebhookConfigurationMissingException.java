package com.decathlon.idp_core.domain.exception.webhook;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONFIGURATION_MISSING;

/**
 * Thrown when ingestion validation is executed without a loaded webhook
 * configuration.
 */
public class WebhookConfigurationMissingException extends RuntimeException {

  /**
   * Creates a new exception for missing webhook configuration.
   *
   * @param connectorIdentifier
   *          webhook identifier used during ingestion
   */
  public WebhookConfigurationMissingException(String connectorIdentifier) {
    super(String.format(WEBHOOK_CONFIGURATION_MISSING, connectorIdentifier));
  }

}
