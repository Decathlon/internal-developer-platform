package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookSecurityException;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecurityProcessor {

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
        .orElseThrow(() -> new WebhookSecurityException("No webhook security strategy registered for type: " + security.type()));

    boolean validated = strategy.validateRequest(headers, toByteArray(rawPayload), security.config());
    if (!validated) {
      throw new WebhookSecurityException("Webhook authentication failed for connector '%s' with strategy '%s'".formatted(webhookConnector.identifier(), security.type()));
    }

    log.debug("Webhook security validation passed for connector '{}' with strategy '{}'.", webhookConnector.identifier(), security.type());
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
