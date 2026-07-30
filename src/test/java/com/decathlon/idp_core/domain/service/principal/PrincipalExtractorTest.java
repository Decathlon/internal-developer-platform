package com.decathlon.idp_core.domain.service.principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

/// Unit tests for PrincipalExtractor verifying correct extraction of principal
/// information from various authentication types.
class PrincipalExtractorTest {

  private PrincipalExtractor principalExtractor;

  @BeforeEach
  void setUp() {
    principalExtractor = new PrincipalExtractor();
  }

  @Test
  void shouldExtractHumanPrincipalFromJwtToken() {
    // Given: JWT token for human user with standard OIDC claims
    Map<String, Object> claims = Map.of("sub", "alice-sub-123", "preferred_username", "alice",
        "name", "Alice Dupont", "email", "alice.dupont@decathlon.com", "groups",
        List.of("platform-team", "admins"));

    Jwt jwt = createJwt(claims);
    Authentication authentication = new JwtAuthenticationToken(jwt);

    // When: Extract principal info
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then: Human principal correctly extracted
    assertThat(principalInfo.identifier()).isEqualTo("alice");
    assertThat(principalInfo.kind()).isEqualTo(PrincipalKind.HUMAN);
    assertThat(principalInfo.name()).isEqualTo("Alice Dupont");
    assertThat(principalInfo.attributes()).containsEntry("email", "alice.dupont@decathlon.com");
    assertThat(principalInfo.groups()).containsExactlyInAnyOrder("platform-team", "admins");
  }

  @Test
  void shouldExtractServiceAccountFromJwtToken() {
    // Given: JWT token for service account (M2M OAuth2)
    Map<String, Object> claims = Map.of("sub", "service-sub-456", "client_id", "github-connector",
        "service_name", "GitHub Actions Webhook", "origin", "github", "groups",
        List.of("devops-tools"));

    Jwt jwt = createJwt(claims);
    Authentication authentication = new JwtAuthenticationToken(jwt);

    // When: Extract principal info
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then: Service account correctly extracted
    assertThat(principalInfo.identifier()).isEqualTo("github-connector");
    assertThat(principalInfo.kind()).isEqualTo(PrincipalKind.SERVICE_ACCOUNT);
    assertThat(principalInfo.name()).isEqualTo("GitHub Actions Webhook");
    assertThat(principalInfo.attributes()).containsEntry("client_id", "github-connector")
        .containsEntry("origin", "github");
    assertThat(principalInfo.groups()).containsExactly("devops-tools");
  }

  @Test
  void shouldExtractHumanPrincipalFromJwtWithMinimalClaims() {
    // Given: JWT token with minimal claims (only sub)
    Map<String, Object> claims = Map.of("sub", "bob-sub-789", "email", "bob@example.com");

    Jwt jwt = createJwt(claims);
    Authentication authentication = new JwtAuthenticationToken(jwt);

    // When: Extract principal info
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then: Principal extracted with fallback values
    assertThat(principalInfo.identifier()).isEqualTo("bob-sub-789");
    assertThat(principalInfo.kind()).isEqualTo(PrincipalKind.HUMAN);
    assertThat(principalInfo.name()).isEqualTo("bob-sub-789"); // Falls back to identifier
    assertThat(principalInfo.attributes()).containsEntry("email", "bob@example.com");
    assertThat(principalInfo.groups()).isEmpty();
  }

  @Test
  void shouldExtractServiceAccountWithAzpClaim() {
    // Given: JWT token with azp (authorized party) claim instead of client_id
    Map<String, Object> claims = Map.of("sub", "service-sub-999", "azp", "kafka-connector", "name",
        "Kafka Event Producer");

    Jwt jwt = createJwt(claims);
    Authentication authentication = new JwtAuthenticationToken(jwt);

    // When: Extract principal info
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then: Service account extracted using azp as identifier
    assertThat(principalInfo.identifier()).isEqualTo("kafka-connector");
    assertThat(principalInfo.kind()).isEqualTo(PrincipalKind.SERVICE_ACCOUNT);
    assertThat(principalInfo.name()).isEqualTo("Kafka Event Producer");
    assertThat(principalInfo.attributes()).containsEntry("client_id", "kafka-connector");
  }

  @Test
  void shouldExtractFallbackPrincipalFromBasicAuth() {
    // Given: Basic authentication (no JWT)
    Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "password");

