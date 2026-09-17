package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.decathlon.idp_core.infrastructure.adapters.api.security.GlobalAuthorizationManager;

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

  private final GlobalAuthorizationManager globalAuthorizationManager;

  public ApiKeyFilterChainConfig(GlobalAuthorizationManager globalAuthorizationManager) {
    this.globalAuthorizationManager = globalAuthorizationManager;
  }

  @Bean
  @Order(3)
  public SecurityFilterChain apiKeySecurityFilterChain(HttpSecurity http) {
    http.authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/**")
        .access(globalAuthorizationManager).anyRequest().authenticated()).cors(withDefaults());
    return http.build();
  }
}
