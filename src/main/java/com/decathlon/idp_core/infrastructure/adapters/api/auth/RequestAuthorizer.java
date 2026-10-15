package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.authorization.PrincipalNotAuthorizedException;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationAction;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationResource;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.service.authorization.GlobalAuthorizationService;
import com.decathlon.idp_core.domain.service.principal.PrincipalProvisioningService;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

import lombok.RequiredArgsConstructor;

/// Reapplies global authorization after a controller has resolved body-only resource context.
@Component
@RequiredArgsConstructor
public class RequestAuthorizer {

  private final PrincipalExtractor principalExtractor;
  private final PrincipalProvisioningService provisioningService;
  private final GlobalAuthorizationService authorizationService;
  private final AuthorizationPolicy authorizationPolicy;

  /// Authorizes a sensitive operation using its fully resolved resource context.
  ///
  /// @param request current HTTP request
  /// @param action operation being performed
  /// @param resource resource context resolved from the request body
  public void authorize(HttpServletRequest request, AuthorizationAction action,
      AuthorizationResource resource) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new PrincipalNotAuthorizedException("unknown");
    }

    Object context = request.getAttribute(ProvisionedPrincipalContext.REQUEST_ATTRIBUTE);
    ProvisionedPrincipalContext provisionedContext = context instanceof ProvisionedPrincipalContext value
        ? value
        : null;
    PrincipalInfo principal = provisionedContext != null
        ? provisionedContext.principal()
        : principalExtractor.extractPrincipalInfo(authentication);
    Optional<Entity> principalEntity = provisionedContext != null
        ? provisionedContext.principalEntity()
        : provisioningService.getPrincipal(principal.identifier());

    authorizationService.authorize(
        new AuthorizationRequest(principal, principalEntity, action, resource),
        authorizationPolicy);
  }
}
