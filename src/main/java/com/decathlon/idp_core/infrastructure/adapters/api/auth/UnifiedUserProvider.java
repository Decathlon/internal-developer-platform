package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

import lombok.RequiredArgsConstructor;

/// UnifiedUserProvider is a Spring component that implements the UserIdentityProvider interface to provide a consistent way
/// to retrieve the authenticated user's identity across different authentication mechanisms (JWT, OAuth2, OpenID).
/// The shared principal extractor keeps audit identities aligned with the identifiers used for provisioning
/// and authorization.
@Component
@RequiredArgsConstructor
public class UnifiedUserProvider implements UserIdentityProvider {

  private final PrincipalExtractor principalExtractor;

  @Override
  public String getAuthId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return "UNKNOWN";
    }

    return principalExtractor.extractPrincipalInfo(authentication).identifier();
  }

  @Override
  public String getName() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // Guard against unauthenticated/null context to prevent NullPointerExceptions
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return "UNKNOWN";
    }

    return authentication.getName();
  }
}
