package com.decathlon.idp_core.domain.exception.webhook;

public class WebhookConnectorNotFoundException extends RuntimeException {

    public WebhookConnectorNotFoundException(String identifier) {
        super(String.format("No webhook connector found for identifier: %s", identifier));
    }
}
