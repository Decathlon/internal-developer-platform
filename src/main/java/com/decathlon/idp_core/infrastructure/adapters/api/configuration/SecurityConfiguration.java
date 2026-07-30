package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.JitProvisioningFilter;

/// Spring Security configuration for OAuth2 resource server with JWT authentication.
///
/// **Security policy rationale:**
/// - Public access: Actuator endpoints for health monitoring, Swagger UI for API documentation
/// - Protected access: All `/api/v1/**` endpoints require full authentication via JWT
/// - OAuth2 integration: JWT tokens validated against configured JWKS endpoint
///
/// **Infrastructure specifics:**
/// - CORS origins externalized via `spring.web.cors.allowed-origins` in `application.yml`
/// - JWT resource server autoconfigured with Spring Security OAuth2
/// - Security filter chain processes authentication before reaching controllers

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({CorsProperties.class, SecurityRoleProperties.class})
@ConditionalOnProperty(name = "app.security.mock-enabled", havingValue = "false", matchIfMissing = true)
public class SecurityConfiguration {

  private final CorsProperties corsProperties;
  private final JitProvisioningFilter jitProvisioningFilter;
  private final SecurityRoleProperties securityRoleProperties;

  public SecurityConfiguration(CorsProperties corsProperties,
      JitProvisioningFilter jitProvisioningFilter, SecurityRoleProperties securityRoleProperties) {
    this.corsProperties = corsProperties;
    this.jitProvisioningFilter = jitProvisioningFilter;
    this.securityRoleProperties = securityRoleProperties;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http.authorizeHttpRequests(authorize -> authorize.requestMatchers("/actuator/**").permitAll()
        .requestMatchers("/", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
        .requestMatchers("/api/v1/**").fullyAuthenticated().anyRequest().authenticated())
        .cors(withDefaults())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .addFilterAfter(jitProvisioningFilter, BearerTokenAuthenticationFilter.class);
    return http.build();
  }

  /// Configures JWT authentication converter with baseline role assignment.
  ///
  /// **Business purpose:** Assigns the configured baseline role (default: `*`
  /// Super Admin)
  /// to all authenticated principals during V1 phase. This prevents authorization
  /// blocks
  /// during initial catalog ingestion and schema evolution.
  ///
  /// **Design rationale:** Role assignment happens at authentication time, before
  /// JIT
  /// provisioning. This ensures the principal has necessary permissions to
  /// trigger
  /// catalog operations if needed.
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> List.of(new SimpleGrantedAuthority(securityRoleProperties.baselineRole())));
    return converter;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Exact origins (no wildcard, safe with allowCredentials)
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
