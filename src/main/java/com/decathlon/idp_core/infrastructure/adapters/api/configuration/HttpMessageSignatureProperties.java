package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/// HTTP message signature verification configuration.
@ConfigurationProperties("app.security.http-message-signature")
public record HttpMessageSignatureProperties(@DefaultValue("true") boolean enabled, String secret,
    @DefaultValue("x-request-id") String identifierHeaderName,
    @DefaultValue("HMAC_SHA256") HttpMessageSignatureAlgorithm algorithm,
    @DefaultValue("30") long validityDurationSeconds, @DefaultValue("0") long clockSkewSeconds,
    @DefaultValue( {
        "/actuator/health", "/actuator/health/**", "/swagger-ui.html", "/swagger-ui/**",
        "/v3/api-docs/**"}) List<String> excludedPaths){

  /// Validates startup policy and takes an immutable copy of the exclusion paths.
  public HttpMessageSignatureProperties {
    if (enabled && (secret == null || secret.isBlank())) {
      throw new IllegalArgumentException("HTTP message signature secret is required when enabled");
    }
    if (identifierHeaderName == null || !identifierHeaderName.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
        || identifierHeaderName.equalsIgnoreCase("Authorization")
        || identifierHeaderName.equalsIgnoreCase("Signature")) {
      throw new IllegalArgumentException("Invalid HTTP message signature ID header name");
    }
    Objects.requireNonNull(algorithm, "HTTP message signature algorithm is required");
    if (validityDurationSeconds <= 0 || clockSkewSeconds < 0) {
      throw new IllegalArgumentException(
          "HTTP message signature validity must be positive and clock skew nonnegative");
    }
    excludedPaths = List.copyOf(excludedPaths);
    if (excludedPaths.stream()
        .anyMatch(path -> !path.startsWith("/") || path.contains("\r") || path.contains("\n"))) {
      throw new IllegalArgumentException(
          "HTTP message signature exclusions must be absolute Ant paths");
    }
  }

  @Override
  public String toString() {
    return "HttpMessageSignatureProperties[enabled=" + enabled + ", secret=[REDACTED]"
        + ", identifierHeaderName=" + identifierHeaderName + ", algorithm=" + algorithm
        + ", validityDurationSeconds=" + validityDurationSeconds + ", clockSkewSeconds="
        + clockSkewSeconds + ", excludedPaths=" + excludedPaths + "]";
  }
}
