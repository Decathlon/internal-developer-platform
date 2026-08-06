package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecurityProcessor {
  public boolean validate(Map<String, Object> headers, WebhookConnector webhookConnector) {
    // Implement security validation logic here
    log.info("WebhookSecurity headers: {} strategy: {}", headers, webhookConnector.security());
    return true;
  }
}
