package com.decathlon.idp_core.infrastructure.adapters.api.principal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;

/// Domain service responsible for extracting principal information from Spring Security authentication context.
///
/// **Business purpose:** Transforms technical authentication tokens into domain-level PrincipalInfo models.
/// Supports multiple authentication mechanisms:
/// - JWT tokens (Resource Server authentication)
/// - OAuth2/OIDC user objects (Authorization Server flow)
/// - Service account tokens (M2M, API keys)
///
/// **Design rationale:** Isolates authentication-specific logic from core business operations.
/// Enables testability by providing a clear contract for principal extraction.
@Service
public class PrincipalExtractor {

  private static final String CLAIM_SUB = "sub";
  private static final String CLAIM_PREFERRED_USERNAME = "preferred_username";
  private static final String CLAIM_NAME = "name";
  private static final String CLAIM_EMAIL = "email";
  private static final String CLAIM_GROUPS = "groups";
  private static final String CLAIM_CLIENT_ID = "client_id";
  private static final String CLAIM_ORIGIN = "origin";
  private static final String CLAIM_AZP = "azp";
  private static final String CLAIM_SERVICE_NAME = "service_name";
  private static final String CLAIM_GRANT_TYPE = "grant_type";
  private static final String CLAIM_GTY = "gty";
  private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

  /// Extracts principal information from the current authentication context.
  ///
  /// **Contract:** Returns a PrincipalInfo containing:
  /// - Unique identifier (sub for humans, client_id for services)
  /// - Principal kind (HUMAN or SERVICE_ACCOUNT)
  /// - Display name
  /// - Relevant attributes (email, client_id, etc.)
  /// - Group memberships
  ///
  /// @param authentication the Spring Security authentication object
  /// @return extracted principal information
  public PrincipalInfo extractPrincipalInfo(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtToken) {
      return extractFromJwt(jwtToken);
    }

    if (authentication.getPrincipal()instanceof OAuth2User oauth2User) {
      return extractFromOAuth2User(oauth2User);
    }

    // Fallback for basic auth or other mechanisms
    return createFallbackPrincipal(authentication);
  }

  private PrincipalInfo extractFromJwt(JwtAuthenticationToken jwtToken) {
    var claims = jwtToken.getToken().getClaims();
    String sub = jwtToken.getToken().getSubject();

    // Detect if this is a service account token
    // 1. grant_type = client_credentials (definitive proof of M2M flow)
    // 2. Explicit service_name claim
    // 3. sub equals client_id (M2M flow where subject is the OAuth2 client itself)
    // Humans: sub is a user identifier, even if client_id is present (identifying
    // the OAuth2 client app)
    String grantType = Optional.ofNullable(claims.get(CLAIM_GRANT_TYPE))
        .or(() -> Optional.ofNullable(claims.get(CLAIM_GTY))).map(Object::toString).orElse(null);

    String clientId = Optional.ofNullable(claims.get(CLAIM_CLIENT_ID))
        .or(() -> Optional.ofNullable(claims.get(CLAIM_AZP))).map(Object::toString).orElse(null);

    boolean isServiceAccount = GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType)
        || claims.containsKey(CLAIM_SERVICE_NAME) || (clientId != null && sub.equals(clientId));

    if (isServiceAccount) {
      return extractServiceAccountFromJwt(sub, claims);
    }

    return extractHumanFromJwt(sub, claims);
  }

  private PrincipalInfo extractHumanFromJwt(String sub, Map<String, Object> claims) {
    String identifier = Optional.ofNullable(claims.get(CLAIM_PREFERRED_USERNAME))
        .map(Object::toString).orElse(sub);

    String name = Optional.ofNullable(claims.get(CLAIM_NAME)).map(Object::toString)
        .orElse(identifier);

    Map<String, String> attributes = new HashMap<>();
    Optional.ofNullable(claims.get(CLAIM_EMAIL)).map(Object::toString)
        .ifPresent(email -> attributes.put(CLAIM_EMAIL, email));

    List<String> groups = extractGroups(claims);

    return new PrincipalInfo(identifier, PrincipalKind.HUMAN, name, attributes, groups);
  }

  private PrincipalInfo extractServiceAccountFromJwt(String sub, Map<String, Object> claims) {
    String clientId = Optional.ofNullable(claims.get(CLAIM_CLIENT_ID))
        .or(() -> Optional.ofNullable(claims.get(CLAIM_AZP))).map(Object::toString).orElse(sub);

    String serviceName = Optional.ofNullable(claims.get(CLAIM_SERVICE_NAME))
        .or(() -> Optional.ofNullable(claims.get(CLAIM_NAME))).map(Object::toString)
        .orElse(clientId);

    Map<String, String> attributes = new HashMap<>();
    attributes.put(CLAIM_CLIENT_ID, clientId);
    Optional.ofNullable(claims.get(CLAIM_ORIGIN)).map(Object::toString)
        .ifPresent(origin -> attributes.put(CLAIM_ORIGIN, origin));

    List<String> groups = extractGroups(claims);

    return new PrincipalInfo(clientId, PrincipalKind.SERVICE_ACCOUNT, serviceName, attributes,
        groups);
  }

  private PrincipalInfo extractFromOAuth2User(OAuth2User oauth2User) {
    if (oauth2User instanceof OidcUser oidcUser) {
      return extractFromOidcUser(oidcUser);
    }

    // Standard OAuth2 user
    String sub = Optional.ofNullable(oauth2User.getAttribute(CLAIM_SUB)).map(Object::toString)
        .orElse(oauth2User.getName());

    String name = Optional.ofNullable(oauth2User.getAttribute(CLAIM_NAME)).map(Object::toString)
        .orElse(sub);

    Map<String, String> attributes = new HashMap<>();
    Optional.ofNullable(oauth2User.getAttribute(CLAIM_EMAIL)).map(Object::toString)
        .ifPresent(email -> attributes.put(CLAIM_EMAIL, email));

    List<String> groups = extractGroupsFromAttributes(oauth2User.getAttributes());

    return new PrincipalInfo(sub, PrincipalKind.HUMAN, name, attributes, groups);
  }

  private PrincipalInfo extractFromOidcUser(OidcUser oidcUser) {
    String sub = oidcUser.getSubject();
    String identifier = Optional.ofNullable(oidcUser.getPreferredUsername()).orElse(sub);
    String name = Optional.ofNullable(oidcUser.getFullName())
        .or(() -> Optional.ofNullable(oidcUser.getGivenName())).orElse(identifier);

    Map<String, String> attributes = new HashMap<>();
    Optional.ofNullable(oidcUser.getEmail()).ifPresent(email -> attributes.put(CLAIM_EMAIL, email));

    List<String> groups = extractGroupsFromAttributes(oidcUser.getAttributes());

    return new PrincipalInfo(identifier, PrincipalKind.HUMAN, name, attributes, groups);
  }

  private PrincipalInfo createFallbackPrincipal(Authentication authentication) {
    String name = authentication.getName();
    return new PrincipalInfo(name, PrincipalKind.HUMAN, name, Map.of(), List.of());
  }

  @SuppressWarnings("unchecked")
  private List<String> extractGroups(Map<String, Object> claims) {
    Object groupsClaim = claims.get(CLAIM_GROUPS);
    if (groupsClaim instanceof List<?> list) {
      return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
    return List.of();
  }

  @SuppressWarnings("unchecked")
  private List<String> extractGroupsFromAttributes(Map<String, Object> attributes) {
    Object groupsAttr = attributes.get(CLAIM_GROUPS);
    if (groupsAttr instanceof List<?> list) {
      return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
    return List.of();
  }
}
