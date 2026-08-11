package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.JitProvisioningFilter;

class JwtFilterChainConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(JwtFilterChainConfig.class))
      .withBean(JitProvisioningFilter.class, () -> mock(JitProvisioningFilter.class))
      .withBean(JwtAuthenticationConverter.class, JwtAuthenticationConverter::new)
      .withBean(HttpSecurity.class, () -> httpSecurity());

  @Test
  void shouldCreateJwtFilterChainWhenEnabled() {
    contextRunner.withPropertyValues("app.security.authentication.jwt.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(JwtFilterChainConfig.class)
            .hasSingleBean(SecurityFilterChain.class));
  }

  @Test
  void shouldNotCreateJwtFilterChainWhenDisabled() {
    contextRunner.withPropertyValues("app.security.authentication.jwt.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(JwtFilterChainConfig.class)
            .doesNotHaveBean(SecurityFilterChain.class));
  }

  private HttpSecurity httpSecurity() {
    HttpSecurity http = mock(HttpSecurity.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
    when(http.build()).thenReturn(mock(DefaultSecurityFilterChain.class));
    return http;
  }
}
