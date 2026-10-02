package com.decathlon.idp_core.domain.model.authorization;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/// Immutable authorization configuration used by domain authorization services.
public record AuthorizationPolicy(AuthorizationMode mode, Set<String> globalPrincipalIdentifiers,
    Optional<String> requiredPrincipalProperty) {

  public AuthorizationPolicy {
    Objects.requireNonNull(mode, "mode must not be null");
    globalPrincipalIdentifiers = globalPrincipalIdentifiers != null
        ? Set.copyOf(globalPrincipalIdentifiers)
        : Set.of();
    Objects.requireNonNull(requiredPrincipalProperty, "requiredPrincipalProperty must not be null");
    requiredPrincipalProperty = requiredPrincipalProperty.map(String::trim)
        .filter(propertyName -> !propertyName.isBlank());
  }
}
