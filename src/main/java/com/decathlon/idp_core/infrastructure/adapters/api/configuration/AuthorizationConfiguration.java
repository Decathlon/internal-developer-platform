package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.infrastructure.adapters.api.auth.GlobalAuthorizationFilter;

/// Bridges external authorization properties into the pure domain policy.
@Configuration
@EnableConfigurationProperties(AuthorizationProperties.class)
public class AuthorizationConfiguration {

  /// Binds a disabled servlet registration so the filter only runs in selected
  /// security chains.
  ///
  /// @param filter the filter instance used by the JWT and mock security chains
  /// @return a disabled servlet-container registration
  @Bean
  public FilterRegistrationBean<GlobalAuthorizationFilter> globalAuthorizationFilterRegistration(
      GlobalAuthorizationFilter filter) {
    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  /// Creates the pure domain policy from externalized authorization settings.
  ///
  /// @param properties external authorization configuration
  /// @return an immutable domain authorization policy
  @Bean
  public AuthorizationPolicy authorizationPolicy(AuthorizationProperties properties) {
    return new AuthorizationPolicy(properties.mode(),
        Set.copyOf(properties.globalPrincipalIdentifiers()),
        Optional.ofNullable(properties.requiredPrincipalProperty()));
  }
}
