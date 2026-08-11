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

@Configuration
@ConditionalOnProperty(prefix = "app.security.authentication.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtFilterChainConfig {

  private final JitProvisioningFilter jitProvisioningFilter;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;

  public JwtFilterChainConfig(JitProvisioningFilter jitProvisioningFilter,
      JwtAuthenticationConverter jwtAuthenticationConverter) {
    this.jitProvisioningFilter = jitProvisioningFilter;
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
  }

  @Bean
  @Order(2)
  public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/**").fullyAuthenticated().anyRequest().authenticated())
        .cors(withDefaults())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .addFilterAfter(jitProvisioningFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }
}
