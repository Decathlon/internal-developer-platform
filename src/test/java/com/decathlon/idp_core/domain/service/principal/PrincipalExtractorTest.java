package com.decathlon.idp_core.domain.service.principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties.ServiceAccountDetection;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractionStrategy;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.JwtPrincipalExtractionStrategy;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.OAuth2UserPrincipalExtractionStrategy;

/**
 * Comprehensive unit tests for PrincipalExtractor and principal extraction
 * strategies.
 *
 * This test suite covers: - **PrincipalExtractor orchestration**: Strategy
 * selection, ordering, and fallback behavior -
 * **JwtPrincipalExtractionStrategy**: JWT token extraction for both human users
 * and service accounts - **OAuth2UserPrincipalExtractionStrategy**: OAuth2 and
 * OIDC user extraction
 *
 * All strategies are tested with various configurations and edge cases to
 * ensure full coverage.
 */
@ExtendWith(MockitoExtension.class)
class PrincipalExtractorTest {

  private PrincipalExtractor principalExtractor;

  @Nested
  @DisplayName("PrincipalExtractor Orchestration Tests")
  class PrincipalExtractorOrchestrationTests {

    @Test
    @DisplayName("Should use applicable strategy when supported")
    void shouldUseApplicableStrategyWhenSupported() {
      // Given: A strategy that supports the authentication and returns a principal
      PrincipalInfo expectedPrincipal = new PrincipalInfo("test-id", PrincipalKind.HUMAN,
          "Test User", Map.of(), List.of());

      PrincipalExtractionStrategy supportingStrategy = mock(PrincipalExtractionStrategy.class);
      when(supportingStrategy.supports(any())).thenReturn(true);
      when(supportingStrategy.extract(any())).thenReturn(expectedPrincipal);

      principalExtractor = new PrincipalExtractor(List.of(supportingStrategy));
      Authentication authentication = mock(Authentication.class);

      // When: Extract principal info
      PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

      // Then: The strategy was used to extract the principal
      assertThat(principalInfo).isEqualTo(expectedPrincipal);
    }

    @Test
    @DisplayName("Should fallback when no strategy supports authentication")
    void shouldFallbackWhenNoStrategySupportsAuthentication() {
      // Given: No strategy supports the authentication type
      PrincipalExtractionStrategy unsupportingStrategy = mock(PrincipalExtractionStrategy.class);
      when(unsupportingStrategy.supports(any())).thenReturn(false);

      principalExtractor = new PrincipalExtractor(List.of(unsupportingStrategy));
      Authentication authentication = new UsernamePasswordAuthenticationToken("testuser",
          "password");

      // When: Extract principal info from unsupported authentication
      PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

      // Then: Fallback principal is created
      assertThat(principalInfo.identifier()).isEqualTo("testuser");
      assertThat(principalInfo.kind()).isEqualTo(PrincipalKind.HUMAN);
      assertThat(principalInfo.name()).isEqualTo("testuser");
      assertThat(principalInfo.attributes()).isEmpty();
      assertThat(principalInfo.groups()).isEmpty();
    }

    @Test
    @DisplayName("Should use first supporting strategy from multiple strategies")
    void shouldUseFirstSupportingStrategyFromList() {
      // Given: Multiple strategies, first one supports the authentication
      PrincipalInfo expectedPrincipal = new PrincipalInfo("first-strategy", PrincipalKind.HUMAN,
          "First Strategy", Map.of(), List.of());

      PrincipalExtractionStrategy firstStrategy = mock(PrincipalExtractionStrategy.class);
      when(firstStrategy.supports(any())).thenReturn(true);
      when(firstStrategy.extract(any())).thenReturn(expectedPrincipal);

      PrincipalExtractionStrategy secondStrategy = mock(PrincipalExtractionStrategy.class);

      principalExtractor = new PrincipalExtractor(List.of(firstStrategy, secondStrategy));
      Authentication authentication = mock(Authentication.class);

      // When: Extract principal info
      PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

      // Then: First strategy's principal is returned
      assertThat(principalInfo).isEqualTo(expectedPrincipal);
    }

