package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/// Custom JWT decoder configuration for OAuth2 resource server integration.
///
/// **Infrastructure rationale:** Overrides Spring Boot's strict auto-configuration to handle
/// identity providers with non-standard JWKS responses. Some providers omit explicit 'alg'
/// or 'use' attributes on keys, causing Spring's default decoder to fail validation.
///
/// **Technical solution:** Uses bare-bones NimbusJwtDecoder configuration that defaults to
/// RS256 and is more permissive with key validation. Successfully decodes tokens with valid
/// 'kid' matching, bypassing strict attribute validation that would drop keys.
///
/// **Security considerations:** Maintains security while providing flexibility for various
/// identity provider implementations that may not follow all JWKS specifications exactly.
@Configuration
public class JwtConfiguration {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .build();
    }
}
