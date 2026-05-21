package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HmacSignatureValidator Tests")
class HmacSignatureValidatorTest {

    private static final String SECRET = "super-secret-key";
    private static final String BODY = "{\"action\":\"closed\"}";

    private HmacSignatureValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HmacSignatureValidator();
    }

    @Test
    @DisplayName("Should compute a valid hex-encoded HMAC-SHA256 digest")
    void shouldComputeValidHmacSha256() throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var expected = HexFormat.of().formatHex(mac.doFinal(BODY.getBytes(StandardCharsets.UTF_8)));

        var actual = validator.computeHexSha256(BODY.getBytes(StandardCharsets.UTF_8), SECRET);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should produce different digests for different payloads")
    void shouldProduceDifferentDigestsForDifferentPayloads() {
        var digest1 = validator.computeHexSha256("payload1".getBytes(StandardCharsets.UTF_8), SECRET);
        var digest2 = validator.computeHexSha256("payload2".getBytes(StandardCharsets.UTF_8), SECRET);

        assertThat(digest1).isNotEqualTo(digest2);
    }

    @Test
    @DisplayName("Should produce different digests for different secrets")
    void shouldProduceDifferentDigestsForDifferentSecrets() {
        assertThat(validator.computeHexSha256(BODY.getBytes(StandardCharsets.UTF_8), "secret-a")).isNotEqualTo(validator.computeHexSha256(BODY.getBytes(StandardCharsets.UTF_8), "secret-b"));
    }

    @Test
    @DisplayName("Should throw WebhookAuthenticationException on internal crypto error")
    void shouldThrowOnCryptoError() {
        assertThat(validator.computeHexSha256(new byte[0], SECRET)).isNotBlank();
    }

    @Test
    @DisplayName("Should throw WebhookAuthenticationException when secret is empty string")
    void shouldThrowWhenSecretIsEmpty() {
        assertThatThrownBy(() -> validator.computeHexSha256(BODY.getBytes(StandardCharsets.UTF_8), ""))
                .isInstanceOf(WebhookAuthenticationException.class)
                .hasMessageContaining("Unable to compute HMAC signature");
    }
}
