package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/// Spring Security configuration for OAuth2 resource server with JWT authentication.
///
/// **Security policy rationale:**
/// - Public access: Actuator endpoints for health monitoring, Swagger UI for API documentation
/// - Protected access: All `/api/v1/**` endpoints require full authentication via JWT
/// - OAuth2 integration: JWT tokens validated against configured JWKS endpoint
///
/// **Infrastructure specifics:**
/// - CORS origins externalized via `spring.web.cors.allowed-origins` in `application.yml`
/// - JWT resource server auto-configured with Spring Security OAuth2
/// - Security filter chain processes authentication before reaching controllers

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${spring.web.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/**").fullyAuthenticated().anyRequest().authenticated()
                )
                .cors(withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
