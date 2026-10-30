package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import com.decathlon.idp_core.domain.model.enums.RelationTargetValidationMode;

class RelationValidationConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(RelationValidationConfiguration.class);

  @Test
  void shouldDefaultToStrictMode() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context.getBean(RelationValidationProperties.class).targetValidationMode())
          .isEqualTo(RelationTargetValidationMode.STRICT);
      assertThat(context.getBean(RelationTargetValidationMode.class))
          .isEqualTo(RelationTargetValidationMode.STRICT);
    });
  }

  @Test
  void shouldBindTolerantModeFromEnvironmentVariable() {
    contextRunner
        .withInitializer(context -> context.getEnvironment().getPropertySources()
            .addFirst(new SystemEnvironmentPropertySource("testEnvironment",
                Map.<String, Object>of("APP_RELATIONS_TARGET_VALIDATION_MODE", "TOLERANT"))))
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(RelationTargetValidationMode.class))
              .isEqualTo(RelationTargetValidationMode.TOLERANT);
        });
  }

  @Test
  void shouldRejectUnsupportedMode() {
    contextRunner.withPropertyValues("app.relations.target-validation-mode=LENIENT")
        .run(context -> assertThat(context).hasFailed());
  }
}
