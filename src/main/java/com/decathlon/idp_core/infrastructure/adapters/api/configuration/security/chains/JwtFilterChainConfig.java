package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.JitProvisioningFilter;
import com.decathlon.idp_core.infrastructure.adapters.api.security.GlobalAuthorizationManager;

@Configuration
@ConditionalOnProperty(prefix = "app.security.authentication.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtFilterChainConfig {

  private final JitProvisioningFilter jitProvisioningFilter;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;
  private final GlobalAuthorizationManager globalAuthorizationManager;

  public JwtFilterChainConfig(JitProvisioningFilter jitProvisioningFilter,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      GlobalAuthorizationManager globalAuthorizationManager) {
    this.jitProvisioningFilter = jitProvisioningFilter;
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    this.globalAuthorizationManager = globalAuthorizationManager;
  }

  @Bean
  @Order(2)
  public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) {
    http.authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/**")
        .access(globalAuthorizationManager).anyRequest().authenticated()).cors(withDefaults())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .addFilterAfter(jitProvisioningFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }
}
