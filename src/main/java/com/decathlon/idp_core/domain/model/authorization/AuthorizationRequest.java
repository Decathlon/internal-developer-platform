package com.decathlon.idp_core.domain.model.authorization;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;

/// Domain input describing a principal's request without infrastructure types.
public record AuthorizationRequest(@NotNull PrincipalInfo principal,
    @NotNull Optional<Entity> principalEntity, @NotNull AuthorizationAction action,
    @NotNull AuthorizationResource resource) {

  public AuthorizationRequest {
    Objects.requireNonNull(principal,
        ValidationMessages.authorizationValueMustNotBeNull("principal"));
    Objects.requireNonNull(principalEntity,
        ValidationMessages.authorizationValueMustNotBeNull("principalEntity"));
    Objects.requireNonNull(action, ValidationMessages.authorizationValueMustNotBeNull("action"));
    Objects.requireNonNull(resource,
        ValidationMessages.authorizationValueMustNotBeNull("resource"));
  }
}
