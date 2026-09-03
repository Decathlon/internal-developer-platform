package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.JitProvisioningFilter;

@DisplayName("OAuth2LoginFilterChainConfigTest")
@ExtendWith(MockitoExtension.class)
class OAuth2LoginFilterChainConfigTest {

  @Mock
  DefaultSecurityFilterChain securityFilterChain;

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(OAuth2LoginFilterChainConfig.class))
      .withBean(JitProvisioningFilter.class, () -> mock(JitProvisioningFilter.class))
      .withBean(HttpSecurity.class, this::httpSecurity);

  @Test
  void shouldCreateOauth2LoginFilterChainWhenEnabled() {
    contextRunner.withPropertyValues("app.security.authentication.oauth2-login.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(OAuth2LoginFilterChainConfig.class)
            .hasSingleBean(SecurityFilterChain.class));
  }

  @Test
  void shouldNotCreateOauth2LoginFilterChainWhenDisabled() {
    contextRunner.withPropertyValues("app.security.authentication.oauth2-login.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(OAuth2LoginFilterChainConfig.class)
            .doesNotHaveBean(SecurityFilterChain.class));
  }

  private HttpSecurity httpSecurity() {
    HttpSecurity http = mock(HttpSecurity.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
    when(http.build()).thenReturn(securityFilterChain);
    return http;
  }
}
