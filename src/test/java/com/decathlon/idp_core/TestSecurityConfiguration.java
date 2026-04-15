package com.decathlon.idp_core;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/// Test security configuration that disables authentication for integration tests.
///
/// **Why this exists:** Integration tests need to bypass security mechanisms
/// to focus on business logic testing rather than authentication flows.
/// This configuration ensures all requests are permitted without authentication.
@TestConfiguration
@EnableWebSecurity
@Profile({ "test" })
public class TestSecurityConfiguration {
  /// Configures a permissive security filter chain for testing.
  ///
  /// **Security settings:**
  /// - CSRF protection disabled (not needed for API tests)
  /// - All requests permitted without authentication
  ///
  /// **Why permissive security:** Test scenarios focus on business logic validation
  /// rather than security enforcement, requiring unrestricted access.
  @Bean
  public SecurityFilterChain securityFilterChainTest(HttpSecurity http) throws Exception {

    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
    return http.build();
  }
}
