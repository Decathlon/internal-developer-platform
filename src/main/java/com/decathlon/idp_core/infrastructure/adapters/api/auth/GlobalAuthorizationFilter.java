package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.decathlon.idp_core.domain.exception.authorization.PrincipalNotAuthorizedException;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.service.authorization.GlobalAuthorizationService;
import com.decathlon.idp_core.domain.service.principal.PrincipalProvisioningService;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

import lombok.extern.slf4j.Slf4j;

/// Enforces the global authorization policy for authenticated JWT and mock requests.
@Slf4j
@Component
public class GlobalAuthorizationFilter extends OncePerRequestFilter {

  private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

  private final PrincipalExtractor principalExtractor;
  private final PrincipalProvisioningService provisioningService;
  private final GlobalAuthorizationService authorizationService;
  private final AuthorizationPolicy authorizationPolicy;

  /// Creates a filter backed by the domain authorization service and configured
  /// policy.
  ///
  /// @param principalExtractor converts Spring authentication into a domain
  /// principal
  /// @param provisioningService retrieves the principal's catalog entity
  /// @param authorizationService evaluates the authorization policy
  /// @param authorizationPolicy configured policy for authenticated requests
  public GlobalAuthorizationFilter(PrincipalExtractor principalExtractor,
      PrincipalProvisioningService provisioningService,
      GlobalAuthorizationService authorizationService, AuthorizationPolicy authorizationPolicy) {
    this.principalExtractor = principalExtractor;
    this.provisioningService = provisioningService;
    this.authorizationService = authorizationService;
    this.authorizationPolicy = authorizationPolicy;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      filterChain.doFilter(request, response);
      return;
    }

    PrincipalInfo principal = principalExtractor.extractPrincipalInfo(authentication);
    var principalEntity = provisioningService.getPrincipal(principal.identifier());
    AuthorizationRequest authorizationRequest = new AuthorizationRequest(principal, principalEntity,
        READ_METHODS.contains(request.getMethod()));

    try {
      authorizationService.authorize(authorizationRequest, authorizationPolicy);
    } catch (PrincipalNotAuthorizedException exception) {
      log.warn("Authorization denied for principal {} on {} {}", principal.identifier(),
          request.getMethod(), request.getRequestURI());
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    filterChain.doFilter(request, response);
  }
}
