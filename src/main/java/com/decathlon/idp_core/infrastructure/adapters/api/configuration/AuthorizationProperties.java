package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.decathlon.idp_core.domain.model.authorization.AuthorizationMode;

/// Authorization settings bound from `app.security.authorization`.
@ConfigurationProperties(prefix = "app.security.authorization")
public record AuthorizationProperties(AuthorizationMode mode,
    List<String> globalPrincipalIdentifiers) {

  public AuthorizationProperties {
    mode = mode != null ? mode : AuthorizationMode.GLOBAL;
    globalPrincipalIdentifiers = globalPrincipalIdentifiers != null
        ? globalPrincipalIdentifiers.stream()
            .filter(identifier -> identifier != null && !identifier.isBlank()).map(String::trim)
            .toList()
        : List.of();
  }
}
