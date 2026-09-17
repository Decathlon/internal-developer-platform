package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.client.RestClientException;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;

@DisplayName("JwtBearerJwksUriPolicy Tests")
class JwtBearerJwksUriPolicyTest {

  @Test
  @DisplayName("cannot be instantiated because it is a utility class")
  void constructor_isPrivateAndThrows() throws Exception {
    Constructor<JwtBearerJwksUriPolicy> constructor = JwtBearerJwksUriPolicy.class
        .getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThatThrownBy(constructor::newInstance)
        .hasCauseInstanceOf(UnsupportedOperationException.class)
        .hasRootCauseMessage("Utility class");
  }

  @Test
  @DisplayName("accepts a public HTTPS JWKS URI")
  void validateConfiguredJwksUri_acceptsPublicHttpsUri() {
    assertThatCode(
        () -> JwtBearerJwksUriPolicy.validateConfiguredJwksUri("https://8.8.8.8/oauth2/v3/certs"))
            .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("trims surrounding whitespace before validating a configured JWKS URI")
  void validateConfiguredJwksUri_trimsWhitespace() {
    assertThatCode(() -> JwtBearerJwksUriPolicy
        .validateConfiguredJwksUri("  https://8.8.8.8/oauth2/v3/certs  "))
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
  @DisplayName("rejects malformed configured JWKS URIs")
  void validateConfiguredJwksUri_rejectsMalformedUri() {
    assertThatThrownBy(
        () -> JwtBearerJwksUriPolicy.validateConfiguredJwksUri("https://issuer.example.com/ bad"))
            .isInstanceOf(WebhookSecurityConfigurationException.class)
            .hasMessageContaining("URI is malformed");
  }

  @Test
  @DisplayName("rejects configured JWKS URIs without a host")
  void validateConfiguredJwksUri_rejectsMissingHost() {
    assertThatThrownBy(() -> JwtBearerJwksUriPolicy.validateConfiguredJwksUri("https:///jwks"))
        .isInstanceOf(WebhookSecurityConfigurationException.class)
        .hasMessageContaining("host_missing");
  }

  @Test
  @DisplayName("rejects configured JWKS URIs that resolve to an unresolvable host")
  void validateConfiguredJwksUri_rejectsUnresolvableHost() {
    assertThatThrownBy(() -> JwtBearerJwksUriPolicy
        .validateConfiguredJwksUri("https://does-not-resolve.invalid/jwks"))
            .isInstanceOf(WebhookSecurityConfigurationException.class)
            .hasMessageContaining("host_unresolvable");
  }

  @Test
  @DisplayName("rejects localhost JWKS URIs at runtime fetch time")
  void validateRuntimeJwksUri_rejectsLocalhost() {
    URI jwksUri = URI.create("https://localhost/.well-known/jwks.json");

    assertThatThrownBy(() -> JwtBearerJwksUriPolicy.validateRuntimeJwksUri(jwksUri))
        .isInstanceOf(RestClientException.class).hasMessageContaining("unsafe_host");
  }

  @Test
  @DisplayName("rejects IPv6 loopback JWKS URIs at runtime fetch time")
  void validateRuntimeJwksUri_rejectsIpv6Loopback() {
    URI jwksUri = URI.create("https://[::1]/.well-known/jwks.json");

    assertThatThrownBy(() -> JwtBearerJwksUriPolicy.validateRuntimeJwksUri(jwksUri))
        .isInstanceOf(RestClientException.class).hasMessageContaining("unsafe_host");
  }

  @Test
  @DisplayName("rejects runtime JWKS URIs without a host")
  void validateRuntimeJwksUri_rejectsMissingHost() {
    URI jwksUri = URI.create("https:///jwks");

    assertThatThrownBy(() -> JwtBearerJwksUriPolicy.validateRuntimeJwksUri(jwksUri))
        .isInstanceOf(RestClientException.class).hasMessageContaining("host_missing");
  }

  @Test
  @DisplayName("rejects runtime JWKS URIs that resolve to an unresolvable host")
  void validateRuntimeJwksUri_rejectsUnresolvableHost() {
    URI jwksUri = URI.create("https://does-not-resolve.invalid/jwks");

    assertThatThrownBy(() -> JwtBearerJwksUriPolicy.validateRuntimeJwksUri(jwksUri))
        .isInstanceOf(RestClientException.class).hasMessageContaining("host_unresolvable");
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
  @DisplayName("returns false for an empty address list")
  void containsPrivateOrLoopbackAddress_returnsFalseForEmptyArray() {
    assertThat(JwtBearerJwksUriPolicy.containsPrivateOrLoopbackAddress(new InetAddress[]{}))
        .isFalse();
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

  @ParameterizedTest(name = "{0}")
  @MethodSource("reservedAddresses")
  @DisplayName("rejects reserved addresses that must never be used for JWKS fetching")
  void isPrivateOrLoopbackAddress_rejectsReservedAddresses(InetAddress address) {
    assertThat(JwtBearerJwksUriPolicy.isPrivateOrLoopbackAddress(address)).isTrue();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("publicAddresses")
  @DisplayName("accepts public addresses")
  void isPrivateOrLoopbackAddress_acceptsPublicAddresses(InetAddress address) {
    assertThat(JwtBearerJwksUriPolicy.isPrivateOrLoopbackAddress(address)).isFalse();
  }

  @Test
  @DisplayName("accepts truly global public IPv4 addresses")
  void isPrivateOrLoopbackAddress_acceptsPublicIpv4() throws Exception {
    InetAddress publicAddress = InetAddress.getByAddress("public", new byte[]{8, 8, 8, 8});
    assertThat(JwtBearerJwksUriPolicy.isPrivateOrLoopbackAddress(publicAddress)).isFalse();
  }

  private static Stream<Arguments> reservedAddresses() throws Exception {
    return Stream.of(Arguments.of(InetAddress.getByAddress("any-local", new byte[]{0, 0, 0, 0})),
        Arguments.of(InetAddress.getByAddress("loopback", new byte[]{127, 0, 0, 1})),
        Arguments
            .of(InetAddress.getByAddress("link-local", new byte[]{(byte) 169, (byte) 254, 0, 1})),
        Arguments
            .of(InetAddress.getByAddress("site-local", new byte[]{(byte) 192, (byte) 168, 0, 1})),
        Arguments.of(InetAddress.getByAddress("multicast", new byte[]{(byte) 224, 0, 0, 1})),
        Arguments.of(InetAddress.getByAddress("ula",
            new byte[]{(byte) 0xfc, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1})),
        Arguments.of(InetAddress.getByAddress("cgn-low", new byte[]{(byte) 100, 64, 0, 1})),
        Arguments.of(InetAddress.getByAddress("cgn-high", new byte[]{(byte) 100, 127, 0, 1})),
        Arguments.of(InetAddress.getByAddress("bench-low", new byte[]{(byte) 198, 18, 0, 1})),
        Arguments.of(InetAddress.getByAddress("bench-high", new byte[]{(byte) 198, 19, 0, 1})));
  }

  private static Stream<Arguments> publicAddresses() throws Exception {
    return Stream.of(Arguments.of(InetAddress.getByAddress("public-v4", new byte[]{8, 8, 8, 8})),
        Arguments.of(InetAddress.getByAddress("public-v6", new byte[]{(byte) 0x20, 0x01, 0x48, 0x60,
            0x48, 0x60, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0x88, (byte) 0x88})));
  }
}
