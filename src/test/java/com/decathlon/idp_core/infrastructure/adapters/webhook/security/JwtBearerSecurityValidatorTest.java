package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("JwtBearerSecurityValidator Tests")
@ExtendWith(MockitoExtension.class)
class JwtBearerSecurityValidatorTest {

  private final JwtBearerSecurityValidator validator = new JwtBearerSecurityValidator();

  @Test
  @DisplayName("Should support JWT_BEARER (case-insensitive)")
  void shouldSupportJwtBearer() {
    assertThat(validator.supports("JWT_BEARER")).isTrue();
    assertThat(validator.supports("jwt_bearer")).isTrue();
    assertThat(validator.supports("STATIC_TOKEN")).isFalse();
  }

  @Nested
  @DisplayName("validateConfiguration — missing config keys")
  class MissingConfigKeys {

    @Test
    @DisplayName("Should throw when jwks_uri is missing from config")
    void shouldThrowWhenJwksUriMissing() {
      Map<String, String> config = Map.of("other_key", "value");

      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("jwks_uri");
    }
  }
}
