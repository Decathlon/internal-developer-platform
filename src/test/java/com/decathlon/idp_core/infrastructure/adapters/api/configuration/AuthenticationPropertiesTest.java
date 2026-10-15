package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthenticationPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(TestConfiguration.class);

  @Test
  void shouldBindConfiguredPrincipalIdentifierClaim() {
    contextRunner
        .withPropertyValues("app.security.authentication.principal-identifier-claim=person_uuid")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(AuthenticationProperties.class).principalIdentifierClaim())
              .isEqualTo("person_uuid");
        });
  }

  @Test
  void shouldDefaultPrincipalIdentifierClaimToSubject() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context.getBean(AuthenticationProperties.class).principalIdentifierClaim())
          .isEqualTo("sub");
    });
  }

  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class TestConfiguration {
  }
}
