package com.decathlon.idp_core.infrastructure.adapters.webhook.model;

import jakarta.validation.constraints.NotBlank;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

public record JwtBearerConfig(@NotBlank String jwksUri) implements SecurityConfig {

  @Override
  public WebhookSecurityType type() {
    return WebhookSecurityType.JWT_BEARER;
  }
}
