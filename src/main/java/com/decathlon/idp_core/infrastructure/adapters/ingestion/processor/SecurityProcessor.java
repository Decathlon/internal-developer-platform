package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookSecurityException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.security.WebhookRequestAuthenticator;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecurityProcessor {

  private static final String WEBHOOK_AUTHENTICATION_FAILED_MESSAGE = "Webhook authentication failed";

  private final List<WebhookSecurityStrategy> strategies;

  public SecurityProcessor(List<WebhookSecurityStrategy> strategies) {
    this.strategies = List.copyOf(strategies);
  }

  public void validate(Map<String, Object> headers, Object rawPayload,
      WebhookConnector webhookConnector) {
    WebhookSecurity security = webhookConnector.security();
    if (security == null || security.type() == WebhookSecurityType.NONE) {
      return;
    }

    WebhookSecurityStrategy strategy = strategies.stream()
        .filter(candidate -> candidate.supports(security.type())).findFirst()
        .orElseThrow(() -> new WebhookSecurityException(WEBHOOK_AUTHENTICATION_FAILED_MESSAGE));

    if (!(strategy instanceof WebhookRequestAuthenticator authenticator)) {
      throw new WebhookSecurityException(WEBHOOK_AUTHENTICATION_FAILED_MESSAGE);
    }

    authenticator.validateRequest(headers, toByteArray(rawPayload), security.config());

    log.debug("Webhook security validation passed for connector '{}' with strategy '{}'.",
        webhookConnector.identifier(), security.type());
  }

  private byte[] toByteArray(Object payload) {
    if (payload == null) {
      return new byte[0];
    }
    return switch (payload) {
      case byte[] bytes -> bytes;
      case String string -> string.getBytes(StandardCharsets.UTF_8);
      default -> payload.toString().getBytes(StandardCharsets.UTF_8);
    };
  }

}
