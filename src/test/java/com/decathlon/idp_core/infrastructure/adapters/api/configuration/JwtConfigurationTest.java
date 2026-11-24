package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("JwtConfigurationTest")
class JwtConfigurationTest {

  private static final String JWKS = "https://idp.example.com/jwks.json";
  private static final String ISSUER = "https://idp.example.com/";

  @Test
  void shouldAcceptAnyIssuerAndAudienceWhenNothingIsConfigured() {
    var validator = new JwtConfiguration(JWKS, "", "").jwtValidator();

    assertThat(
        validator.validate(jwt("https://other.example.com/", List.of("other-app"))).hasErrors())
            .isFalse();
  }

  @Test
  void shouldRejectExpiredTokenEvenWhenNothingIsConfigured() {
    var validator = new JwtConfiguration(JWKS, null, null).jwtValidator();
    Jwt expired = Jwt.withTokenValue("t").header("alg", "RS256").issuer(ISSUER)
        .issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(3600))
        .build();

    assertThat(validator.validate(expired).hasErrors()).isTrue();
  }

  @Test
  void shouldRejectTokenFromAnotherIssuerWhenIssuerIsConfigured() {
    var validator = new JwtConfiguration(JWKS, ISSUER, "").jwtValidator();

    assertThat(validator.validate(jwt(ISSUER, List.of("any"))).hasErrors()).isFalse();
    assertThat(validator.validate(jwt("https://other.example.com/", List.of("any"))).hasErrors())
        .isTrue();
  }

  @Test
  void shouldRejectTokenForAnotherAudienceWhenAudiencesAreConfigured() {
    var validator = new JwtConfiguration(JWKS, "", " idp-core , idp-core-cli ").jwtValidator();

    assertThat(validator.validate(jwt(ISSUER, List.of("idp-core-cli"))).hasErrors()).isFalse();
    assertThat(validator.validate(jwt(ISSUER, List.of("x", "idp-core"))).hasErrors()).isFalse();
    assertThat(validator.validate(jwt(ISSUER, List.of("other-app"))).hasErrors()).isTrue();
  }

  @Test
  void shouldRejectTokenWithoutAudienceWhenAudiencesAreConfigured() {
    var validator = new JwtConfiguration(JWKS, "", "idp-core").jwtValidator();

    OAuth2TokenValidatorResult result = validator.validate(jwt(ISSUER, null));

    assertThat(result.hasErrors()).isTrue();
  }

  private static Jwt jwt(String issuer, List<String> audience) {
    Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "RS256").issuer(issuer)
        .issuedAt(Instant.now().minusSeconds(60)).expiresAt(Instant.now().plusSeconds(3600));
    if (audience != null) {
      builder.audience(audience);
    }
    return builder.build();
  }
}
