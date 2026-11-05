package com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureAlgorithm;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureProperties;

class HttpMessageSignatureVerifierTest {

  static final String SECRET = "http-message-test-secret";
  static final String ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
  static final String CREATED = "1793789280";
  static final String EXPIRES = "1793789310";
  static final String COMPONENTS = "x-request-id (created) (expires)";

  // Generated independently with Python hmac/hashlib and the UTF-8 dummy secret.
  static Stream<Arguments> knownAnswers() {
    return Stream.of(
        Arguments.of(HttpMessageSignatureAlgorithm.HMAC_SHA256,
            "hDLDYJ+Y16dQPFUj3X6LXzvnp/T+wwwt0D9BCnpgUqA="),
        Arguments.of(HttpMessageSignatureAlgorithm.HMAC_SHA1, "/v/oIsZSMmwvzB6GHYuBKRy6mo0="),
        Arguments.of(HttpMessageSignatureAlgorithm.HMAC_SHA384,
            "79q26OAzlDSfqxC6R72xdv0h8w5fD/aEJnXfPbDzHkrWQHlBDq0Wt7uVk5FWVaDA"),
        Arguments.of(HttpMessageSignatureAlgorithm.HMAC_SHA512,
            "vim88k2M2jR/4tmqv8iBzDHw3Zf8dVvwNKkllZqq1AwsrFgCLsmFOAjDSwlPGK7B7OKVYbPvQsO7wtNeSgHU6Q=="));
  }

  @ParameterizedTest
  @MethodSource("knownAnswers")
  void verifiesIndependentKnownAnswers(HttpMessageSignatureAlgorithm algorithm, String digest) {
    String header = attributes(algorithm, COMPONENTS, CREATED, EXPIRES, digest);
    assertThat(verifier(algorithm, 30, 0, "1793789290", "x-request-id").verify(header, ID))
        .isEqualTo(HttpMessageSignatureResult.VALID);
  }

  @Test
  void parsesQuotedTimestampExampleWithoutClaimingItsHmacIsVerified() {
    String mock = attributes(HttpMessageSignatureAlgorithm.HMAC_SHA256, COMPONENTS, CREATED,
        EXPIRES, "hOb3Mz++JLh3BaBmXJkNT2Rf455fU+03ZjmF1MzPMms=");
    var parsed = new HttpMessageSignatureParser().parse(mock).orElseThrow();
    assertThat(parsed.createdText()).isEqualTo(CREATED);
    assertThat(parsed.expiresText()).isEqualTo(EXPIRES);
    assertThat(parsed.components()).containsExactly("x-request-id", "(created)", "(expires)");
    assertThat(parsed.toString()).doesNotContain("hOb3");
  }

