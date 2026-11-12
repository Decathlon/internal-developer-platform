package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.util.Map;

/// Security contract request payload represented as `{ type, config }`.
///
/// `type` may be omitted for disabled connectors, which the mapper interprets as
/// `NONE`. `config` is optional for `NONE` and mandatory for other strategies.
public record InboundWebhookSecurityContractDtoIn(String type, Map<String, String> config) {

  public InboundWebhookSecurityContractDtoIn {
    config = config != null ? Map.copyOf(config) : null;
  }
}
