package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;

@Configuration
public class PublicFilterChainConfig {

  private final AuthenticationProperties authProperties;

  public PublicFilterChainConfig(AuthenticationProperties authProperties) {
    this.authProperties = authProperties;
  }

  @Bean
  @Order(1) // Evaluated first
  public SecurityFilterChain publicFilterChain(HttpSecurity http) {
    // Uses the DRY properties from YAML
    String[] publicPaths = authProperties.jitProvisioningExcludedPaths().toArray(new String[0]);

    http.securityMatcher(publicPaths).authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .cors(withDefaults());

    return http.build();
  }
}
