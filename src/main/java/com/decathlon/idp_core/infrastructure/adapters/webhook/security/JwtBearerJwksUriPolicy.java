package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.function.Function;

import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;

/// Shared JWKS URI policy used at configuration time and runtime fetch time.
final class JwtBearerJwksUriPolicy {

  private static final String HTTPS_SCHEME = "https";
  private static final String LOCALHOST = "localhost";

  private JwtBearerJwksUriPolicy() {
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

  private static void validateUri(URI jwksUri,
      Function<String, RuntimeException> exceptionFactory) {
    if (!HTTPS_SCHEME.equalsIgnoreCase(jwksUri.getScheme())) {
      throw exceptionFactory
          .apply("Invalid jwks_uri for JWT_BEARER security: only HTTPS URIs are allowed");
    }

    if (!StringUtils.hasText(jwksUri.getHost())) {
      throw exceptionFactory.apply("Invalid jwks_uri for JWT_BEARER security: host is missing");
    }

    if (isPrivateOrLoopbackHost(jwksUri.getHost(), exceptionFactory)) {
      throw exceptionFactory.apply(
          "Invalid jwks_uri for JWT_BEARER security: localhost, loopback, private and link-local hosts are not allowed");
    }
  }

  private static boolean isPrivateOrLoopbackHost(String host,
      Function<String, RuntimeException> exceptionFactory) {
    String normalizedHost = host.trim();

    if (LOCALHOST.equalsIgnoreCase(normalizedHost)) {
      return true;
    }

    try {
      for (InetAddress address : InetAddress.getAllByName(normalizedHost)) {
        byte[] rawAddress = address.getAddress();
        boolean isIpv6UniqueLocal = rawAddress.length == 16 && (rawAddress[0] & 0xfe) == 0xfc;
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
            || address.isLinkLocalAddress() || address.isSiteLocalAddress() || isIpv6UniqueLocal
            || address.isMulticastAddress()) {
          return true;
        }
      }
      return false;
    } catch (UnknownHostException _) {
      throw exceptionFactory
          .apply("Invalid jwks_uri for JWT_BEARER security: host cannot be resolved");
    }
  }
}
