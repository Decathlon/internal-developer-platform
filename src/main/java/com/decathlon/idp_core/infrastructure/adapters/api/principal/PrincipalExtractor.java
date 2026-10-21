package com.decathlon.idp_core.infrastructure.adapters.api.principal;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;

import lombok.RequiredArgsConstructor;

/// Infrastructure service responsible for extracting principal information from Spring Security authentication context.
///
/// **Business purpose:** Transforms technical authentication tokens into domain-level PrincipalInfo models.
/// Supports multiple authentication mechanisms:
/// - JWT tokens (Resource Server authentication)
/// - OAuth2/OIDC user objects (Authorization Server flow)
/// - Service account tokens (M2M, API keys)
///
/// **Design rationale:** Isolates authentication-specific logic from core business operations.
/// Enables testability by providing a clear contract for principal extraction.
///
/// **IdP Coupling & Claim Mapping:**
/// This extractor is tightly coupled to Identity Provider (IdP) behavior because different IdPs
/// use different JWT claim names and structures. For example:
/// - Auth0 uses `preferred_username` for human username, `client_id` for M2M clients
/// - Keycloak uses `preferred_username` for humans, `clientId` for clients
/// - Azure AD uses `unique_name` for humans, different M2M indicators
/// - Custom IdPs may use entirely custom claim names
///
/// **Solution:** The `AuthenticationProperties` configuration class allows mapping between
/// IdP-specific claim names and this extractor's internal logic. Configure `app.security.authentication.user-claim-mappings`
/// in `application.yml` to match your IdP's token structure.
///
/// JWT kind and identifiers use the provider-specific
/// `idp.auth.principal-extraction` configuration. OAuth2/OIDC user extraction
/// continues to use `AuthenticationProperties`.
///
/// @see AuthenticationProperties
@Service
@RequiredArgsConstructor
public class PrincipalExtractor {

  // Spring automatically injects all beans implementing
  // PrincipalExtractionStrategy!
  private final List<PrincipalExtractionStrategy> strategies;

  public PrincipalInfo extractPrincipalInfo(Authentication authentication) {
    return strategies.stream().filter(strategy -> strategy.supports(authentication)).findFirst()
        .map(strategy -> strategy.extract(authentication))
        .orElseGet(() -> createFallbackPrincipal(authentication));
  }

  private PrincipalInfo createFallbackPrincipal(Authentication authentication) {
    String name = authentication != null ? authentication.getName() : "UNKNOWN";
    return new PrincipalInfo(name, PrincipalKind.HUMAN, name, Map.of(), List.of());
  }
}
