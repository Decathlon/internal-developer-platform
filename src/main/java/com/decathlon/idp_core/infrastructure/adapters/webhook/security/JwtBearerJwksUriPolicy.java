package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;

import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;

/// Shared JWKS URI policy used at configuration time and runtime fetch time.
final class JwtBearerJwksUriPolicy {

  private static final String HTTPS_SCHEME = "https";
  private static final Set<String> BLOCKED_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

  private JwtBearerJwksUriPolicy() {
    throw new UnsupportedOperationException("Utility class");
  }

  static void validateConfiguredJwksUri(String jwksUri) {
    try {
      validateUri(new URI(jwksUri.trim()), WebhookSecurityConfigurationException::new);
    } catch (URISyntaxException | IllegalArgumentException _) {
      throw new WebhookSecurityConfigurationException(
          "Invalid jwks_uri for JWT_BEARER security: URI is malformed");
    }
  }

  static void validateRuntimeJwksUri(URI jwksUri) {
    validateUri(jwksUri, RestClientException::new);
  }

  private static void validateUri(URI jwksUri, ExceptionFactory exceptionFactory) {
    if (!HTTPS_SCHEME.equalsIgnoreCase(jwksUri.getScheme())) {
      throw exceptionFactory
          .create("Invalid jwks_uri for JWT_BEARER security: cause=only_https_allowed");
    }

    String host = jwksUri.getHost();
    if (!StringUtils.hasText(host)) {
      throw exceptionFactory.create("Invalid jwks_uri for JWT_BEARER security: cause=host_missing");
    }

    if (isPrivateOrLoopbackHost(host.trim(), exceptionFactory)) {
      throw exceptionFactory.create("Invalid jwks_uri for JWT_BEARER security: cause=unsafe_host");
    }
  }

  private static boolean isPrivateOrLoopbackHost(String host, ExceptionFactory exceptionFactory) {
    if (BLOCKED_HOSTS.contains(host.toLowerCase())) {
      return true;
    }

    try {
      return containsPrivateOrLoopbackAddress(InetAddress.getAllByName(host));
    } catch (UnknownHostException _) {
      throw exceptionFactory
          .create("Invalid jwks_uri for JWT_BEARER security: cause=host_unresolvable");
    }
  }

  static boolean containsPrivateOrLoopbackAddress(InetAddress[] addresses) {
    return Arrays.stream(addresses).anyMatch(JwtBearerJwksUriPolicy::isPrivateOrLoopbackAddress);
  }

  static boolean isPrivateOrLoopbackAddress(InetAddress address) {
    // 1. Predicates standards du JDK
    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
      return true;
    }

    byte[] bytes = address.getAddress();

    // 2. IPv6 Unique Local Addresses (fc00::/7 -> RFC 4193)
    if (bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC) {
      return true;
    }

    // 3. IPv4 Reserved Ranges : CGNAT (100.64.0.0/10) & Benchmarking
    // (198.18.0.0/15)
    if (bytes.length == 4) {
      int b0 = Byte.toUnsignedInt(bytes[0]);
      int b1 = Byte.toUnsignedInt(bytes[1]);

      boolean isCgnat = (b0 == 100 && b1 >= 64 && b1 <= 127);
      boolean isBenchmarking = (b0 == 198 && b1 == 18 || b1 == 19);

      return isCgnat || isBenchmarking;
    }

    return false;
  }
}
