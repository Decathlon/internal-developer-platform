package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_SECURITY_CONFIG_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_SECURITY_TYPE_MANDATORY;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// Security contract request payload represented as `{ type, config }`.
public record InboundWebhookSecurityContractDtoIn(
    @NotBlank(message = WEBHOOK_CONNECTOR_SECURITY_TYPE_MANDATORY) String type,
    @NotNull(message = WEBHOOK_CONNECTOR_SECURITY_CONFIG_MANDATORY) Map<String, String> config) {

  public InboundWebhookSecurityContractDtoIn {
    config = config != null ? Map.copyOf(config) : null;
  }
}
