package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

@DisplayName("Runtime Security: Basic Auth Validator")
class BasicAuthSecurityValidatorTest {

  private BasicAuthSecurityValidator validator;

  @BeforeEach
  void setUp() {
    validator = new BasicAuthSecurityValidator();
  }

  @Test
  @DisplayName("Should support BASIC_AUTH only")
  void shouldReturnTrueWhenTypeIsBasicAuth() {
    assertThat(validator.supports(WebhookSecurityType.BASIC_AUTH)).isTrue();
    assertThat(validator.supports(WebhookSecurityType.HMAC_SHA256)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.STATIC_TOKEN)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.JWT_BEARER)).isFalse();
  }

  @Nested
  @DisplayName("validateConfiguration — env variable existence")
  class ValidateConfiguration {

    @Test
    @DisplayName("Should throw when secret_alias env variable does not exist")
    void shouldThrowWhenSecretAliasEnvVarMissing() {
      Map<String, String> config = Map.of("username", "admin", "secret_alias",
          "UNSET_BASIC_AUTH_VAR");

      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("UNSET_BASIC_AUTH_VAR");
    }

    @Test
    @DisplayName("Should accept configuration when secret_alias env variable exists")
    void shouldAcceptWhenSecretAliasEnvVarExists() {
      String passwordEnv = "BASIC_AUTH_CONFIG_TEST";
      System.setProperty(passwordEnv, "secret");
      try {
        Map<String, String> config = Map.of("username", "admin", "secret_alias", passwordEnv);

        assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
      } finally {
        System.clearProperty(passwordEnv);
      }
    }
  }

  @Test
  @DisplayName("validateRequest() -> Resolves username and password from runtime environment")
  void shouldResolveUsernameAndPasswordAtRuntime() {
    String usernameEnv = "BASIC_AUTH_USERNAME_TEST";
    String passwordEnv = "BASIC_AUTH_PASSWORD_TEST";
    System.setProperty(usernameEnv, "admin");
    System.setProperty(passwordEnv, "secret");

    try {
      String credentials = Base64.getEncoder()
          .encodeToString("admin:secret".getBytes(StandardCharsets.UTF_8));
      var headers = Map.<String, Object>of("Authorization", "Basic " + credentials);
      var config = Map.of("username", usernameEnv, "secret_alias", passwordEnv);

      assertThatCode(() -> validator.validateRequest(headers, new byte[0], config))
          .doesNotThrowAnyException();
    } finally {
      System.clearProperty(usernameEnv);
      System.clearProperty(passwordEnv);
    }
  }

}
