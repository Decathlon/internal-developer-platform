package com.decathlon.idp_core.domain.model.authorization;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;

/// Domain input describing a principal's request without infrastructure types.
public record AuthorizationRequest(@NotNull PrincipalInfo principal,
    @NotNull Optional<Entity> principalEntity, @NotNull AuthorizationAction action,
    @NotNull AuthorizationResource resource) {

  public AuthorizationRequest {
    Objects.requireNonNull(principal, "principal must not be null");
    Objects.requireNonNull(principalEntity, "principalEntity must not be null");
    Objects.requireNonNull(action, "action must not be null");
    Objects.requireNonNull(resource, "resource must not be null");
  }
}
