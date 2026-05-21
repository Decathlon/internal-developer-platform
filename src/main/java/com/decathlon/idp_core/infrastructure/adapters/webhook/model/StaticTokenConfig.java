package com.decathlon.idp_core.infrastructure.adapters.webhook.model;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

import jakarta.validation.constraints.NotBlank;

public record StaticTokenConfig(
        @NotBlank String headerName,
        @NotBlank String secretAlias
) implements SecurityConfig {

    @Override
    public WebhookSecurityType type() {
        return WebhookSecurityType.STATIC_TOKEN;
    }
}
