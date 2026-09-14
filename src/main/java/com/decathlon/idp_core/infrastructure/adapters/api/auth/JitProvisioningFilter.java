package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPatternParser;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.service.principal.PrincipalProvisioningService;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SecurityConfiguration;
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
/// Failures are logged but don't block the request (fail-open for availability).
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

  private final PrincipalExtractor principalExtractor;
  private final PrincipalProvisioningService provisioningService;
  private final AuthenticationProperties authProperties;
  private final PathPatternParser patternParser = new PathPatternParser();

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.isAuthenticated()
        && !isAnonymous(authentication)) {
      provisionPrincipalSafely(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private void provisionPrincipalSafely(Authentication authentication) {
    try {
      PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);
      provisioningService.provisionPrincipal(principalInfo);

      log.debug("JIT provisioning successful for principal: {} (kind: {})",
          principalInfo.identifier(), principalInfo.kind());
    } catch (Exception e) {
      // Log error but don't block the request - fail-open for availability
      // The principal may not have a catalog entry, but can still authenticate
      log.warn("JIT provisioning failed for authenticated principal: {}", e.getMessage(), e);
    }
  }

  private boolean isAnonymous(Authentication authentication) {
    return authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    return authProperties.jitProvisioningExcludedPaths().stream()
        .anyMatch(pattern -> patternParser.parse(pattern).matches(PathContainer.parsePath(path)));
  }
}
