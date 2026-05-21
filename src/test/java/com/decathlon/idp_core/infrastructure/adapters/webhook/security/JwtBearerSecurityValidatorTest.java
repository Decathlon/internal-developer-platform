package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("JwtBearerSecurityValidator Tests")
@ExtendWith(MockitoExtension.class)
class JwtBearerSecurityValidatorTest {

    @Mock
    private WebhookJwtDecoderProvider jwtDecoderProvider;

    @Mock
    private JwtDecoder jwtDecoder;

    private JwtBearerSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JwtBearerSecurityValidator(jwtDecoderProvider);
    }

    @Test
    @DisplayName("Should support JWT_BEARER (case-insensitive)")
    void shouldSupportJwtBearer() {
        assertThat(validator.supports("JWT_BEARER")).isTrue();
        assertThat(validator.supports("jwt_bearer")).isTrue();
        assertThat(validator.supports("STATIC_TOKEN")).isFalse();
    }

    @Nested
    @DisplayName("validateRequest — missing or invalid Authorization header")
    class MissingOrInvalidAuthorizationHeader {

        @Test
        @DisplayName("Should throw when Authorization header is absent")
        void shouldThrowWhenAuthorizationHeaderMissing() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.JWT_BEARER,
                    Map.of("jwks_uri", "https://example.com/.well-known/jwks.json"));
            byte[] rawBody = new byte[0];
            Map<String,String> headers = Map.<String, String>of();

            assertThatThrownBy(() -> validator.validateRequest(security, headers, rawBody))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("Missing Authorization Bearer header");
        }

        @Test
        @DisplayName("Should throw when Authorization header does not start with 'Bearer '")
        void shouldThrowWhenAuthorizationNotBearer() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.JWT_BEARER,
                    Map.of("jwks_uri", "https://example.com/.well-known/jwks.json"));
            Map<String,String> headers = Map.of("Authorization", "Basic dXNlcjpwYXNz");

            assertThatThrownBy(() -> validator.validateRequest(security, headers, new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("Missing Authorization Bearer header");
        }

        @Test
        @DisplayName("Should throw when bearer token is blank after 'Bearer ' prefix")
        void shouldThrowWhenBearerTokenIsBlank() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.JWT_BEARER,
                    Map.of("jwks_uri", "https://example.com/.well-known/jwks.json"));
            Map<String,String> headers = Map.of("Authorization", "Bearer   ");

            assertThatThrownBy(() -> validator.validateRequest(security, headers, new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("Missing bearer token");
        }
    }

    @Nested
    @DisplayName("validateRequest — missing config keys")
    class MissingConfigKeys {

        @Test
        @DisplayName("Should throw when jwks_uri is missing from config")
        void shouldThrowWhenJwksUriMissing() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.JWT_BEARER, Map.of("other_key", "value"));
            byte[] rawBody = new byte[0];
            Map<String,String> headers = Map.<String, String>of();

            assertThatThrownBy(() -> validator.validateRequest(security, headers, rawBody))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("jwks_uri");
        }
    }

    @Nested
    @DisplayName("validateRequest — JWT signature verification")
    class JwtSignatureVerification {

        @Test
        @DisplayName("Should decode JWT with configured jwks_uri")
        void shouldDecodeJwtWithConfiguredJwksUri() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.JWT_BEARER,
                    Map.of("jwks_uri", "https://example.com/.well-known/jwks.json"));
            String token = "eyJhbGciOiJSUzI1NiJ9.payload.signature";
            Map<String,String> headers = Map.of("Authorization", "Bearer " + token);

            when(jwtDecoderProvider.get("https://example.com/.well-known/jwks.json")).thenReturn(jwtDecoder);
            when(jwtDecoder.decode(token)).thenReturn(
                    Jwt.withTokenValue(token)
                            .header("alg", "RS256")
                            .claim("sub", "webhook-caller")
                            .build());

            validator.validateRequest(security, headers, new byte[0]);

            verify(jwtDecoderProvider).get("https://example.com/.well-known/jwks.json");
            verify(jwtDecoder).decode(token);
        }

        @Test
        @DisplayName("Should throw when JWT signature validation fails")
        void shouldThrowWhenJwtSignatureValidationFails() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.JWT_BEARER,
                    Map.of("jwks_uri", "https://example.com/.well-known/jwks.json"));
            String token = "invalid-token";
            Map<String,String> headers = Map.of("Authorization", "Bearer " + token);

            when(jwtDecoderProvider.get("https://example.com/.well-known/jwks.json")).thenReturn(jwtDecoder);
            when(jwtDecoder.decode(token)).thenThrow(new JwtException("bad signature"));

            assertThatThrownBy(() -> validator.validateRequest(security, headers, new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("Invalid JWT bearer token");
        }
    }
}
