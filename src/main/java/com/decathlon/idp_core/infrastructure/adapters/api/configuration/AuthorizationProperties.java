package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.decathlon.idp_core.domain.model.security.AuthorizationMode;

/// Type-safe configuration for global authorization and break-glass access.
@ConfigurationProperties(prefix = "app.security.authorization")
public record AuthorizationProperties(AuthorizationMode mode,
    Set<String> globalPrincipalIdentifiers) {

  public AuthorizationProperties {
    mode = mode == null ? AuthorizationMode.GLOBAL : mode;
    globalPrincipalIdentifiers = globalPrincipalIdentifiers == null
        ? Set.of()
        : globalPrincipalIdentifiers.stream()
            .filter(identifier -> identifier != null && !identifier.isBlank())
            .collect(Collectors.toUnmodifiableSet());
  }

  /// Parses comma-separated environment variable values for relaxed binding.
  public static Set<String> identifiers(String value) {
    return value == null
        ? Set.of()
        : Arrays.stream(value.split(",")).map(String::trim)
            .filter(identifier -> !identifier.isBlank()).collect(Collectors.toUnmodifiableSet());
  }
}
