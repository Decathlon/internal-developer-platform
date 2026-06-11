package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/// UnifiedUserProvider is a Spring component that implements the UserIdentityProvider interface to provide a consistent way
/// to retrieve the authenticated user's identity across different authentication mechanisms (JWT, OAuth2, OpenID).
/// It checks the current security context for the authentication type and extracts the user ID accordingly:
/// - For JWT authentication, it retrieves the subject (sub) claim from the JWT token.
/// - For OAuth2 authentication, it first checks if the user is an OIDC user to
/// retrieve the subject, otherwise it looks for a "sub" or "id" attribute in the OAuth2 user attributes, falling back to the authentication name if neither is found.
/// - For basic authentication, it simply returns the authentication name.
@Component
public class UnifiedUserProvider implements UserIdentityProvider {

  @Override
  public String getAuthId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return "UNKNOWN";
    }

    // Jwt Case
    if (authentication instanceof JwtAuthenticationToken jwtToken) {
      return jwtToken.getToken().getSubject();
    }

    // OAuth2 and OpenId case
    if (authentication.getPrincipal()instanceof OAuth2User oauth2Token) {

      if (oauth2Token instanceof OidcUser oidcUser) {
        return oidcUser.getSubject();
      }

      return Optional.ofNullable(oauth2Token.getAttribute("sub")).map(Object::toString)
          .orElseGet(() -> Optional.ofNullable(oauth2Token.getAttribute("id")).map(Object::toString)
              .orElse(authentication.getName()));
    }

    // Basic Auth case
    return authentication.getName();
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
