package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

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

import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties.ServiceAccountDetection;

@DisplayName("PublicFilterChainConfigTest")
@ExtendWith(MockitoExtension.class)
class PublicFilterChainConfigTest {

  @Mock
  DefaultSecurityFilterChain securityFilterChain;

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(PublicFilterChainConfig.class))
      .withBean(AuthenticationProperties.class,
          () -> authenticationProperties(List.of("/public/**")))
      .withBean(HttpSecurity.class, () -> httpSecurity());

  @Test
  void shouldCreatePublicFilterChain() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(PublicFilterChainConfig.class)
        .hasSingleBean(SecurityFilterChain.class));
  }

  private AuthenticationProperties authenticationProperties(List<String> excludedPaths) {
    return new AuthenticationProperties(Map.of(),
        new ServiceAccountDetection(false, "legacy", "token_type", "m2m", List.of()),
        excludedPaths);
  }

  private HttpSecurity httpSecurity() {
    HttpSecurity http = mock(HttpSecurity.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
    when(http.build()).thenReturn(securityFilterChain);
    return http;
  }
}
