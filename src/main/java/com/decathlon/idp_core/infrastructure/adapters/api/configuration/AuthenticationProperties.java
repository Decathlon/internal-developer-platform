package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

/// Type-safe authentication configuration properties bound from
/// `app.security.authentication`.
///
/// # Purpose
/// Externalizes hardcoded authentication logic to configuration, enabling:
/// - Support for multiple identity providers (Auth0, Keycloak, Azure AD, etc.)
/// - Flexible authentication mechanism configuration
/// - Conditional authentication mechanism selection per environment
/// - Clear documentation of identity-provider-specific token claim expectations
///
/// # Design Rationale
/// Configuration is externalized to `application.yml` for easy environment
/// customization.
///
/// # Principal Identifier
/// OAuth2 users use the provider-configured principal name, while OIDC users use
/// the standard subject. JWT resource-server identifiers and classification are
/// configured by [PrincipalExtractionProperties].
///
/// @see SecurityConfiguration
/// @see PrincipalExtractor
@ConfigurationProperties(prefix = "app.security.authentication")
public record AuthenticationProperties(
    // Maps optional standard claims to the names used by the configured identity
    // provider.
    Map<String, String> userClaimMappings,

    // Public paths excluded from JIT provisioning.
    List<String> jitProvisioningExcludedPaths) {

}
