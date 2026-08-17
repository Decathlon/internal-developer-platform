package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
  @DisplayName("supports() -> True for BASIC_AUTH only")
  void shouldReturnTrueWhenTypeIsBasicAuth() {
    assertThat(validator.supports(WebhookSecurityType.BASIC_AUTH)).isTrue();
    assertThat(validator.supports(WebhookSecurityType.HMAC_SHA256)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.STATIC_TOKEN)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.JWT_BEARER)).isFalse();
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
