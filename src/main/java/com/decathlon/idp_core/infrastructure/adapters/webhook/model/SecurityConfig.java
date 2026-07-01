package com.decathlon.idp_core.infrastructure.adapters.webhook.model;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = HmacConfig.class, name = "HMAC_SHA256"),
    @JsonSubTypes.Type(value = StaticTokenConfig.class, name = "STATIC_TOKEN"),
    @JsonSubTypes.Type(value = BasicAuthConfig.class, name = "BASIC_AUTH"),
    @JsonSubTypes.Type(value = JwtBearerConfig.class, name = "JWT_BEARER")})
public interface SecurityConfig {
  WebhookSecurityType type();
}
