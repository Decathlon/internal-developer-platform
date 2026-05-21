package com.decathlon.idp_core.domain.exception.webhook;

public class WebhookAuthenticationException extends RuntimeException {
    public WebhookAuthenticationException(String message) {
        super(message);
    }

    public WebhookAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
