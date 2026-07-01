package com.decathlon.idp_core.infrastructure.adapters.webhook.model;

import jakarta.validation.constraints.NotBlank;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

public record HmacConfig(@NotBlank String headerName, @NotBlank String secretAlias, String prefix,
    String encoding) implements SecurityConfig {

  public static final String DEFAULT_ENCODING = "hex";

  public HmacConfig {
    if (prefix == null) {
      prefix = "";
    }
    if (encoding == null || encoding.isBlank()) {
      encoding = DEFAULT_ENCODING;
    }
  }

  @Override
  public WebhookSecurityType type() {
    return WebhookSecurityType.HMAC_SHA256;
  }
}
