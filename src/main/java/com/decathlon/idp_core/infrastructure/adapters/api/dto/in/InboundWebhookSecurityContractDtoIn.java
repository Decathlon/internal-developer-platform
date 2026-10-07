package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_SECURITY_TYPE_MANDATORY;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

/// Security contract request payload represented as `{ type, config }`.
///
/// `config` is optional here: it is only mandatory for strategies other than
/// `NONE`, which the domain enforces.
public record InboundWebhookSecurityContractDtoIn(
    @NotBlank(message = WEBHOOK_CONNECTOR_SECURITY_TYPE_MANDATORY) String type,
    Map<String, String> config) {

  public InboundWebhookSecurityContractDtoIn {
    config = config != null ? Map.copyOf(config) : null;
  }
}
