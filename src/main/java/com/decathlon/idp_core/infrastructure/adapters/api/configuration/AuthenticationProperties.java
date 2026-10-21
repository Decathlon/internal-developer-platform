package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

/**
 * Type-safe authentication configuration properties bound from
 * {@code app.security.authentication}.
 *
 * <h2>Purpose</h2> Externalizes all hardcoded authentication logic to
 * configuration, enabling:
 * <ul>
 * <li>Support for multiple Identity Providers (Auth0, Keycloak, Azure AD,
 * etc.)</li>
 * <li>Flexible authentication mechanism configuration</li>
 * <li>Conditional authentication mechanism selection per environment</li>
 * <li>Clear documentation of IdP-specific token claim expectations</li>
 * </ul>
 *
 * <h2>Design rationale</h2> Configuration is externalized to
 * {@code application.yml} for easy environment customization.
 *
 * <h2>Principal Identifier</h2> The configured claim supplies the stable
 * identifier for OAuth2/OIDC human principals. JWT resource-server identifiers
 * and classification are configured by {@link PrincipalExtractionProperties}.
 * <p>
 *
 * @see SecurityConfiguration
 * @see PrincipalExtractor
 */
@ConfigurationProperties(prefix = "app.security.authentication")
public record AuthenticationProperties(
    // Maps optional standard claims to the names used by the configured identity
    // provider.
    Map<String, String> userClaimMappings,

    // Public paths excluded from JIT provisioning.
    List<String> jitProvisioningExcludedPaths,

    // Claim containing the stable identifier for a human principal; defaults to
    // "sub".
    String principalIdentifierClaim) {

  public AuthenticationProperties {
    principalIdentifierClaim = principalIdentifierClaim == null
        || principalIdentifierClaim.isBlank() ? "sub" : principalIdentifierClaim.trim();
  }

}
