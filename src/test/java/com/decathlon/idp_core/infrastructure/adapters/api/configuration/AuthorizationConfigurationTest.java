package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import com.decathlon.idp_core.domain.model.authorization.AuthorizationMode;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.infrastructure.adapters.api.auth.GlobalAuthorizationFilter;

class AuthorizationConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(AuthorizationConfiguration.class)
      .withBean(GlobalAuthorizationFilter.class, () -> mock(GlobalAuthorizationFilter.class));

  @Test
  void shouldBridgeConfiguredAuthorizationPropertiesIntoDomainPolicy() {
    contextRunner
        .withPropertyValues("app.security.authorization.mode=GLOBAL",
            "app.security.authorization.global-principal-identifiers=super-admin, platform-admin")
        .run(context -> {
          assertThat(context).hasSingleBean(AuthorizationProperties.class)
              .hasSingleBean(AuthorizationPolicy.class);
          assertThat(context.getBean(AuthorizationPolicy.class).mode())
              .isEqualTo(AuthorizationMode.GLOBAL);
          assertThat(context.getBean(AuthorizationPolicy.class).globalPrincipalIdentifiers())
              .containsExactlyInAnyOrder("super-admin", "platform-admin");
          assertThat(
              context.getBean("globalAuthorizationFilterRegistration", FilterRegistrationBean.class)
                  .isEnabled()).isFalse();
        });
  }

  @Test
  void shouldDefaultToGlobalModeWhenModeIsNotConfigured() {
    contextRunner.run(context -> assertThat(context.getBean(AuthorizationPolicy.class).mode())
        .isEqualTo(AuthorizationMode.GLOBAL));
  }
}
