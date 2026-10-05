package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
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
import com.decathlon.idp_core.infrastructure.adapters.api.exception.MissingPrincipalIdentifierException;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiErrorResponseWriter;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

import lombok.extern.slf4j.Slf4j;

/// Enforces the global authorization policy for authenticated API requests.
@Slf4j
@Component
public class GlobalAuthorizationFilter extends OncePerRequestFilter {

  private final PrincipalExtractor principalExtractor;
  private final PrincipalProvisioningService provisioningService;
  private final GlobalAuthorizationService authorizationService;
  private final AuthorizationPolicy authorizationPolicy;
  private final AuthorizationRequestFactory authorizationRequestFactory;
  private final ApiErrorResponseWriter errorResponseWriter;

  /// Creates a filter backed by the domain authorization service and configured
  /// policy.
  ///
  /// @param principalExtractor converts Spring authentication into a domain
  /// principal
  /// @param provisioningService retrieves the principal's catalog entity
  /// @param authorizationService evaluates the authorization policy
  /// @param authorizationPolicy configured policy for authenticated requests
  /// @param authorizationRequestFactory maps HTTP input to domain authorization
  /// context
  /// @param errorResponseWriter writes standard JSON errors at the filter
  /// boundary
  public GlobalAuthorizationFilter(PrincipalExtractor principalExtractor,
      PrincipalProvisioningService provisioningService,
      GlobalAuthorizationService authorizationService, AuthorizationPolicy authorizationPolicy,
      AuthorizationRequestFactory authorizationRequestFactory,
      ApiErrorResponseWriter errorResponseWriter) {
    this.principalExtractor = principalExtractor;
    this.provisioningService = provisioningService;
    this.authorizationService = authorizationService;
    this.authorizationPolicy = authorizationPolicy;
    this.authorizationRequestFactory = authorizationRequestFactory;
    this.errorResponseWriter = errorResponseWriter;
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

    var context = request.getAttribute(ProvisionedPrincipalContext.REQUEST_ATTRIBUTE);
    var principalContext = context instanceof ProvisionedPrincipalContext provisionedContext
        ? provisionedContext
        : null;
    Optional<PrincipalInfo> principalInfo = principalContext != null
        ? Optional.of(principalContext.principal())
        : extractPrincipal(authentication, response);
    if (principalInfo.isEmpty()) {
      return;
    }
    PrincipalInfo principal = principalInfo.orElseThrow();
    var principalEntity = principalContext != null
        ? principalContext.principalEntity()
        : provisioningService.getPrincipal(principal.identifier());
    AuthorizationRequest authorizationRequest = authorizationRequestFactory.create(request,
        principal, principalEntity);

    try {
      authorizationService.authorize(authorizationRequest, authorizationPolicy);
    } catch (PrincipalNotAuthorizedException _) {
      log.warn("Authorization denied for principal {} on {} {}", principal.identifier(),
          request.getMethod(), request.getRequestURI());
      errorResponseWriter.write(response, HttpStatus.FORBIDDEN,
          "Principal is not authorized to perform this operation");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private Optional<PrincipalInfo> extractPrincipal(Authentication authentication,
      HttpServletResponse response) throws IOException {
    try {
      return Optional.of(principalExtractor.extractPrincipalInfo(authentication));
    } catch (MissingPrincipalIdentifierException exception) {
      log.warn("Authentication rejected: {}", exception.getMessage());
      errorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, exception.getMessage());
      return Optional.empty();
    }
  }
}
