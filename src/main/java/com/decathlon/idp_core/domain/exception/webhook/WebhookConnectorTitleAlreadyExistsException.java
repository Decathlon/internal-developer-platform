package com.decathlon.idp_core.domain.exception.webhook;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_TITLE_ALREADY_EXIST;

public class WebhookConnectorTitleAlreadyExistsException extends RuntimeException {
  public WebhookConnectorTitleAlreadyExistsException(String webhookName) {
    super(String.format("%s:%s", WEBHOOK_CONNECTOR_TITLE_ALREADY_EXIST, webhookName));
  }
}
