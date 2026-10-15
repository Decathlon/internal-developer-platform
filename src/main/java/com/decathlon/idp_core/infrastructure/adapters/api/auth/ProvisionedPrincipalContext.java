package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.util.Optional;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;

/// Request-scoped result shared by JIT provisioning and global authorization.
public record ProvisionedPrincipalContext(PrincipalInfo principal,
    Optional<Entity> principalEntity) {

  public static final String REQUEST_ATTRIBUTE = ProvisionedPrincipalContext.class.getName();
}
