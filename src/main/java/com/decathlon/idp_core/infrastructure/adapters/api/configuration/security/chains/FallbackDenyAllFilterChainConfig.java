package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/// Fail-closed fallback applied when no authentication mode is enabled.
///
/// **Security rationale:** every authenticated chain (JWT, OAuth2 login, API key, mock) is
/// conditional on its own `app.security.authentication.*.enabled` flag, and only the public
/// and webhook chains declare a request matcher. Without this fallback, a deployment where
/// all flags are `false` leaves `/api/v1/**` outside every filter chain, which means the API
/// is served without authentication or authorization.
///
/// **Behaviour:** when none of the modes is enabled, this chain matches every request not
/// already handled by the public or webhook chains and denies it. It is not registered when
/// at least one mode is enabled, because those chains already match any request and Spring
/// Security rejects a chain declared after an any-request chain as unreachable.
@Configuration
@Conditional(FallbackDenyAllFilterChainConfig.NoAuthenticationModeEnabled.class)
public class FallbackDenyAllFilterChainConfig {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FallbackDenyAllFilterChainConfig.class);

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  public SecurityFilterChain fallbackDenyAllFilterChain(HttpSecurity http) {
    LOGGER.warn(
        "No authentication mode is enabled (app.security.authentication.[jwt|oauth2-login|api-key|mock].enabled). "
            + "All non-public requests will be denied.");
    http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
    return http.build();
  }

  /// Matches when none of the authentication modes is switched on.
  static class NoAuthenticationModeEnabled extends NoneNestedConditions {

    NoAuthenticationModeEnabled() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(prefix = "app.security.authentication.jwt", name = "enabled", havingValue = "true")
    static class JwtEnabled {
    }

    @ConditionalOnProperty(prefix = "app.security.authentication.oauth2-login", name = "enabled", havingValue = "true")
    static class OAuth2LoginEnabled {
    }

    @ConditionalOnProperty(prefix = "app.security.authentication.api-key", name = "enabled", havingValue = "true")
    static class ApiKeyEnabled {
    }

    @ConditionalOnProperty(prefix = "app.security.authentication.mock", name = "enabled", havingValue = "true")
    static class MockEnabled {
    }
  }
}