    @Test
    @DisplayName("Should use second strategy when first doesn't support")
    void shouldUseSecondStrategyWhenFirstDoesntSupport() {
      // Given: Multiple strategies, first doesn't support but second does
      PrincipalInfo expectedPrincipal = new PrincipalInfo("second-strategy", PrincipalKind.HUMAN,
          "Second Strategy", Map.of(), List.of());

      PrincipalExtractionStrategy firstStrategy = mock(PrincipalExtractionStrategy.class);
      when(firstStrategy.supports(any())).thenReturn(false);

      PrincipalExtractionStrategy secondStrategy = mock(PrincipalExtractionStrategy.class);
      when(secondStrategy.supports(any())).thenReturn(true);
      when(secondStrategy.extract(any())).thenReturn(expectedPrincipal);

      principalExtractor = new PrincipalExtractor(List.of(firstStrategy, secondStrategy));
      Authentication authentication = mock(Authentication.class);

      // When: Extract principal info
      PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

      // Then: Second strategy's principal is returned
      assertThat(principalInfo).isEqualTo(expectedPrincipal);
    }

    @Test
    @DisplayName("Should fallback when null authentication")
    void shouldFallbackWhenAuthenticationIsNull() {
      // Given: Extractor with no strategies
      principalExtractor = new PrincipalExtractor(List.of());

      // When: Extract principal info with null authentication
      PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(null);

      // Then: Fallback principal with UNKNOWN is created
      assertThat(principalInfo.identifier()).isEqualTo("UNKNOWN");
      assertThat(principalInfo.name()).isEqualTo("UNKNOWN");
      assertThat(principalInfo.kind()).isEqualTo(PrincipalKind.HUMAN);
      assertThat(principalInfo.attributes()).isEmpty();
      assertThat(principalInfo.groups()).isEmpty();
    }

    @Test
    @DisplayName("Should fallback with empty strategies list")
    void shouldFallbackWithEmptyStrategiesList() {
      // Given: Extractor with empty strategy list
      principalExtractor = new PrincipalExtractor(List.of());
      Authentication authentication = new UsernamePasswordAuthenticationToken("user123",
          "password");

      // When: Extract principal info
      PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

      // Then: Fallback uses authentication name
      assertThat(principalInfo.identifier()).isEqualTo("user123");
      assertThat(principalInfo.name()).isEqualTo("user123");
    }
  }

  @Nested
  @DisplayName("JwtPrincipalExtractionStrategy Tests")
  class JwtPrincipalExtractionStrategyTests {

    private AuthenticationProperties authProperties;
    private JwtPrincipalExtractionStrategy jwtStrategy;

    @BeforeEach
    void setUp() {
      // Default configuration for tests
      Map<String, String> claimMappings = new HashMap<>();
      claimMappings.put("preferred_username", "preferred_username");
      claimMappings.put("name", "name");
      claimMappings.put("email", "email");
      claimMappings.put("groups", "groups");
      claimMappings.put("client_id", "client_id");
      claimMappings.put("azp", "azp");
      claimMappings.put("service_name", "service_name");
      claimMappings.put("grant_type", "grant_type");
      claimMappings.put("gty", "gty");

      authProperties = new AuthenticationProperties(claimMappings, new ServiceAccountDetection(true,
          "legacy", "token_type", "m2m", List.of("grant_type", "service_name")), List.of());
      jwtStrategy = new JwtPrincipalExtractionStrategy(authProperties);
    }

    @Test
    @DisplayName("Should support JwtAuthenticationToken")
    void shouldSupportJwtAuthenticationToken() {
      // Given: JwtAuthenticationToken
      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      var auth = new JwtAuthenticationToken(jwtToken);

      // When/Then: Strategy supports this authentication
      assertThat(jwtStrategy.supports(auth)).isTrue();
    }

    @Test
    @DisplayName("Should not support non-JWT authentication")
    void shouldNotSupportNonJwtAuthentication() {
      // Given: Non-JWT authentication
      Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass");

      // When/Then: Strategy does not support this authentication
      assertThat(jwtStrategy.supports(auth)).isFalse();
    }

    @Test
    @DisplayName("Should extract human principal from JWT with all claims")
    void shouldExtractHumanFromJwtWithAllClaims() {
      // Given: JWT with human user claims
      Map<String, Object> claims = new HashMap<>();
      claims.put("preferred_username", "john.doe");
      claims.put("name", "John Doe");
      claims.put("email", "john@example.com");
      claims.put("groups", List.of("admin", "users"));

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("user-123");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Principal is extracted correctly
      assertThat(principal.identifier()).isEqualTo("john.doe");
      assertThat(principal.name()).isEqualTo("John Doe");
      assertThat(principal.kind()).isEqualTo(PrincipalKind.HUMAN);
      assertThat(principal.attributes()).containsEntry("email", "john@example.com");
      assertThat(principal.groups()).containsExactly("admin", "users");
    }

