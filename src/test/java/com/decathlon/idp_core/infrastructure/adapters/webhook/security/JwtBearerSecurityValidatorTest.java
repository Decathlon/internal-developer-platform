package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException;

@DisplayName("JwtBearerSecurityValidator Tests")
@ExtendWith(MockitoExtension.class)
class JwtBearerSecurityValidatorTest {

  @Mock
  private WebhookJwtDecoderProvider jwtDecoderProvider;

  @Mock
  private JwtDecoder jwtDecoder;

  private JwtBearerSecurityValidator validator;

  @BeforeEach
  void setUp() {
    validator = new JwtBearerSecurityValidator(jwtDecoderProvider);
  }

  private JwtBearerSecurityValidator validatorWithPublicHostResolution() {
    return new JwtBearerSecurityValidator(jwtDecoderProvider) {
      @Override
      protected InetAddress[] resolveHostAddresses(String host) {
        try {
          return new InetAddress[]{InetAddress.getByAddress(host, new byte[]{8, 8, 8, 8})};
        } catch (UnknownHostException exception) {
          throw new AssertionError(exception);
        }
      }
    };
  }

  @Test
  @DisplayName("Should support JWT_BEARER only")
  void shouldSupportJwtBearer() {
    assertThat(validator.supports(WebhookSecurityType.JWT_BEARER)).isTrue();
    assertThat(validator.supports(WebhookSecurityType.STATIC_TOKEN)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.HMAC_SHA256)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.BASIC_AUTH)).isFalse();
  }

  @Nested
  @DisplayName("validateConfiguration — missing config keys")
  class MissingConfigKeys {

    @Test
    @DisplayName("Should throw when jwks_uri is missing from config")
    void shouldThrowWhenJwksUriMissing() {
      Map<String, String> config = Map.of("other_key", "value");

      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("jwks_uri");
    }

    @Test
    @DisplayName("Should throw when jwks_uri uses HTTP instead of HTTPS")
    void shouldThrowWhenJwksUriIsNotHttps() {
      Map<String, String> config = Map.of("jwks_uri", "http://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "expected@example.com");

      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("HTTPS");
    }

    @Test
    @DisplayName("Should reject jwks_uri when it is an environment reference")
    void shouldRejectJwksUriWhenItIsAnEnvironmentReference() {
      String envKey = "JWKS_URI";
      System.setProperty(envKey, "https://issuer/.well-known/jwks.json");

      try {
        Map<String, String> config = Map.of("jwks_uri", "${JWKS_URI}", "client_id_field", "email",
            "client_id_values", "expected@example.com");

        assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
            com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
            .hasMessageContaining("runtime environment references are not supported");
      } finally {
        System.clearProperty(envKey);
      }
    }

    @Test
    @DisplayName("Should throw when jwks_uri points to localhost")
    void shouldThrowWhenJwksUriPointsToLocalhost() {
      Map<String, String> config = Map.of("jwks_uri", "https://localhost/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "expected@example.com");

      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("localhost");
    }

    @Test
    @DisplayName("Should throw when jwks_uri points to a private IP address")
    void shouldThrowWhenJwksUriPointsToPrivateIpAddress() {
      Map<String, String> config = Map.of("jwks_uri", "https://192.168.1.10/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "expected@example.com");

      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("private and link-local hosts");
    }

    @Test
    @DisplayName("Should throw when a domain name resolves to a loopback address")
    void shouldThrowWhenJwksUriDomainResolvesToLoopbackAddress() {
      JwtBearerSecurityValidator loopingValidator = new JwtBearerSecurityValidator(
          jwtDecoderProvider) {
        @Override
        protected InetAddress[] resolveHostAddresses(String host) throws UnknownHostException {
          return new InetAddress[]{InetAddress.getByName("127.0.0.1")};
        }
      };

      Map<String, String> config = Map.of("jwks_uri", "https://jwks.internal.example/keys",
          "client_id_field", "email", "client_id_values", "expected@example.com");

      assertThatThrownBy(() -> loopingValidator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("localhost, loopback, private and link-local hosts");
    }

    @Test
    @DisplayName("Should throw when client_id_field is missing from config")
    void shouldThrowWhenClientIdFieldMissing() {
      JwtBearerSecurityValidator safeValidator = validatorWithPublicHostResolution();
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_values", "expected@example.com");

      assertThatThrownBy(() -> safeValidator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("client_id_field").hasMessageContaining("email")
          .hasMessageContaining("azp");
    }

    @Test
    @DisplayName("Should throw when client_id_values is missing from config")
    void shouldThrowWhenClientIdValuesMissing() {
      JwtBearerSecurityValidator safeValidator = validatorWithPublicHostResolution();
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email");

      assertThatThrownBy(() -> safeValidator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("client_id_values");
    }

    @Test
    @DisplayName("Should throw when client_id_values is an env reference")
    void shouldThrowWhenClientIdValuesIsEnvironmentReference() {
      JwtBearerSecurityValidator safeValidator = validatorWithPublicHostResolution();
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "env:JWT_ALLOWED_CLIENTS");

      assertThatThrownBy(() -> safeValidator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("runtime environment references are not supported");
    }

    @Test
    @DisplayName("Should throw when client_id_field is neither azp nor email")
    void shouldThrowWhenClientIdFieldIsUnsupported() {
      JwtBearerSecurityValidator safeValidator = validatorWithPublicHostResolution();
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "sub", "client_id_values", "expected@example.com");

      assertThatThrownBy(() -> safeValidator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("azp").hasMessageContaining("email");
    }

    @Test
    @DisplayName("Should accept missing audience because it is optional")
    void shouldAcceptMissingAudience() {
      JwtBearerSecurityValidator safeValidator = validatorWithPublicHostResolution();
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "expected@example.com");

      assertThatCode(() -> safeValidator.validateConfiguration(config)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw when expected_audience is an env reference")
    void shouldThrowWhenAudienceIsEnvironmentReference() {
      JwtBearerSecurityValidator safeValidator = validatorWithPublicHostResolution();
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "expected@example.com",
          "expected_audience", "env:JWT_AUDIENCE");

      assertThatThrownBy(() -> safeValidator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("Invalid expected_audience");
    }
  }

  @Nested
  @DisplayName("validateRequest — client identifier matching")
  class ValidateRequestClientIdentifier {

    @Test
    @DisplayName("Should accept Bearer token when configured claim value matches")
    void shouldAcceptValidClientIdentifierFromJwt() {
      when(jwtDecoderProvider.get("https://issuer/.well-known/jwks.json")).thenReturn(jwtDecoder);
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values",
          "ps-fb25-product-events-produ@cpe-idp-stg-337o.iam.gserviceaccount.com");
      String token = "signed-token";
      Map<String, Object> headers = Map.of("Authorization", "Bearer " + token);
      Jwt jwt = jwtWithClaim("email",
          "ps-fb25-product-events-produ@cpe-idp-stg-337o.iam.gserviceaccount.com");
      when(jwtDecoder.decode(token)).thenReturn(jwt);

      assertThatCode(() -> validator.validateRequest(headers, new byte[0], config))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should not revalidate jwks_uri on every request once configuration is valid")
    void shouldNotRevalidateJwksUriAtRuntime() {
      JwtBearerSecurityValidator runtimeValidator = new JwtBearerSecurityValidator(
          jwtDecoderProvider) {
        @Override
        protected InetAddress[] resolveHostAddresses(String host) {
          throw new AssertionError("JWKS host validation should not run during request validation");
        }
      };

      when(jwtDecoderProvider.get("https://issuer/.well-known/jwks.json")).thenReturn(jwtDecoder);
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values",
          "ps-fb25-product-events-produ@cpe-idp-stg-337o.iam.gserviceaccount.com");
      String token = "signed-token";
      Map<String, Object> headers = Map.of("Authorization", "Bearer " + token);
      Jwt jwt = jwtWithClaim("email",
          "ps-fb25-product-events-produ@cpe-idp-stg-337o.iam.gserviceaccount.com");
      when(jwtDecoder.decode(token)).thenReturn(jwt);

      assertThatCode(() -> runtimeValidator.validateRequest(headers, new byte[0], config))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject Bearer token when claim value does not match")
    void shouldRejectWhenClientIdentifierDoesNotMatch() {
      when(jwtDecoderProvider.get("https://issuer/.well-known/jwks.json")).thenReturn(jwtDecoder);
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values",
          "ps-fb25-product-events-produ@cpe-idp-stg-337o.iam.gserviceaccount.com");
      String token = "signed-token";
      Map<String, Object> headers = Map.of("Authorization", "Bearer " + token);
      Jwt jwt = jwtWithClaim("email", "another-service@project.iam.gserviceaccount.com");
      when(jwtDecoder.decode(token)).thenReturn(jwt);

      assertThatThrownBy(() -> validator.validateRequest(headers, new byte[0], config))
          .isInstanceOf(WebhookAuthForbiddenException.class);
    }

    @Test
    @DisplayName("Should reject malformed Bearer token")
    void shouldRejectMalformedToken() {
      when(jwtDecoderProvider.get("https://issuer/.well-known/jwks.json")).thenReturn(jwtDecoder);
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "expected@example.com");
      String token = "bad-token";
      Map<String, Object> headers = Map.of("Authorization", "Bearer " + token);
      when(jwtDecoder.decode(token)).thenThrow(new JwtException("invalid signature"));

      assertThatThrownBy(() -> validator.validateRequest(headers, new byte[0], config))
          .isInstanceOf(WebhookAuthUnauthorizedException.class);
    }

    @Test
    @DisplayName("Should validate using azp when client_id_field is configured")
    void shouldUseAzpClaimWhenConfigured() {
      when(jwtDecoderProvider.get("https://issuer/.well-known/jwks.json")).thenReturn(jwtDecoder);
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "azp", "client_id_values", "client-allowed");
      String token = "signed-token";
      Map<String, Object> headers = Map.of("Authorization", "Bearer " + token);
      Jwt jwt = jwtWithClaim("azp", "client-allowed");
      when(jwtDecoder.decode(token)).thenReturn(jwt);

      assertThatCode(() -> validator.validateRequest(headers, new byte[0], config))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject request when client_id_field is missing at runtime")
    void shouldRejectWhenClientIdFieldMissingAtRuntime() {
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_values", "client-allowed");
      String token = "signed-token";
      Map<String, Object> headers = Map.of("Authorization", "Bearer " + token);

      assertThatThrownBy(() -> validator.validateRequest(headers, new byte[0], config))
          .isInstanceOf(
              com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("client_id_field");
    }

    @Test
    @DisplayName("Should accept token when audience matches configured value")
    void shouldAcceptWhenAudienceMatches() {
      when(jwtDecoderProvider.get("https://issuer/.well-known/jwks.json")).thenReturn(jwtDecoder);
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "expected@example.com",
          "expected_audience", "https://idp-preprod.run.app/webhooks/dpac-component-events");
      String token = "signed-token";
      Map<String, Object> headers = Map.of("Authorization", "Bearer " + token);
      Jwt jwt = jwtWithClaims(Map.of("email", "expected@example.com", "aud",
          List.of("https://idp-preprod.run.app/webhooks/dpac-component-events")));
      when(jwtDecoder.decode(token)).thenReturn(jwt);

      assertThatCode(() -> validator.validateRequest(headers, new byte[0], config))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject token when audience does not match configured value")
    void shouldRejectWhenAudienceDoesNotMatch() {
      when(jwtDecoderProvider.get("https://issuer/.well-known/jwks.json")).thenReturn(jwtDecoder);
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "expected@example.com",
          "expected_audience", "https://idp-preprod.run.app/webhooks/dpac-component-events");
      String token = "signed-token";
      Map<String, Object> headers = Map.of("Authorization", "Bearer " + token);
      Jwt jwt = jwtWithClaims(
          Map.of("email", "expected@example.com", "aud", List.of("https://another-audience")));
      when(jwtDecoder.decode(token)).thenReturn(jwt);

      assertThatThrownBy(() -> validator.validateRequest(headers, new byte[0], config))
          .isInstanceOf(WebhookAuthForbiddenException.class)
          .hasMessageContaining("audience was rejected");
    }

    @Test
    @DisplayName("Should reject token when audience is configured but missing in JWT")
    void shouldRejectWhenAudienceMissingInJwt() {
      when(jwtDecoderProvider.get("https://issuer/.well-known/jwks.json")).thenReturn(jwtDecoder);
      Map<String, String> config = Map.of("jwks_uri", "https://issuer/.well-known/jwks.json",
          "client_id_field", "email", "client_id_values", "expected@example.com",
          "expected_audience", "https://idp-preprod.run.app/webhooks/dpac-component-events");
      String token = "signed-token";
      Map<String, Object> headers = Map.of("Authorization", "Bearer " + token);
      Jwt jwt = jwtWithClaim("email", "expected@example.com");
      when(jwtDecoder.decode(token)).thenReturn(jwt);

      assertThatThrownBy(() -> validator.validateRequest(headers, new byte[0], config))
          .isInstanceOf(WebhookAuthForbiddenException.class)
          .hasMessageContaining("missing required claim: aud");
    }

    private Jwt jwtWithClaim(String claimName, String claimValue) {
      return Jwt.withTokenValue("signed-token").header("alg", "RS256").claim(claimName, claimValue)
          .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
    }

    private Jwt jwtWithClaims(Map<String, Object> claims) {
      var builder = Jwt.withTokenValue("signed-token").header("alg", "RS256");
      claims.forEach(builder::claim);
      return builder.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
    }
  }
}
