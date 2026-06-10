package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Runtime Security: Basic Auth Validator")
class BasicAuthSecurityValidatorTest {

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

}
