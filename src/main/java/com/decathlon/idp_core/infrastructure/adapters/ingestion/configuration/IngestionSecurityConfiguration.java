package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/// Dedicated security filter chain for inbound webhook ingestion routes.
///
/// **Why a dedicated chain:**
/// - Isolates ingestion security from API security to enable modular extraction
/// - Keeps `/webhooks/**` intentionally public for external systems
/// - Restricts CSRF bypass to webhook endpoints only
@Configuration
@ConditionalOnProperty(name = "app.security.mock-enabled", havingValue = "false", matchIfMissing = true)
public class IngestionSecurityConfiguration {

  /// Configures the ingestion-only security filter chain.
  ///
  /// **Scope:** applies only to `/webhooks/**` routes.
  ///
  /// @param http HttpSecurity used to build the ingestion filter chain
  /// @return ingestion security filter chain
  @Bean
  @Order(1)
  public SecurityFilterChain ingestionSecurityFilterChain(HttpSecurity http) {
    http.securityMatcher("/webhooks/**")
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .csrf(csrf -> csrf.ignoringRequestMatchers("/webhooks/**"));
    return http.build();
  }
}
