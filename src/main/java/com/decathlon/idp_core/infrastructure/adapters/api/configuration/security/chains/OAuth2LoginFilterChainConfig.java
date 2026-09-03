package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.JitProvisioningFilter;

/// Security filter chain for browser-based OAuth2 login.
///
/// **Purpose:** Enables local interactive authentication against an OAuth2 provider
/// such as GitHub. Once Spring Security authenticates the user, the JIT provisioning
/// filter provisions the principal in the catalog on the callback request.
@Configuration
@ConditionalOnProperty(prefix = "app.security.authentication.oauth2-login", name = "enabled", havingValue = "true")
public class OAuth2LoginFilterChainConfig {

  private final JitProvisioningFilter jitProvisioningFilter;

  public OAuth2LoginFilterChainConfig(JitProvisioningFilter jitProvisioningFilter) {
    this.jitProvisioningFilter = jitProvisioningFilter;
  }

  @Bean
  @Order(2)
  public SecurityFilterChain oauth2LoginSecurityFilterChain(HttpSecurity http) {
    http.sessionManagement(
        session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .cors(withDefaults()).csrf(withDefaults())
        .authorizeHttpRequests(auth -> auth.requestMatchers("/oauth2/**", "/login/**").permitAll()
            .requestMatchers("/api/v1/**").fullyAuthenticated().anyRequest().authenticated())
        .oauth2Login(withDefaults())
        .addFilterAfter(jitProvisioningFilter, OAuth2LoginAuthenticationFilter.class);

    return http.build();
  }
}
