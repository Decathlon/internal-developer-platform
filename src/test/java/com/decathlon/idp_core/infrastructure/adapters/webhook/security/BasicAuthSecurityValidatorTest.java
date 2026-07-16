package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;

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

}
