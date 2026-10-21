package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthenticationPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(TestConfiguration.class);

  @Test
  void shouldBindAuthenticationProperties() {
    contextRunner
        .withPropertyValues("app.security.authentication.user-claim-mappings.name=display_name",
            "app.security.authentication.jit-provisioning-excluded-paths[0]=/health")
        .run(context -> {
          assertThat(context).hasNotFailed();
          AuthenticationProperties properties = context.getBean(AuthenticationProperties.class);
          assertThat(properties.userClaimMappings()).containsEntry("name", "display_name");
          assertThat(properties.jitProvisioningExcludedPaths()).containsExactly("/health");
        });
  }

  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class TestConfiguration {
  }
}
