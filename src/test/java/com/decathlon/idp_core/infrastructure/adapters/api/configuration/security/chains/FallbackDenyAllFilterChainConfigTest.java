package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

@DisplayName("FallbackDenyAllFilterChainConfigTest")
@ExtendWith(MockitoExtension.class)
class FallbackDenyAllFilterChainConfigTest {

  @Mock
  DefaultSecurityFilterChain securityFilterChain;

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(FallbackDenyAllFilterChainConfig.class))
      .withBean(HttpSecurity.class, this::httpSecurity);

  @Test
  void shouldCreateDenyAllChainWhenNoAuthenticationPropertyIsSet() {
    contextRunner
        .run(context -> assertThat(context).hasSingleBean(FallbackDenyAllFilterChainConfig.class)
            .hasSingleBean(SecurityFilterChain.class));
  }

  @Test
  void shouldCreateDenyAllChainWhenEveryModeIsExplicitlyDisabled() {
    contextRunner
        .withPropertyValues("app.security.authentication.jwt.enabled=false",
            "app.security.authentication.oauth2-login.enabled=false",
            "app.security.authentication.api-key.enabled=false",
            "app.security.authentication.mock.enabled=false")
        .run(context -> assertThat(context).hasSingleBean(FallbackDenyAllFilterChainConfig.class)
            .hasSingleBean(SecurityFilterChain.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"jwt", "oauth2-login", "api-key", "mock"})
  void shouldNotCreateDenyAllChainWhenOneModeIsEnabled(String mode) {
    contextRunner.withPropertyValues("app.security.authentication." + mode + ".enabled=true")
        .run(context -> assertThat(context).doesNotHaveBean(FallbackDenyAllFilterChainConfig.class)
            .doesNotHaveBean(SecurityFilterChain.class));
  }

  private HttpSecurity httpSecurity() {
    HttpSecurity http = mock(HttpSecurity.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
    when(http.build()).thenReturn(securityFilterChain);
    return http;
  }
}
