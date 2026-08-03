package com.decathlon.idp_core.domain.exception.webhook;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_ALREADY_EXIST;

public class WebhookConnectorAlreadyExistException extends RuntimeException {

  public WebhookConnectorAlreadyExistException(String identifier) {
    super(String.format("%s:%s", WEBHOOK_CONNECTOR_ALREADY_EXIST, identifier));
  }
}