    // When: Extract principal info
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then: Fallback principal created
    assertThat(principalInfo.identifier()).isEqualTo("testuser");
    assertThat(principalInfo.kind()).isEqualTo(PrincipalKind.HUMAN);
    assertThat(principalInfo.name()).isEqualTo("testuser");
    assertThat(principalInfo.attributes()).isEmpty();
    assertThat(principalInfo.groups()).isEmpty();
  }

  @Test
  void shouldHandleEmptyGroupsClaim() {
    // Given: JWT token with empty groups claim
    Map<String, Object> claims = Map.of("sub", "charlie-sub", "preferred_username", "charlie",
        "name", "Charlie Brown", "email", "charlie@example.com", "groups", List.of());

    Jwt jwt = createJwt(claims);
    Authentication authentication = new JwtAuthenticationToken(jwt);

    // When: Extract principal info
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then: Groups list is empty
    assertThat(principalInfo.groups()).isEmpty();
  }

  @Test
  void shouldHandleMissingOptionalClaims() {
    // Given: JWT token without optional claims (email, groups, etc.)
    Map<String, Object> claims = Map.of("sub", "david-sub", "preferred_username", "david");

    Jwt jwt = createJwt(claims);
    Authentication authentication = new JwtAuthenticationToken(jwt);

    // When: Extract principal info
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then: Optional fields are empty/null
    assertThat(principalInfo.identifier()).isEqualTo("david-sub");
    assertThat(principalInfo.attributes()).doesNotContainKey("email");
    assertThat(principalInfo.groups()).isEmpty();
  }

  @Test
  void shouldExtractFromStandardOAuth2User() {
    // Given: A standard OAuth2 user authentication
    org.springframework.security.oauth2.core.user.OAuth2User oauth2User = mock(
        org.springframework.security.oauth2.core.user.OAuth2User.class);
    when(oauth2User.getName()).thenReturn("fallback-name");
    when(oauth2User.getAttribute("sub")).thenReturn("oauth2-sub-123");
    when(oauth2User.getAttribute("name")).thenReturn("OAuth2 User");
    when(oauth2User.getAttribute("email")).thenReturn("oauth2@example.com");
    when(oauth2User.getAttributes()).thenReturn(Map.of("sub", "oauth2-sub-123", "name",
        "OAuth2 User", "email", "oauth2@example.com", "groups", List.of("oauth-group-1")));

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(oauth2User);

    // When
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then
    assertThat(principalInfo.identifier()).isEqualTo("oauth2-sub-123");
    assertThat(principalInfo.kind()).isEqualTo(PrincipalKind.HUMAN);
    assertThat(principalInfo.name()).isEqualTo("OAuth2 User");
    assertThat(principalInfo.attributes()).containsEntry("email", "oauth2@example.com");
    assertThat(principalInfo.groups()).containsExactly("oauth-group-1");
  }

  @Test
  void shouldExtractFromOidcUser() {
    // Given: An OIDC user authentication
    org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser = mock(
        org.springframework.security.oauth2.core.oidc.user.OidcUser.class);
    when(oidcUser.getSubject()).thenReturn("oidc-sub-456");
    when(oidcUser.getPreferredUsername()).thenReturn("oidc-username");
    when(oidcUser.getFullName()).thenReturn("OIDC Full Name");
    when(oidcUser.getEmail()).thenReturn("oidc@example.com");
    when(oidcUser.getAttributes())
        .thenReturn(Map.of("groups", List.of("oidc-group-1", "oidc-group-2")));

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(oidcUser);

    // When
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then
    assertThat(principalInfo.identifier()).isEqualTo("oidc-username");
    assertThat(principalInfo.kind()).isEqualTo(PrincipalKind.HUMAN);
    assertThat(principalInfo.name()).isEqualTo("OIDC Full Name");
    assertThat(principalInfo.attributes()).containsEntry("email", "oidc@example.com");
    assertThat(principalInfo.groups()).containsExactlyInAnyOrder("oidc-group-1", "oidc-group-2");
  }

  @Test
  void shouldExtractFromOidcUserWithMinimalClaims() {
    // Given: An OIDC user with only a subject
    org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser = mock(
        org.springframework.security.oauth2.core.oidc.user.OidcUser.class);
    when(oidcUser.getSubject()).thenReturn("oidc-minimal-sub");
    when(oidcUser.getAttributes()).thenReturn(Map.of());

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(oidcUser);

    // When
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then: Fallbacks to subject for identifier and name
    assertThat(principalInfo.identifier()).isEqualTo("oidc-minimal-sub");
    assertThat(principalInfo.name()).isEqualTo("oidc-minimal-sub");
    assertThat(principalInfo.attributes()).isEmpty();
    assertThat(principalInfo.groups()).isEmpty();
  }

  @Test
  void shouldHandleMalformedGroupsClaimGracefully() {
    // Given: JWT token where 'groups' is a String instead of a List
    Map<String, Object> claims = Map.of("sub", "malformed-groups-sub", "email", "user@example.com",
        "preferred_username", "user", "groups", "this-should-be-a-list-but-is-a-string");

    Jwt jwt = createJwt(claims);
    Authentication authentication = new JwtAuthenticationToken(jwt);

    // When
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    // Then: Safe casting returns empty list instead of throwing ClassCastException
    assertThat(principalInfo.groups()).isEmpty();
  }

  private Jwt createJwt(Map<String, Object> claims) {
    return Jwt.withTokenValue("mock-token").header("alg", SignatureAlgorithm.RS256.getName())
        .claims(c -> c.putAll(claims)).build();
  }
}
