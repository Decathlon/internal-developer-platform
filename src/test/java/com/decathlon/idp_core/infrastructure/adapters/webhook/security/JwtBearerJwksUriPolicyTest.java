package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;

@DisplayName("JwtBearerJwksUriPolicy Tests")
class JwtBearerJwksUriPolicyTest {

  @Test
  @DisplayName("accepts a public HTTPS JWKS URI")
  void validateConfiguredJwksUri_acceptsPublicHttpsUri() {
    assertThatCode(() -> JwtBearerJwksUriPolicy
        .validateConfiguredJwksUri("https://www.googleapis.com/oauth2/v3/certs"))
            .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("rejects HTTP JWKS URIs")
  void validateConfiguredJwksUri_rejectsHttpUri() {
    assertThatThrownBy(
        () -> JwtBearerJwksUriPolicy.validateConfiguredJwksUri("http://issuer.example.com/jwks"))
            .isInstanceOf(WebhookSecurityConfigurationException.class)
            .hasMessageContaining("HTTPS");
  }

  @Test
  @DisplayName("rejects localhost JWKS URIs at runtime fetch time")
  void validateRuntimeJwksUri_rejectsLocalhost() {
    URI jwksUri = URI.create("https://localhost/.well-known/jwks.json");

    assertThatThrownBy(() -> JwtBearerJwksUriPolicy.validateRuntimeJwksUri(jwksUri))
        .isInstanceOf(RestClientException.class).hasMessageContaining("localhost");
  }
}
