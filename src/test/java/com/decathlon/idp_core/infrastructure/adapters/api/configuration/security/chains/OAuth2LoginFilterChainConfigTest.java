package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.GlobalAuthorizationFilter;
import com.decathlon.idp_core.infrastructure.adapters.api.auth.JitProvisioningFilter;

@DisplayName("OAuth2LoginFilterChainConfigTest")
@ExtendWith(MockitoExtension.class)
class OAuth2LoginFilterChainConfigTest {

  @Mock
  DefaultSecurityFilterChain securityFilterChain;

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(OAuth2LoginFilterChainConfig.class))
      .withBean(JitProvisioningFilter.class, () -> mock(JitProvisioningFilter.class))
      .withBean(GlobalAuthorizationFilter.class, () -> mock(GlobalAuthorizationFilter.class))
      .withBean(ClientRegistrationRepository.class, () -> mock(ClientRegistrationRepository.class))
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

  @Test
  void shouldNotCreateOauth2LoginFilterChainWithoutClientRegistration() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OAuth2LoginFilterChainConfig.class))
        .withBean(JitProvisioningFilter.class, () -> mock(JitProvisioningFilter.class))
        .withBean(GlobalAuthorizationFilter.class, () -> mock(GlobalAuthorizationFilter.class))
        .withBean(HttpSecurity.class, this::httpSecurity)
        .withPropertyValues("app.security.authentication.oauth2-login.enabled=true")
        .run(context -> assertThat(context).doesNotHaveBean(OAuth2LoginFilterChainConfig.class)
            .doesNotHaveBean(SecurityFilterChain.class));
  }

  @Test
  void shouldApplyGlobalAuthorizationAfterJitProvisioning() {
    var jitFilter = mock(JitProvisioningFilter.class);
    var authorizationFilter = mock(GlobalAuthorizationFilter.class);
    var configuration = new OAuth2LoginFilterChainConfig(jitFilter, authorizationFilter);
    HttpSecurity http = httpSecurity();

    configuration.oauth2LoginSecurityFilterChain(http);

    verify(http).addFilterAfter(authorizationFilter, JitProvisioningFilter.class);
  }

  private HttpSecurity httpSecurity() {
    HttpSecurity http = mock(HttpSecurity.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
    when(http.build()).thenReturn(securityFilterChain);
    when(http.sessionManagement(any())).thenReturn(http);
    when(http.cors(any())).thenReturn(http);
    when(http.csrf(any())).thenReturn(http);
    when(http.authorizeHttpRequests(any())).thenReturn(http);
    when(http.oauth2Login(any())).thenReturn(http);
    when(http.addFilterAfter(any(), any())).thenReturn(http);
    return http;
  }
}
