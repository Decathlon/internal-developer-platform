package com.decathlon.idp_core.infrastructure.adapters.webhook.model;

import jakarta.validation.constraints.NotBlank;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

public record BasicAuthConfig(@NotBlank String username,
    @NotBlank String secretAlias) implements SecurityConfig {

  @Override
  public WebhookSecurityType type() {
    return WebhookSecurityType.BASIC_AUTH;
  }
}
