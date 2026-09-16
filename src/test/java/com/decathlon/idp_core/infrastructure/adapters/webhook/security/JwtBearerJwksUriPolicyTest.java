package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
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
            .hasMessageContaining("only_https");
  }

  @Test
  @DisplayName("rejects localhost JWKS URIs at runtime fetch time")
  void validateRuntimeJwksUri_rejectsLocalhost() {
    URI jwksUri = URI.create("https://localhost/.well-known/jwks.json");

    assertThatThrownBy(() -> JwtBearerJwksUriPolicy.validateRuntimeJwksUri(jwksUri))
        .isInstanceOf(RestClientException.class).hasMessageContaining("unsafe_host");
  }

  @Test
  @DisplayName("detects private and loopback addresses for JWKS transport DNS resolution")
  void containsPrivateOrLoopbackAddress_detectsUnsafeAddresses() throws Exception {
    InetAddress publicAddress = InetAddress.getByAddress("public", new byte[]{8, 8, 8, 8});
    InetAddress privateAddress = InetAddress.getByAddress("private",
        new byte[]{(byte) 10, 0, 0, 1});

    assertThat(
        JwtBearerJwksUriPolicy.containsPrivateOrLoopbackAddress(new InetAddress[]{publicAddress}))
            .isFalse();
    assertThat(JwtBearerJwksUriPolicy
        .containsPrivateOrLoopbackAddress(new InetAddress[]{publicAddress, privateAddress}))
            .isTrue();
  }

  @Test
  @DisplayName("rejects carrier-grade NAT range (100.64.0.0/10)")
  void isPrivateOrLoopbackAddress_rejectsCgnRange() throws Exception {
    // 100.75.0.1 is in CGN range 100.64.0.0/10
    InetAddress cgnAddress = InetAddress.getByAddress("cgn", new byte[]{100, 75, 0, 1});
    assertThat(JwtBearerJwksUriPolicy.isPrivateOrLoopbackAddress(cgnAddress)).isTrue();
  }

  @Test
  @DisplayName("rejects benchmarking range (198.18.0.0/15)")
  void isPrivateOrLoopbackAddress_rejectsBenchmarkingRange() throws Exception {
    // 198.18.0.1 is in benchmarking range 198.18.0.0/15
    InetAddress benchmarkAddress = InetAddress.getByAddress("bench",
        new byte[]{(byte) 198, 18, 0, 1});
    assertThat(JwtBearerJwksUriPolicy.isPrivateOrLoopbackAddress(benchmarkAddress)).isTrue();
  }

  @Test
  @DisplayName("accepts truly global public IPv4 addresses")
  void isPrivateOrLoopbackAddress_acceptsPublicIpv4() throws Exception {
    InetAddress publicAddress = InetAddress.getByAddress("public", new byte[]{8, 8, 8, 8});
    assertThat(JwtBearerJwksUriPolicy.isPrivateOrLoopbackAddress(publicAddress)).isFalse();
  }
}
