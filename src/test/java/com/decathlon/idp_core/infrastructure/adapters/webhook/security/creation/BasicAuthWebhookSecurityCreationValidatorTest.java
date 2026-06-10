package com.decathlon.idp_core.infrastructure.adapters.webhook.security.creation;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.infrastructure.adapters.webhook.security.BasicAuthSecurityValidator;

@DisplayName("Security Validator: Basic Auth Creation")
class BasicAuthWebhookSecurityCreationValidatorTest {

  private BasicAuthSecurityValidator validator;

  @BeforeEach
  void setUp() {
    validator = new BasicAuthSecurityValidator();
  }

  @Test
  @DisplayName("supports() -> True for 'BASIC_AUTH' (case-insensitive), False for others")
  void shouldReturnTrueWhenTypeIsBasicAuth() {
    assertThat(validator.supports("BASIC_AUTH")).isTrue();
    assertThat(validator.supports("basic_auth")).isTrue();
    assertThat(validator.supports("HMAC_SHA256")).isFalse();
  }

  @Nested
  @DisplayName("Valid Configurations")
  class ValidConfigurations {

    @Test
    @DisplayName("validateConfiguration() -> Passes when both username and UPPER_SNAKE_CASE secret_alias are present")
    void shouldPassWhenAllRequiredFieldsAreValid() {
      Map<String, String> config = Map.of("username", "webhook-user", "secret_alias",
          "TEST_CREDENTIAL_ALIAS");
      assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateConfiguration() -> Passes when secret key is provided as camelCase 'secretAlias'")
    void shouldPassWhenSecretKeyIsCamelCase() {
      Map<String, String> config = Map.of("username", "webhook-user", "secretAlias",
          "TEST_CREDENTIAL_ALIAS");
      assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Invalid Configurations (Missing or Malformed Fields)")
  class InvalidConfigurations {

    @Test
    @DisplayName("validateConfiguration() -> Throws exception when 'username' is completely missing")
    void shouldThrowExceptionWhenUsernameIsMissing() {
      Map<String, String> config = Map.of("secret_alias", "TEST_CREDENTIAL_ALIAS");
      assertThatThrownBy(() -> validator.validateConfiguration(config))
          .isInstanceOf(WebhookSecurityConfigurationException.class)
          .hasMessageContaining("username");
    }

    @Test
    @DisplayName("validateConfiguration() -> Throws exception when 'secret_alias' is completely missing")
    void shouldThrowExceptionWhenSecretAliasIsMissing() {
      Map<String, String> config = Map.of("username", "webhook-user");
      assertThatThrownBy(() -> validator.validateConfiguration(config))
          .isInstanceOf(WebhookSecurityConfigurationException.class)
          .hasMessageContaining("secret_alias");
    }

    @Test
    @DisplayName("validateConfiguration() -> Throws exception when 'secret_alias' contains lowercase letters")
    void shouldThrowExceptionWhenSecretAliasIsNotUpperSnakeCase() {
      Map<String, String> config = Map.of("username", "webhook-user", "secret_alias",
          "not_upper_snake");
      assertThatThrownBy(() -> validator.validateConfiguration(config))
          .isInstanceOf(WebhookSecurityConfigurationException.class)
          .hasMessageContaining("UPPER_SNAKE_CASE");
    }
  }
}
