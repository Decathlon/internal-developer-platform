package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPatternParser;

import com.decathlon.idp_core.domain.model.authorization.AuthorizationMode;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.service.principal.PrincipalProvisioningService;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.PrincipalProvisioningMode;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.PrincipalProvisioningProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SecurityConfiguration;
import com.decathlon.idp_core.infrastructure.adapters.api.exception.MissingPrincipalIdentifierException;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiErrorResponseWriter;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// Infrastructure filter that triggers Just-In-Time (JIT) provisioning of principals.
///
/// **Purpose:** Intercepts every authenticated request to ensure the authenticated
/// principal has a corresponding Entity in the catalog. This enables:
/// - Automatic user onboarding (no manual account creation)
/// - Real-time profile synchronization from identity provider
/// - Unified identity management for humans and service accounts
///
/// **Design rationale:** Positioned after Spring Security authentication but before
/// controllers, ensuring JIT provisioning happens transparently for all API endpoints.
/// Provisioning failures reject the request so authorization cannot proceed without a
/// verified principal link.
///
/// **Excluded Paths:** Public endpoints (health checks, documentation, etc.) are excluded
/// from JIT provisioning to avoid unnecessary database queries and log pollution.
/// Excluded paths are configured via `app.security.authentication.jit-provisioning-excluded-paths`
/// in `application.yml`. **Important:** Keep these paths in sync with SecurityConfiguration's
/// permitAll() matchers to maintain consistent security policy.
///
/// @see AuthenticationProperties
/// @see SecurityConfiguration
@Slf4j
@Component
@RequiredArgsConstructor
public class JitProvisioningFilter extends OncePerRequestFilter {

  private static final PathPatternParser PATTERN_PARSER = new PathPatternParser();

  private final PrincipalExtractor principalExtractor;
  private final PrincipalProvisioningService provisioningService;
  private final AuthenticationProperties authProperties;
  private final PrincipalProvisioningProperties provisioningProperties;
  private final AuthorizationPolicy authorizationPolicy;
  private final ApiErrorResponseWriter errorResponseWriter;

  /// Verifies an authenticated principal before allowing the request to continue.
  ///
  /// Unauthenticated and anonymous requests pass through for the configured
  /// security
  /// chain to handle. Authenticated requests stop here if extraction or
  /// provisioning
  /// fails, or if the principal cannot be linked.
  ///
  /// @param request current HTTP request
  /// @param response current HTTP response
  /// @param filterChain remaining filters and request handler
  /// @throws ServletException if a downstream filter fails
  /// @throws IOException if request processing fails
  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.isAuthenticated() && !isAnonymous(authentication)
        && !provisionPrincipalSafely(request, response, authentication)) {
      return;
    }

    filterChain.doFilter(request, response);
  }

  /// Extracts the authenticated identity and applies the configured
  /// principal-linking mode.
  ///
  /// On success, stores the verified principal context on the request for
  /// authorization.
  /// Missing links are rejected, while unexpected provisioning failures return a
  /// service
  /// unavailable response and prevent the request from continuing.
  ///
  /// @param request current HTTP request
  /// @param response current HTTP response
  /// @param authentication Spring Security authentication
  /// @return `true` when principal verification succeeds; otherwise `false`
  /// @throws IOException if writing an error response fails
  private boolean provisionPrincipalSafely(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {
    PrincipalInfo principalInfo;
    try {
      principalInfo = principalExtractor.extractPrincipalInfo(authentication);
    } catch (MissingPrincipalIdentifierException exception) {
      log.warn("Authentication rejected: {}", exception.getMessage());
      errorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, exception.getMessage());
      return false;
    }

    try {
      if (provisioningProperties != null
          && provisioningProperties.mode() == PrincipalProvisioningMode.LINK_ONLY) {
        Optional<Entity> linkedPrincipal = provisioningService
            .findPrincipalWithSameKind(principalInfo);
        if (linkedPrincipal.isEmpty()) {
          if (authorizationPolicy.mode() == AuthorizationMode.GLOBAL && authorizationPolicy
              .globalPrincipalIdentifiers().contains(principalInfo.identifier())) {
            Entity globalPrincipal = provisioningService
                .provisionGlobalAdministrator(principalInfo);
            log.debug(
                "Principal evaluation accepted: mode={}, principal={}, kind={}, linked=true, "
                    + "global=true",
                PrincipalProvisioningMode.LINK_ONLY, principalInfo.identifier(),
                principalInfo.kind());
            request.setAttribute(ProvisionedPrincipalContext.REQUEST_ATTRIBUTE,
                new ProvisionedPrincipalContext(principalInfo, Optional.of(globalPrincipal)));
            return true;
          }
          log.debug("Principal evaluation rejected: mode={}, principal={}, kind={}, linked=false",
              PrincipalProvisioningMode.LINK_ONLY, principalInfo.identifier(),
              principalInfo.kind());
          errorResponseWriter.write(response, HttpStatus.UNAUTHORIZED,
              "No linked principal matches the authenticated identity");
          return false;
        }
        log.debug("Principal evaluation accepted: mode={}, principal={}, kind={}, linked=true",
            PrincipalProvisioningMode.LINK_ONLY, principalInfo.identifier(), principalInfo.kind());
        request.setAttribute(ProvisionedPrincipalContext.REQUEST_ATTRIBUTE,
            new ProvisionedPrincipalContext(principalInfo, linkedPrincipal));
        return true;
      }

      provisioningService.provisionPrincipal(principalInfo);
      Optional<Entity> matchingPrincipal = provisioningService
          .findPrincipalWithSameKind(principalInfo);
      if (matchingPrincipal.isEmpty()) {
        log.debug("Principal evaluation rejected: mode={}, principal={}, kind={}, linked=false",
            PrincipalProvisioningMode.JIT, principalInfo.identifier(), principalInfo.kind());
        errorResponseWriter.write(response, HttpStatus.UNAUTHORIZED,
            "No linked principal matches the authenticated identity");
        return false;
      }
      log.debug("Principal evaluation accepted: mode={}, principal={}, kind={}, linked=true",
          PrincipalProvisioningMode.JIT, principalInfo.identifier(), principalInfo.kind());
      request.setAttribute(ProvisionedPrincipalContext.REQUEST_ATTRIBUTE,
          new ProvisionedPrincipalContext(principalInfo, matchingPrincipal));

      log.debug("JIT provisioning successful for principal: {} (kind: {})",
          principalInfo.identifier(), principalInfo.kind());
      return true;
    } catch (Exception e) {
      log.warn("JIT provisioning failed for authenticated principal: {}", e.getMessage(), e);
      errorResponseWriter.write(response, HttpStatus.SERVICE_UNAVAILABLE,
          "Unable to verify your access at this time. Please try again later.");
      return false;
    }
  }

  /// Checks whether the supplied authentication represents an anonymous user.
  ///
  /// @param authentication Spring Security authentication to inspect
  /// @return `true` when the authentication is anonymous
  private boolean isAnonymous(Authentication authentication) {
    return authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
  }

  /// Excludes configured public paths from principal provisioning.
  ///
  /// @param request current HTTP request
  /// @return `true` when the request path matches a configured exclusion
  /// pattern
  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    return authProperties.jitProvisioningExcludedPaths().stream()
        .anyMatch(pattern -> PATTERN_PARSER.parse(pattern).matches(PathContainer.parsePath(path)));
  }
}
