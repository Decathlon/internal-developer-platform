package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook;

import java.util.Map;

/// Security strategy returned for webhook configuration responses.
/// Only returns the strategy type to avoid exposing technical secret references.
public record InboundWebhookSecurityDtoOut(String type, Map<String, String> config) {

  public InboundWebhookSecurityDtoOut {
    config = config != null ? Map.copyOf(config) : null;
  }
}
