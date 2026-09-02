package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.JitProvisioningFilter;

import lombok.extern.slf4j.Slf4j;

/// Spring Security configuration for API authentication and authorization.
///
/// **Security policy rationale:**
/// - Public access: Actuator endpoints for health monitoring, Swagger UI for API documentation
/// - Protected access: All `/api/v1/**` endpoints require authentication
/// - Authentication mechanism: Configurable via `app.security.authentication.mechanism`
///   - JWT (OAuth2 resource server with JWT validation)
///   - API_KEY (simple key-based authentication for webhooks)
///   - MOCK (for local development only)
/// API and platform Spring Security configuration for OAuth2 resource server with JWT
/// authentication.
///
/// **Infrastructure specifics:**
/// - CORS origins externalized via `spring.web.cors.allowed-origins` in `application.yml`
/// - Authentication mechanism conditionally applied based on configuration
/// - Security filter chain processes authentication before reaching controllers
/// - JIT provisioning filter adds authenticated principals to the catalog
///
/// **Future extensions:**
/// This configuration is designed to support multiple authentication backends:
/// - Add API_KEY authentication for webhook security (separate realm)
/// - Add SAML support for enterprise SSO
/// - Add mutual TLS (mTLS) for service-to-service authentication
///
/// @see AuthenticationProperties
/// @see JitProvisioningFilter
@Configuration
@EnableWebSecurity
@Slf4j
@EnableConfigurationProperties({CorsProperties.class, SecurityRoleProperties.class,
    AuthenticationProperties.class})
public class SecurityConfiguration {

  private final CorsProperties corsProperties;
  private final SecurityRoleProperties securityRoleProperties;

  public SecurityConfiguration(CorsProperties corsProperties,
      SecurityRoleProperties securityRoleProperties) {
    this.corsProperties = corsProperties;
    this.securityRoleProperties = securityRoleProperties;
  }

  ///
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> List.of(new SimpleGrantedAuthority(securityRoleProperties.baselineRole())));
    return converter;
  @Order(2)
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
    http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/actuator/**").permitAll()
        .requestMatchers("/", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
        .requestMatchers("/api/**").fullyAuthenticated().anyRequest().authenticated())
        .cors(withDefaults()).oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    if (!corsProperties.allowedOrigins().isEmpty()) {
      configuration.setAllowedOrigins(corsProperties.allowedOrigins());
    }
    if (!corsProperties.allowedOriginPatterns().isEmpty()) {
      configuration.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
    }
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
