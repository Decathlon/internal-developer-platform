package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// Dedicated security filter chain for inbound webhook ingestion routes.
// Authentication model:
// External callers do not authenticate through Spring Security for this entrypoint.
// Authentication/authorization is enforced at application level in the ingestion
// pipeline (for example HMAC, static token, or JWT subscription checks).
// CSRF model (Sonar S4502):
// CSRF attacks rely on browser sessions and cookies. Webhook ingestion is pure
// machine-to-machine traffic, so browser CSRF protection does not apply. For this
// reason, CSRF is intentionally ignored for `/webhooks/**`.
@SuppressWarnings("java:S4502") // CSRF intentionally ignored for machine-to-machine webhook
                                // endpoints
@Configuration
@ConditionalOnProperty(name = "app.security.mock-enabled", havingValue = "false", matchIfMissing = true)
public class IngestionSecurityConfiguration {

  // Configures a dedicated filter chain for webhook ingestion.
  // The chain is scoped to `/webhooks/**`, allows unauthenticated access at
  // Spring
  // Security level, and ignores CSRF for these M2M endpoints.
  @Bean
  @Order(1)
  public SecurityFilterChain ingestionSecurityFilterChain(HttpSecurity http) {
    http.securityMatcher("/webhooks/**")
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .csrf(csrf -> csrf.ignoringRequestMatchers("/webhooks/**"));

    return http.build();
  }
}
