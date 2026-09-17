package com.decathlon.idp_core.domain.service.security;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.security.Action;
import com.decathlon.idp_core.domain.port.AuthorizationPolicyPort;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;

import lombok.RequiredArgsConstructor;

/// Evaluates the Phase 1 global authorization policy.
@Service
@RequiredArgsConstructor
public class GlobalAuthorizationService {

  private final AuthorizationPolicyPort authorizationPolicyPort;
  private final EntityRepositoryPort entityRepositoryPort;

  /// Returns whether the principal may perform the requested action.
  public boolean isAuthorizedInStep1(PrincipalInfo principal, Action action) {
    if (authorizationPolicyPort.globalPrincipalIdentifiers().contains(principal.identifier())) {
      return true;
    }

    if (isAdmin(principal.identifier())) {
      return true;
    }

    return action == Action.READ;
  }

  private boolean isAdmin(String identifier) {
    return entityRepositoryPort.findByTemplateIdentifierAndIdentifier("principal", identifier)
        .flatMap(principal -> principal.properties().stream()
            .filter(property -> "is_admin".equals(property.name()))
            .map(property -> property.value()).findFirst())
        .map(Boolean::parseBoolean).orElse(false);
  }
}
