package com.decathlon.idp_core.infrastructure.adapters.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.security.Action;
import com.decathlon.idp_core.domain.service.security.GlobalAuthorizationService;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthorizationProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

import lombok.RequiredArgsConstructor;

/// Infrastructure bridge from Spring Security authentication to domain policy.
@Component
@RequiredArgsConstructor
public class PermissionEvaluator {

  private final GlobalAuthorizationService globalAuthorizationService;
  private final AuthorizationProperties properties;
  private final PrincipalExtractor principalExtractor;

  public boolean hasPermission(Authentication authentication, Object targetDomainObject,
      Object permission) {
    if (authentication == null || !authentication.isAuthenticated() || properties
        .mode() != com.decathlon.idp_core.domain.model.security.AuthorizationMode.GLOBAL) {
      return false;
    }

    Action action = Action.valueOf(permission.toString().toUpperCase());
    return globalAuthorizationService
        .isAuthorizedInStep1(principalExtractor.extractPrincipalInfo(authentication), action);
  }
}
