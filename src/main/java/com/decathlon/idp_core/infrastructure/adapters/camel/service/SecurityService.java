package com.decathlon.idp_core.infrastructure.adapters.camel.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SecurityService {
  public boolean validate(String rawPayload, Map<String, Object> headers,
      WebhookConnector webhookConnector) {
    // Implement security validation logic here
    log.info("WebhookSecurity rawpalyload: {} strategy: {}", rawPayload,
        webhookConnector.security());
    return true;
  }
}
