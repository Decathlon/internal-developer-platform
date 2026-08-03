package com.decathlon.idp_core.domain.exception.webhook;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_IDENTIFIER_NOT_FOUND;

public class WebhookConnectorNotFoundException extends RuntimeException {

  public WebhookConnectorNotFoundException(String identifier) {
    super(String.format(WEBHOOK_IDENTIFIER_NOT_FOUND, identifier));
  }
}
