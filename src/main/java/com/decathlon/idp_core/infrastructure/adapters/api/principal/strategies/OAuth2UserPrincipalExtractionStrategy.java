package com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies;

import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.EMAIL;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.GROUPS;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.SUB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractionStrategy;

/// Strategy for extracting principal information from OAuth2 and OpenID Connect (OIDC) users.
///
/// **Business purpose:** Handles extraction for the Authorization Code flow where the
/// authenticated principal is represented as an OAuth2User or OidcUser object.
@Component
public class OAuth2UserPrincipalExtractionStrategy implements PrincipalExtractionStrategy {

  private final AuthenticationProperties authProperties;

  public OAuth2UserPrincipalExtractionStrategy(AuthenticationProperties authProperties) {
    this.authProperties = authProperties;
  }

  @Override
  public boolean supports(Authentication authentication) {
    return authentication != null && authentication.getPrincipal() instanceof OAuth2User;
  }

  @Override
  public PrincipalInfo extract(Authentication authentication) {
    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
    return extractFromOAuth2User(oauth2User);
  }

  /// Extracts principal information from an OAuth2User, handling both standard
  /// OAuth2 and OIDC users.
  /// If the user is an OIDC user, it delegates to `extractFromOidcUser` for
  /// richer claim extraction.
  ///
  /// **Business purpose:** Ensures that the principal information is accurately
  /// extracted from the OAuth2User or OidcUser claims, supporting both standard
  /// OAuth2 and
  /// OpenID Connect flows using configurable claim mappings.
  private PrincipalInfo extractFromOAuth2User(OAuth2User oauth2User) {
    // If the user is an OpenID Connect user, we can extract more specific claims
    if (oauth2User instanceof OidcUser oidcUser) {
      return extractFromOidcUser(oidcUser);
    }

    Map<String, String> claimMappings = authProperties.userClaimMappings();

    // Standard OAuth2 user extraction using dynamic claims
    String subClaim = claimMappings.getOrDefault(SUB, SUB);
    String sub = Optional.ofNullable(oauth2User.getAttribute(subClaim)).map(Object::toString)
        .orElse(oauth2User.getName());

    String nameClaim = claimMappings.getOrDefault(NAME, NAME);
    String name = Optional.ofNullable(oauth2User.getAttribute(nameClaim)).map(Object::toString)
        .orElse(sub);

    Map<String, String> attributes = new HashMap<>();
    String emailClaim = claimMappings.getOrDefault(EMAIL, EMAIL);
    Optional.ofNullable(oauth2User.getAttribute(emailClaim)).map(Object::toString)
        .ifPresent(email -> attributes.put(EMAIL, email));

    List<String> groups = extractGroupsFromAttributes(oauth2User.getAttributes(), claimMappings);

    return new PrincipalInfo(sub, PrincipalKind.HUMAN, name, attributes, groups);
  }

  /// Extracts principal information from an OpenID Connect (OIDC) user.
  /// OIDC users provide richer claims than standard OAuth2 users, including full
  /// name and email.
  ///
  /// **Business purpose:** This method ensures that the principal information
  /// is accurately extracted from OIDC claims, which are standardized across
  /// compliant identity providers.
  private PrincipalInfo extractFromOidcUser(OidcUser oidcUser) {
    String sub = oidcUser.getSubject();
    String identifier = Optional.ofNullable(oidcUser.getPreferredUsername()).orElse(sub);

    String name = Optional.ofNullable(oidcUser.getFullName())
        .or(() -> Optional.ofNullable(oidcUser.getGivenName())).orElse(identifier);

    Map<String, String> attributes = new HashMap<>();
    Optional.ofNullable(oidcUser.getEmail()).ifPresent(email -> attributes.put(EMAIL, email));

    List<String> groups = extractGroupsFromAttributes(oidcUser.getAttributes(),
        authProperties.userClaimMappings());

    return new PrincipalInfo(identifier, PrincipalKind.HUMAN, name, attributes, groups);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractGroupsFromAttributes(Map<String, Object> attributes,
      Map<String, String> claimMappings) {
    String groupsClaim = claimMappings.getOrDefault(GROUPS, GROUPS);
    Object groupsAttr = attributes.get(groupsClaim);

    if (groupsAttr instanceof List<?> list) {
      return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
    return List.of();
  }
}