    @Test
    @DisplayName("Should fall back to subject when preferred_username missing")
    void shouldFallbackToSubjectWhenPreferredUsernameMissing() {
      // Given: JWT without preferred_username
      Map<String, Object> claims = new HashMap<>();
      claims.put("name", "John Doe");
      claims.put("email", "john@example.com");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("user-123");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Falls back to subject
      assertThat(principal.identifier()).isEqualTo("user-123");
      assertThat(principal.name()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should fall back to identifier when name claim missing")
    void shouldFallbackToIdentifierWhenNameMissing() {
      // Given: JWT without name claim
      Map<String, Object> claims = new HashMap<>();
      claims.put("preferred_username", "john.doe");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("user-123");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Name falls back to identifier
      assertThat(principal.name()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("Should skip email attribute when not present")
    void shouldSkipEmailAttributeWhenNotPresent() {
      // Given: JWT without email claim
      Map<String, Object> claims = new HashMap<>();
      claims.put("preferred_username", "john.doe");
      claims.put("name", "John Doe");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("user-123");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Email not included in attributes
      assertThat(principal.attributes()).doesNotContainKey("email");
    }

    @Test
    @DisplayName("Should return empty groups when groups claim missing")
    void shouldReturnEmptyGroupsWhenMissing() {
      // Given: JWT without groups claim
      Map<String, Object> claims = new HashMap<>();
      claims.put("preferred_username", "john.doe");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("user-123");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Groups is empty list
      assertThat(principal.groups()).isEmpty();
    }

    @Test
    @DisplayName("Should filter non-string values from groups")
    void shouldFilterNonStringValuesFromGroups() {
      // Given: JWT with mixed-type groups
      Map<String, Object> claims = new HashMap<>();
      claims.put("preferred_username", "john.doe");
      claims.put("groups", java.util.Arrays.asList("admin", 123, "users", null));

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("user-123");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Only string groups are included
      assertThat(principal.groups()).containsExactly("admin", "users");
    }

    @Test
    @DisplayName("Should detect service account with legacy mode - grant_type client_credentials")
    void shouldDetectServiceAccountLegacyGrantType() {
      // Given: JWT with grant_type=client_credentials (legacy mode enabled)
      Map<String, Object> claims = new HashMap<>();
      claims.put("client_id", "my-service");
      claims.put("name", "My Service");
      claims.put("grant_type", "client_credentials");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Detected as service account
      assertThat(principal.kind()).isEqualTo(PrincipalKind.SERVICE_ACCOUNT);
      assertThat(principal.identifier()).isEqualTo("my-service");
      assertThat(principal.name()).isEqualTo("My Service");
    }

    @Test
    @DisplayName("Should detect service account with gty claim")
    void shouldDetectServiceAccountWithGtyClaim() {
      // Given: JWT with gty=client_credentials
      Map<String, Object> claims = new HashMap<>();
      claims.put("client_id", "my-service");
      claims.put("gty", "client_credentials");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Detected as service account
      assertThat(principal.kind()).isEqualTo(PrincipalKind.SERVICE_ACCOUNT);
    }

    @Test
    @DisplayName("Should detect service account with service_name claim")
    void shouldDetectServiceAccountWithServiceNameClaim() {
      // Given: JWT with service_name claim
      Map<String, Object> claims = new HashMap<>();
      claims.put("service_name", "my-service");
      claims.put("client_id", "my-service");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Detected as service account
      assertThat(principal.kind()).isEqualTo(PrincipalKind.SERVICE_ACCOUNT);
    }

    @Test
    @DisplayName("Should detect service account when sub equals client_id")
    void shouldDetectServiceAccountWhenSubEqualsClientId() {
      // Given: JWT where sub equals client_id
      Map<String, Object> claims = new HashMap<>();
      claims.put("client_id", "my-service");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Detected as service account
      assertThat(principal.kind()).isEqualTo(PrincipalKind.SERVICE_ACCOUNT);
    }

    @Test
    @DisplayName("Should extract service account with client_id and fallback to azp")
    void shouldExtractServiceAccountWithClientIdFallbackToAzp() {
      // Given: JWT with azp instead of client_id
      Map<String, Object> claims = new HashMap<>();
      claims.put("azp", "authorized-party");
      claims.put("grant_type", "client_credentials");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("authorized-party");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Uses azp as client_id
      assertThat(principal.kind()).isEqualTo(PrincipalKind.SERVICE_ACCOUNT);
      assertThat(principal.attributes()).containsEntry("client_id", "authorized-party");
    }

    @Test
    @DisplayName("Should extract service account with service_name fallback to name")
    void shouldExtractServiceAccountWithServiceNameFallbackToName() {
      // Given: JWT with name instead of service_name
      Map<String, Object> claims = new HashMap<>();
      claims.put("client_id", "my-service");
      claims.put("name", "My Service Name");
      claims.put("grant_type", "client_credentials");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Uses name for service account
      assertThat(principal.name()).isEqualTo("My Service Name");
    }

    @Test
    @DisplayName("Should include origin in service account attributes")
    void shouldIncludeOriginInServiceAccountAttributes() {
      // Given: JWT with origin claim for service account
      Map<String, Object> claims = new HashMap<>();
      claims.put("client_id", "my-service");
      claims.put("origin", "https://origin.example.com");
      claims.put("grant_type", "client_credentials");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Origin is included in attributes
      assertThat(principal.attributes()).containsEntry("origin", "https://origin.example.com");
    }

    @Test
    @DisplayName("Should extract service account groups")
    void shouldExtractServiceAccountGroups() {
      // Given: Service account JWT with groups
      Map<String, Object> claims = new HashMap<>();
      claims.put("client_id", "my-service");
      claims.put("grant_type", "client_credentials");
      claims.put("groups", List.of("service-admins", "integrations"));

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = jwtStrategy.extract(auth);

      // Then: Groups are extracted for service account
      assertThat(principal.groups()).containsExactly("service-admins", "integrations");
    }

    @Test
    @DisplayName("Should disable service account detection when disabled in config")
    void shouldDisableServiceAccountDetectionWhenDisabled() {
      // Given: Service account detection disabled
      AuthenticationProperties disabledConfig = new AuthenticationProperties(
          authProperties.claimMappings(),
          new ServiceAccountDetection(false, "legacy", "token_type", "m2m",
              List.of("grant_type", "service_name")),
          authProperties.jitProvisioningExcludedPaths());
      JwtPrincipalExtractionStrategy strategyDisabled = new JwtPrincipalExtractionStrategy(
          disabledConfig);

      Map<String, Object> claims = new HashMap<>();
      claims.put("client_id", "my-service");
      claims.put("grant_type", "client_credentials");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = strategyDisabled.extract(auth);

      // Then: Treated as human even with M2M indicators
      assertThat(principal.kind()).isEqualTo(PrincipalKind.HUMAN);
    }

    @Test
    @DisplayName("Should use strict mode for service account detection")
    void shouldUseStrictModeForServiceAccountDetection() {
      // Given: Strict mode configuration
      AuthenticationProperties strictConfig = new AuthenticationProperties(
          authProperties.claimMappings(),
          new ServiceAccountDetection(true, "strict", "token_type", "m2m",
              List.of("grant_type", "service_name")),
          authProperties.jitProvisioningExcludedPaths());
      JwtPrincipalExtractionStrategy strategyStrict = new JwtPrincipalExtractionStrategy(
          strictConfig);

      Map<String, Object> claims = new HashMap<>();
      claims.put("token_type", "m2m");
      claims.put("client_id", "my-service");

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = strategyStrict.extract(auth);

      // Then: Detected with strict mode
      assertThat(principal.kind()).isEqualTo(PrincipalKind.SERVICE_ACCOUNT);
    }

    @Test
    @DisplayName("Should not detect service account in strict mode with wrong claim value")
    void shouldNotDetectServiceAccountInStrictModeWithWrongValue() {
      // Given: Strict mode with wrong claim value
      AuthenticationProperties strictConfig = new AuthenticationProperties(
          authProperties.claimMappings(),
          new ServiceAccountDetection(true, "strict", "token_type", "m2m",
              List.of("grant_type", "service_name")),
          authProperties.jitProvisioningExcludedPaths());
      JwtPrincipalExtractionStrategy strategyStrict = new JwtPrincipalExtractionStrategy(
          strictConfig);

      Map<String, Object> claims = new HashMap<>();
      claims.put("token_type", "human"); // Wrong value
      claims.put("client_id", "my-service");
      claims.put("grant_type", "client_credentials"); // Would match in legacy mode

      var jwtToken = mock(org.springframework.security.oauth2.jwt.Jwt.class);
      when(jwtToken.getSubject()).thenReturn("my-service");
      when(jwtToken.getClaims()).thenReturn(claims);

      var auth = new JwtAuthenticationToken(jwtToken);

      // When: Extract principal
      PrincipalInfo principal = strategyStrict.extract(auth);

      // Then: Not detected as service account (strict mode doesn't check grant_type)
      assertThat(principal.kind()).isEqualTo(PrincipalKind.HUMAN);
    }
  }

  @Nested
  @DisplayName("OAuth2UserPrincipalExtractionStrategy Tests")
  class OAuth2UserPrincipalExtractionStrategyTests {

    private OAuth2UserPrincipalExtractionStrategy oauth2Strategy;

    @BeforeEach
    void setUp() {
      oauth2Strategy = new OAuth2UserPrincipalExtractionStrategy();
    }

    @Test
    @DisplayName("Should support OAuth2User authentication")
    void shouldSupportOAuth2User() {
      // Given: OAuth2User in authentication
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("sub", "user-123");
      OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attributes, "sub");

      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oauth2User);

      // When/Then: Strategy supports this authentication
      assertThat(oauth2Strategy.supports(auth)).isTrue();
    }

    @Test
    @DisplayName("Should support OidcUser authentication (subclass of OAuth2User)")
    void shouldSupportOidcUser() {
      // Given: OidcUser (OIDC implementation of OAuth2User)
      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims()).thenReturn(Map.of("sub", "user-123", "preferred_username",
          "john.doe", "name", "John Doe", "email", "john@example.com"));

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When/Then: Strategy supports OIDC users
      assertThat(oauth2Strategy.supports(auth)).isTrue();
    }

    @Test
    @DisplayName("Should not support non-OAuth2 authentication")
    void shouldNotSupportNonOAuth2Authentication() {
      // Given: Non-OAuth2 authentication
      Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass");

      // When/Then: Strategy does not support this
      assertThat(oauth2Strategy.supports(auth)).isFalse();
    }

    @Test
    @DisplayName("Should not support null authentication")
    void shouldNotSupportNullAuthentication() {
      // When/Then: Null authentication is not supported
      assertThat(oauth2Strategy.supports(null)).isFalse();
    }

    @Test
    @DisplayName("Should extract OAuth2 user with all attributes")
    void shouldExtractOAuth2UserWithAllAttributes() {
      // Given: OAuth2User with all attributes
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("sub", "user-123");
      attributes.put("name", "John Doe");
      attributes.put("email", "john@example.com");
      attributes.put("groups", List.of("admin", "users"));

      OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attributes, "sub");
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oauth2User);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: All attributes extracted correctly
      assertThat(principal.identifier()).isEqualTo("user-123");
      assertThat(principal.name()).isEqualTo("John Doe");
      assertThat(principal.kind()).isEqualTo(PrincipalKind.HUMAN);
      assertThat(principal.attributes()).containsEntry("email", "john@example.com");
      assertThat(principal.groups()).containsExactly("admin", "users");
    }

