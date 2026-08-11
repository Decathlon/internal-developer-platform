package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/// Security filter chain for API key authentication.
///
/// **Configuration:**
/// - Session: Stateless (CSRF protection not needed for token-based
/// authentication)
/// - Authorization: All requests to `/api/v1/**` require full authentication
/// - CORS: Default configuration
///
@Configuration
@ConditionalOnProperty(prefix = "app.security.authentication.api-key", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApiKeyFilterChainConfig {

  @Bean
  @Order(3)
  public SecurityFilterChain apiKeySecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/**").fullyAuthenticated()
        .anyRequest().authenticated()).cors(withDefaults());
    return http.build();
  }
}
