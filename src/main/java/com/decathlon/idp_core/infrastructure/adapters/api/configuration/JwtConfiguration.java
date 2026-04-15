package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Custom JwtDecoder configuration.
 * <p>
 * By defining this bean manually, we override Spring Boot's strict default auto-configuration.
 * This is required because the Identity Provider's JWKS response might be missing explicit
 * 'alg' (Algorithm) or 'use' attributes on the keys.
 * </p>
 * <p>
 * The bare-bones {@code NimbusJwtDecoder.withJwkSetUri().build()} defaults to RS256 and is
 * more forgiving. It successfully decodes the token as long as it finds a valid key with a
 * matching 'kid', bypassing strict attribute validation failures that would normally drop the key.
 * </p>
 */
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
