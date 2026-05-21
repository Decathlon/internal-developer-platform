package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Type-safe CORS configuration properties bound from `spring.web.cors`.
@ConfigurationProperties(prefix = "spring.web.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedOriginPatterns
) {
    /// Compact constructor: normalises null to empty and defensively copies to prevent
    /// external mutation of the configuration list (EI_EXPOSE_REP2 / EI_EXPOSE_REP).
    public CorsProperties {
        allowedOrigins = allowedOrigins != null ? List.copyOf(allowedOrigins) : List.of();
        allowedOriginPatterns = allowedOriginPatterns != null ? List.copyOf(allowedOriginPatterns) : List.of();
    }
}