  @ParameterizedTest
  @CsvSource({"1793789280,1793789310,1793789280,30,0,VALID",
      "1793789280,1793789310,1793789310,30,0,EXPIRED",
      "1793789280,1793789310,1793789310,30,1,VALID",
      "1793789280,1793789310,1793789311,30,1,EXPIRED",
      "1793789280,1793789310,1793789279,30,0,INVALID",
      "1793789280,1793789310,1793789279,30,1,VALID",
      "1793789280,1793789280,1793789280,30,0,INVALID",
      "1793789280,1793789279,1793789280,30,0,INVALID",
      "1793789280,1793789311,1793789290,30,5,INVALID",
      "1793789280,1793789311,1793789290,31,0,VALID",
      "1793789280,1793789280.500000001,1793789280.5,30,0,VALID",
      "1793789280,1793789310.1,1793789290,30,0,INVALID"})
  void enforcesTimeAndLifetime(String created, String expires, String now, long maximum, long skew,
      HttpMessageSignatureResult expected) throws Exception {
    String header = signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id", ID, created,
        expires);
    assertThat(
        verifier(HttpMessageSignatureAlgorithm.HMAC_SHA256, maximum, skew, now, "x-request-id")
            .verify(header, ID)).isEqualTo(expected);
  }

  @Test
  void largeConfiguredDurationsDoNotOverflow() throws Exception {
    String header = signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id", ID, CREATED,
        EXPIRES);
    assertThat(verifier(HttpMessageSignatureAlgorithm.HMAC_SHA256, Long.MAX_VALUE, Long.MAX_VALUE,
        "1793789290", "x-request-id").verify(header, ID))
            .isEqualTo(HttpMessageSignatureResult.VALID);
  }

  @ParameterizedTest
  @EnumSource(HttpMessageSignatureAlgorithm.class)
  void rejectsTamperingAndAlgorithmMismatch(HttpMessageSignatureAlgorithm algorithm)
      throws Exception {
    String header = signed(algorithm, "x-request-id", ID, CREATED, EXPIRES);
    var verifier = verifier(algorithm, 30, 0, "1793789290", "x-request-id");
    assertThat(verifier.verify(header, ID + "-changed"))
        .isEqualTo(HttpMessageSignatureResult.INVALID);
    assertThat(verifier.verify(header.replace(CREATED, "1793789281"), ID))
        .isEqualTo(HttpMessageSignatureResult.INVALID);
    assertThat(verifier.verify(header.replace(algorithm.wireName(), "hmac-md5"), ID))
        .isEqualTo(HttpMessageSignatureResult.INVALID);
    var other = algorithm == HttpMessageSignatureAlgorithm.HMAC_SHA256
        ? HttpMessageSignatureAlgorithm.HMAC_SHA1
        : HttpMessageSignatureAlgorithm.HMAC_SHA256;
    assertThat(verifier(other, 30, 0, "1793789290", "x-request-id").verify(header, ID))
        .isEqualTo(HttpMessageSignatureResult.INVALID);
  }

  @Test
  void acceptsCustomHeaderAndDeclaredComponentOrder() throws Exception {
    String components = "(expires) x-client-request-id (created)";
    String canonical = "(expires): " + EXPIRES + "\nx-client-request-id: " + ID + "\n(created): "
        + CREATED;
    String header = attributes(HttpMessageSignatureAlgorithm.HMAC_SHA256, components, CREATED,
        EXPIRES, digest(HttpMessageSignatureAlgorithm.HMAC_SHA256, canonical));
    assertThat(verifier(HttpMessageSignatureAlgorithm.HMAC_SHA256, 30, 0, "1793789290",
        "X-Client-Request-Id").verify(header, ID)).isEqualTo(HttpMessageSignatureResult.VALID);
  }

  @Test
  void acceptsUnquotedTimestampsAndOptionalWhitespace() throws Exception {
    String header = signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id", ID, CREATED,
        EXPIRES).replace("\"" + CREATED + "\"", CREATED).replace("\"" + EXPIRES + "\"", EXPIRES)
            .replace(",", ", \t");
    assertThat(
        verifier(HttpMessageSignatureAlgorithm.HMAC_SHA256, 30, 0, "1793789290", "x-request-id")
            .verify(header, ID)).isEqualTo(HttpMessageSignatureResult.VALID);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "Signature ", "signature=\"x\",", "signature=\"unterminated",
      "created=1,created=2", "garbage", "signature=\"a\\\"b\""})
  void rejectsMalformedAttributes(String header) {
    assertThat(
        verifier(HttpMessageSignatureAlgorithm.HMAC_SHA256, 30, 0, "1793789290", "x-request-id")
            .verify(header, ID)).isEqualTo(HttpMessageSignatureResult.MALFORMED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1", "1e9", "NaN", "1.1234567890", "999999999999999999999999", "\u0661",
      "\uFF11"})
  void rejectsInvalidTimestampRepresentations(String timestamp) {
    String header = attributes(HttpMessageSignatureAlgorithm.HMAC_SHA256, COMPONENTS, CREATED,
        timestamp, "hDLDYJ+Y16dQPFUj3X6LXzvnp/T+wwwt0D9BCnpgUqA=");
    assertThat(new HttpMessageSignatureParser().parse(header)).isEmpty();
    String invalidCreated = attributes(HttpMessageSignatureAlgorithm.HMAC_SHA256, COMPONENTS,
        timestamp, EXPIRES, "hDLDYJ+Y16dQPFUj3X6LXzvnp/T+wwwt0D9BCnpgUqA=");
    assertThat(new HttpMessageSignatureParser().parse(invalidCreated)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"x-request-id", "x-request-id (created) (created)",
      "x-request-id (created) (expires) host"})
  void rejectsMissingDuplicateOrExtraComponents(String components) {
    String header = attributes(HttpMessageSignatureAlgorithm.HMAC_SHA256, components, CREATED,
        EXPIRES, "hDLDYJ+Y16dQPFUj3X6LXzvnp/T+wwwt0D9BCnpgUqA=");
    assertThat(
        verifier(HttpMessageSignatureAlgorithm.HMAC_SHA256, 30, 0, "1793789290", "x-request-id")
            .verify(header, ID)).isEqualTo(HttpMessageSignatureResult.INVALID);
  }

  @Test
  void rejectsAmbiguousAttributesAndInvalidDigest() throws Exception {
    String header = signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id", ID, CREATED,
        EXPIRES);
    var verifier = verifier(HttpMessageSignatureAlgorithm.HMAC_SHA256, 30, 0, "1793789290",
        "x-request-id");
    assertThat(verifier.verify(header + ",created=1", ID))
        .isEqualTo(HttpMessageSignatureResult.MALFORMED);
    assertThat(
        verifier.verify(header.replace("hDLDYJ+Y16dQPFUj3X6LXzvnp/T+wwwt0D9BCnpgUqA=", "!"), ID))
            .isEqualTo(HttpMessageSignatureResult.MALFORMED);
    assertThat(
        verifier.verify(header.replace("hDLDYJ+Y16dQPFUj3X6LXzvnp/T+wwwt0D9BCnpgUqA=", "YQ=="), ID))
            .isEqualTo(HttpMessageSignatureResult.INVALID);
    assertThat(new HttpMessageSignatureParser().parse("x".repeat(8193))).isEmpty();
  }

  static HttpMessageSignatureVerifier verifier(HttpMessageSignatureAlgorithm algorithm,
      long lifetime, long skew, String now, String idHeader) {
    var seconds = new java.math.BigDecimal(now);
    var instant = Instant.ofEpochSecond(seconds.longValue(),
        seconds.remainder(java.math.BigDecimal.ONE).movePointRight(9).longValueExact());
    return new HttpMessageSignatureVerifier(new HttpMessageSignatureProperties(true, SECRET,
        idHeader, algorithm, lifetime, skew, List.of()), Clock.fixed(instant, ZoneOffset.UTC));
  }

  static String signed(HttpMessageSignatureAlgorithm algorithm, String idHeader, String id,
      String created, String expires) throws Exception {
    String canonical = idHeader.toLowerCase(java.util.Locale.ROOT) + ": " + id + "\n(created): "
        + created + "\n(expires): " + expires;
    return attributes(algorithm,
        idHeader.toLowerCase(java.util.Locale.ROOT) + " (created) (expires)", created, expires,
        digest(algorithm, canonical));
  }

  static String digest(HttpMessageSignatureAlgorithm algorithm, String canonical) throws Exception {
    var mac = Mac.getInstance(algorithm.jcaName());
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), algorithm.jcaName()));
    return Base64.getEncoder()
        .encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
  }

  static String attributes(HttpMessageSignatureAlgorithm algorithm, String components,
      String created, String expires, String digest) {
    return "keyId=\"client\",algorithm=\"" + algorithm.wireName() + "\",headers=\"" + components
        + "\",created=\"" + created + "\",expires=\"" + expires + "\",signature=\"" + digest + "\"";
  }
}