    @Test
    @DisplayName("Should fall back to name when sub not present in OAuth2User")
    void shouldFallbackToNameWhenSubNotPresent() {
      // Given: OAuth2User without sub claim
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("name", "John Doe");

      OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attributes, "name");
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oauth2User);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Falls back to name (which is the OAuth2User.getName())
      assertThat(principal.identifier()).isNotBlank();
    }

    @Test
    @DisplayName("Should fall back to identifier when name not present in OAuth2User")
    void shouldFallbackToIdentifierWhenNameNotPresent() {
      // Given: OAuth2User without name attribute
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("sub", "user-123");

      OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attributes, "sub");
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oauth2User);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Name falls back to identifier
      assertThat(principal.name()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("Should skip email when not present in OAuth2User")
    void shouldSkipEmailWhenNotPresent() {
      // Given: OAuth2User without email
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("sub", "user-123");
      attributes.put("name", "John Doe");

      OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attributes, "sub");
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oauth2User);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Email not in attributes
      assertThat(principal.attributes()).doesNotContainKey("email");
    }

    @Test
    @DisplayName("Should return empty groups when groups not present in OAuth2User")
    void shouldReturnEmptyGroupsWhenNotPresent() {
      // Given: OAuth2User without groups
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("sub", "user-123");

      OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attributes, "sub");
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oauth2User);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Groups is empty
      assertThat(principal.groups()).isEmpty();
    }

    @Test
    @DisplayName("Should filter non-string values from OAuth2 groups")
    void shouldFilterNonStringValuesFromOAuth2Groups() {
      // Given: OAuth2User with mixed-type groups
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("sub", "user-123");
      attributes.put("groups", List.of("admin", 123, "users"));

      OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attributes, "sub");
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oauth2User);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Only string groups included
      assertThat(principal.groups()).containsExactly("admin", "users");
    }

    @Test
    @DisplayName("Should extract OIDC user with preferred username")
    void shouldExtractOidcUserWithPreferredUsername() {
      // Given: OIDC user with preferred_username
      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims()).thenReturn(Map.of("sub", "user-123", "preferred_username",
          "john.doe", "name", "John Doe", "email", "john@example.com"));

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Uses preferred_username as identifier
      assertThat(principal.identifier()).isEqualTo("john.doe");
      assertThat(principal.name()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should fall back to subject when preferred username not present in OIDC")
    void shouldFallbackToSubjectWhenPreferredUsernameNotPresent() {
      // Given: OIDC user without preferred_username
      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims()).thenReturn(Map.of("sub", "user-123", "name", "John Doe"));

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Falls back to subject
      assertThat(principal.identifier()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("Should use full name from OIDC user")
    void shouldUseFullNameFromOidcUser() {
      // Given: OIDC user with full name
      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims()).thenReturn(
          Map.of("sub", "user-123", "preferred_username", "john.doe", "name", "John Michael Doe"));

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Uses full name
      assertThat(principal.name()).isEqualTo("John Michael Doe");
    }

    @Test
    @DisplayName("Should fall back to given name when full name not present in OIDC")
    void shouldFallbackToGivenNameWhenFullNameNotPresent() {
      // Given: OIDC user with only given name
      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims()).thenReturn(
          Map.of("sub", "user-123", "preferred_username", "john.doe", "given_name", "John"));

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Uses given name
      assertThat(principal.name()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should fall back to identifier when neither full nor given name present")
    void shouldFallbackToIdentifierWhenNoNamePresent() {
      // Given: OIDC user without full name or given name
      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims())
          .thenReturn(Map.of("sub", "user-123", "preferred_username", "john.doe"));

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Falls back to identifier
      assertThat(principal.name()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("Should extract email from OIDC user")
    void shouldExtractEmailFromOidcUser() {
      // Given: OIDC user with email
      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims()).thenReturn(
          Map.of("sub", "user-123", "preferred_username", "john.doe", "email", "john@example.com"));

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Email included
      assertThat(principal.attributes()).containsEntry("email", "john@example.com");
    }

    @Test
    @DisplayName("Should extract groups from OIDC user claims")
    void shouldExtractGroupsFromOidcUserClaims() {
      // Given: OIDC user with groups in claims
      Map<String, Object> claims = new HashMap<>();
      claims.put("sub", "user-123");
      claims.put("groups", List.of("admin", "developers"));

      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims()).thenReturn(claims);

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Groups extracted from claims
      assertThat(principal.groups()).containsExactly("admin", "developers");
    }

    @Test
    @DisplayName("Should handle OIDC user with minimal attributes")
    void shouldHandleOidcUserWithMinimalAttributes() {
      // Given: OIDC user with only subject
      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims()).thenReturn(Map.of("sub", "user-123"));

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Still creates valid principal
      assertThat(principal.identifier()).isEqualTo("user-123");
      assertThat(principal.name()).isEqualTo("user-123");
      assertThat(principal.kind()).isEqualTo(PrincipalKind.HUMAN);
      assertThat(principal.attributes()).isEmpty();
      assertThat(principal.groups()).isEmpty();
    }

    @Test
    @DisplayName("Should not include email in OIDC attributes when null")
    void shouldNotIncludeNullEmailFromOidcUser() {
      // Given: OIDC user without email
      var idToken = mock(OidcIdToken.class);
      when(idToken.getClaims())
          .thenReturn(Map.of("sub", "user-123", "preferred_username", "john.doe"));

      var oidcUser = new DefaultOidcUser(List.of(), idToken);
      Authentication auth = mock(Authentication.class);
      when(auth.getPrincipal()).thenReturn(oidcUser);

      // When: Extract principal
      PrincipalInfo principal = oauth2Strategy.extract(auth);

      // Then: Email not in attributes
      assertThat(principal.attributes()).doesNotContainKey("email");
    }
  }
}
