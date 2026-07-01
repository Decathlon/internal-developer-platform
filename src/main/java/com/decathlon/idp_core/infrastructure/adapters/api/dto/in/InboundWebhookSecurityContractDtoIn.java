package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// Security contract request payload represented as `{ type, config }`.
public record InboundWebhookSecurityContractDtoIn(
    @NotBlank(message = "Webhook security type is mandatory") String type,
    @NotNull(message = "Webhook security config section is mandatory") Map<String, String> config) {

  public InboundWebhookSecurityContractDtoIn {
    config = config != null ? Map.copyOf(config) : null;
  }
}
