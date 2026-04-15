package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/// Spring Security configuration for OAuth2 resource server with JWT authentication.
///
/// **Security policy rationale:**
/// - Public access: Actuator endpoints for health monitoring, Swagger UI for API documentation
/// - Protected access: All `/api/v1/**` endpoints require full authentication via JWT
/// - OAuth2 integration: JWT tokens validated against configured JWKS endpoint
///
/// **Infrastructure specifics:**
/// - CORS enabled with default configuration for cross-origin API access
/// - JWT resource server auto-configured with Spring Security OAuth2
/// - Security filter chain processes authentication before reaching controllers

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/**").fullyAuthenticated().anyRequest().authenticated()
                )
                .cors(withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
