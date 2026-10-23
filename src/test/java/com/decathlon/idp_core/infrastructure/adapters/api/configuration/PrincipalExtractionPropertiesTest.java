package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PrincipalExtractionPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(TestConfiguration.class);

  @Test
  void shouldBindHumanAndMachineExtractionSettings() {
    contextRunner
        .withPropertyValues(
            "app.security.authentication.principal-extraction.human-indicator-claims[0]=uuid",
            "app.security.authentication.principal-extraction.human.id-claim=uuid",
            "app.security.authentication.principal-extraction.machine.id-claim=client_id",
            "app.security.authentication.principal-extraction.machine.id-prefix=machine:")
        .run(context -> {
          assertThat(context).hasNotFailed();
          PrincipalExtractionProperties properties = context
              .getBean(PrincipalExtractionProperties.class);
          assertThat(properties.humanIndicatorClaims()).containsExactly("uuid");
          assertThat(properties.human().idClaim()).isEqualTo("uuid");
          assertThat(properties.machine().idClaim()).isEqualTo("client_id");
          assertThat(properties.machine().idPrefix()).isEqualTo("machine:");
        });
  }

  @Test
  void shouldUseDefaultsForMissingConfiguration() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      PrincipalExtractionProperties properties = context
          .getBean(PrincipalExtractionProperties.class);
      assertThat(properties.humanIndicatorClaims()).isEmpty();
      assertThat(properties.human().idClaim()).isEqualTo("sub");
      assertThat(properties.machine().idClaim()).isEqualTo("client_id");
      assertThat(properties.machine().idPrefix()).isEmpty();
    });
  }

  @EnableConfigurationProperties(PrincipalExtractionProperties.class)
  static class TestConfiguration {
  }
}
