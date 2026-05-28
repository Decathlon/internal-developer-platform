package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Type-safe CORS configuration properties bound from `spring.web.cors`.
@ConfigurationProperties(prefix = "spring.web.cors")
public record CorsProperties(List<String> allowedOrigins, List<String> allowedOriginPatterns) {
  /// Compact constructor: normalises null to empty and defensively copies every
  /// list
  /// to prevent external mutation of the internal state (EI_EXPOSE_REP /
  /// EI_EXPOSE_REP2).
  /// List.copyOf() also rejects null elements, enforcing a clean configuration
  /// contract.
  public CorsProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    allowedOriginPatterns = allowedOriginPatterns == null
        ? List.of()
        : List.copyOf(allowedOriginPatterns);
  }
}
