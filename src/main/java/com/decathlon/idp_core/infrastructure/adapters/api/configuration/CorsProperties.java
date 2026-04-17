package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Type-safe CORS configuration properties bound from `spring.web.cors`.
@ConfigurationProperties(prefix = "spring.web.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
    public CorsProperties {
        if (allowedOrigins == null) {
            allowedOrigins = List.of();
        }
    }
}