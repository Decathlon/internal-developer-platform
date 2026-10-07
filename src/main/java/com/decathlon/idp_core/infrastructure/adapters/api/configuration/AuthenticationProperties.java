package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

/**
 * Type-safe authentication and JWT configuration properties bound from
 * {@code app.security.authentication}.
 *
 * <h2>Purpose</h2> Externalizes all hardcoded authentication logic to
 * configuration, enabling:
 * <ul>
 * <li>Support for multiple Identity Providers (Auth0, Keycloak, Azure AD,
 * etc.)</li>
 * <li>Flexible service account detection without code changes</li>
 * <li>Conditional authentication mechanism selection per environment</li>
 * <li>Clear documentation of IdP-specific token claim expectations</li>
 * </ul>
 *
 * <h2>Design rationale</h2> Follows the same pattern as {@code CorsProperties}
 * and {@code SecurityRoleProperties}. Configuration is externalized to
 * {@code application.yml} for easy environment customization.
 *
 * <h2>Claim Mapping Strategy</h2> Different IdPs use different JWT claim names
 * for the same semantic meaning. This configuration allows mapping IdP-specific
 * claim names to standardized extraction logic.
 * <p>
 * Examples:
 * <ul>
 * <li>Auth0: {@code preferred_username} (username) vs. {@code sub} (unique
 * ID)</li>
 * <li>Azure AD: {@code unique_name} vs. {@code oid}</li>
 * <li>Keycloak: {@code preferred_username} vs. {@code sub}</li>
 * </ul>
 *
 * <h2>Service Account Detection</h2> Service accounts (M2M tokens) can be
 * identified in multiple ways depending on the IdP:
 * <ul>
 * <li><strong>Strict Mode (Recommended):</strong> Single definitive claim
 * (e.g., {@code token_type=m2m}, {@code client_credentials})</li>
 * <li><strong>Legacy Mode:</strong> Multiple fallback claims (for backwards
 * compatibility with existing deployments)</li>
 * </ul>
 *
 * @see SecurityConfiguration
 * @see PrincipalExtractor
 */
@ConfigurationProperties(prefix = "app.security.authentication")
public record AuthenticationProperties(
    // Maps standard claim names to the names used by the configured identity
    // provider.
    Map<String, String> userClaimMappings,

    // Controls strict or legacy detection of machine-to-machine tokens.
    ServiceAccountDetection serviceAccountDetection,

    // Public paths excluded from JIT provisioning.
    List<String> jitProvisioningExcludedPaths) {

  // Configuration used to identify service accounts.
  public record ServiceAccountDetection(
      // Enables or disables service account detection.
      boolean enabled,

      // "strict" checks one claim; "legacy" checks the configured fallbacks.
      String mode,

      // Claim checked in strict mode.
      String definitiveClaimName,

      // Value that identifies a service account in strict mode.
      String definitiveClaimValue,

      // Claims checked as fallbacks in legacy mode.
      List<String> legacyFallbackClaims) {
  }
}
