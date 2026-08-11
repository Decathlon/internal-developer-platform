package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

class ApiKeyFilterChainConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ApiKeyFilterChainConfig.class))
      .withBean(HttpSecurity.class, () -> httpSecurity());

  @Test
  void shouldCreateApiKeyFilterChainWhenEnabled() {
    contextRunner.withPropertyValues("app.security.authentication.api-key.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(ApiKeyFilterChainConfig.class)
            .hasSingleBean(SecurityFilterChain.class));
  }

  @Test
  void shouldNotCreateApiKeyFilterChainWhenDisabled() {
    contextRunner.withPropertyValues("app.security.authentication.api-key.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(ApiKeyFilterChainConfig.class)
            .doesNotHaveBean(SecurityFilterChain.class));
  }

  private HttpSecurity httpSecurity() {
    HttpSecurity http = mock(HttpSecurity.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
    when(http.build()).thenReturn(mock(DefaultSecurityFilterChain.class));
    return http;
  }
}
