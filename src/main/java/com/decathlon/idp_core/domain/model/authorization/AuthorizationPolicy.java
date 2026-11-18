package com.decathlon.idp_core.domain.model.authorization;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.decathlon.idp_core.domain.constant.ValidationMessages;

/// Immutable authorization configuration used by domain authorization services.
public record AuthorizationPolicy(AuthorizationMode mode, Set<String> globalPrincipalIdentifiers,
    Optional<String> requiredPrincipalProperty) {

  public AuthorizationPolicy {
    Objects.requireNonNull(mode, ValidationMessages.authorizationValueMustNotBeNull("mode"));
    globalPrincipalIdentifiers = globalPrincipalIdentifiers != null
        ? Set.copyOf(globalPrincipalIdentifiers)
        : Set.of();
    Objects.requireNonNull(requiredPrincipalProperty,
        ValidationMessages.authorizationValueMustNotBeNull("requiredPrincipalProperty"));
    requiredPrincipalProperty = requiredPrincipalProperty.map(String::trim)
        .filter(propertyName -> !propertyName.isBlank());
  }
}
