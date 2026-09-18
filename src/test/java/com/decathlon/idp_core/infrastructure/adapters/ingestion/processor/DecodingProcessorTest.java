package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookDecodingException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.decoder.DecodingProcessor;

@DisplayName("DecodingProcessor unit tests")
class DecodingProcessorTest {

  private static final Map<String, Object> GZIP_HEADERS = Map.of("Content-Encoding", "gzip");
  private static final byte[] CORRUPTED_GZIP_PAYLOAD = "not-a-gzip-stream"
      .getBytes(StandardCharsets.UTF_8);

  // Default processor with the production default limit (10 MB)
  private final DecodingProcessor decodingProcessor = new DecodingProcessor(
      IngestionConstants.MAX_DECOMPRESSED_BYTES);

  @ParameterizedTest(name = "[{index}]")
  @MethodSource("passThroughCases")
  @DisplayName("Returns raw payload for pass-through encodings")
  void decode_returnsRawPayload_forPassThroughEncodings(String payload,
      Map<String, Object> headers) {
    String decoded = decodingProcessor.decode(payload.getBytes(StandardCharsets.UTF_8), headers);
    assertEquals(payload, decoded);
  }

  @ParameterizedTest(name = "[{index}]")
  @MethodSource("gzipDecodingCases")
  @DisplayName("Decodes gzip payload for supported header variants")
  void decode_decodesGzipPayload_forSupportedHeaderVariants(String rawPayload,
      Map<String, Object> headers) throws Exception {
    byte[] gzipPayload = gzip(rawPayload);

    String decoded = decodingProcessor.decode(gzipPayload, headers);

    assertEquals(rawPayload, decoded);
  }

  @Test
  @DisplayName("Returns empty string when payload is null and no encoding is provided")
  void decode_returnsEmptyString_whenPayloadIsNullAndNoEncoding() {
    String decoded = decodingProcessor.decode(null, Map.of());
    assertEquals("", decoded);
  }

  @Test
  @DisplayName("Throws WebhookDecodingException when payload is null but gzip encoding is declared")
  void decode_throwsWebhookDecodingException_whenPayloadIsNullAndGzipEncoding() {
    assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(null, GZIP_HEADERS));
  }

  @Test
  @DisplayName("Throws WebhookDecodingException when gzip payload bytes are corrupted")
  void decode_throwsWebhookDecodingException_whenGzipPayloadIsCorrupted() {
    assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(CORRUPTED_GZIP_PAYLOAD, GZIP_HEADERS));
  }

  @Test
  @DisplayName("Carries detailed message for null payload when gzip encoding is declared")
  void decode_carriesDetailedMessage_whenPayloadIsNullAndGzipEncoding() {
    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(null, GZIP_HEADERS));

    assertEquals("Empty payload cannot be decoded as gzip", exception.getMessage());
  }

  @ParameterizedTest(name = "[{index}] encoding={0}")
  @MethodSource("unsupportedEncodingCases")
  @DisplayName("Throws WebhookDecodingException for unsupported Content-Encoding instead of silent pass-through")
  void decode_throwsWebhookDecodingException_whenContentEncodingIsUnsupported(
      String contentEncoding) {
    Map<String, Object> headers = Map.of("Content-Encoding", contentEncoding);
    String payload = "{\"status\":\"OK\"}";
    byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
    var failureCase = new DecodeFailureCase(decodingProcessor, payloadBytes, headers);

    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        failureCase::execute);

    assertThat(exception.getMessage()).contains("Unsupported Content-Encoding: '")
        .contains(contentEncoding);
  }

  @Test
  @DisplayName("Throws WebhookDecodingException when decompressed payload exceeds the configured size limit (Zip Bomb protection)")
  void decode_throwsWebhookDecodingException_whenDecompressedSizeExceedsLimit() throws Exception {
    // Processor with a very small limit of 10 bytes to trigger the zip bomb guard
    DecodingProcessor restrictedProcessor = new DecodingProcessor(10L);
    byte[] gzipPayload = gzip("This payload will exceed the 10 bytes decompression limit");

    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> restrictedProcessor.decode(gzipPayload, GZIP_HEADERS));

    assertThat(exception.getMessage()).contains("exceeds maximum allowed size of 10 bytes");
  }

  @Test
  @DisplayName("Strips CRLF injection characters from Content-Encoding header in exception messages")
  void decode_sanitizesHeaderValue_whenContentEncodingContainsCrLfCharacters() {
    Map<String, Object> headers = Map.of("Content-Encoding", "br\r\nX-Injected: evil");
    byte[] payloadBytes = "{}".getBytes(StandardCharsets.UTF_8);
    var failureCase = new DecodeFailureCase(decodingProcessor, payloadBytes, headers);

    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        failureCase::execute);

    assertThat(exception.getMessage()).doesNotContain("\r").doesNotContain("\n");
  }

  @Test
  @DisplayName("Rejects oversized default-object payloads before calling toString")
  void decode_throwsWebhookDecodingException_whenDefaultPayloadToStringIsTooLarge() {
    Object oversizedPayload = new Object() {
      @Override
      public String toString() {
        return "X".repeat((int) IngestionConstants.MAX_DECOMPRESSED_BYTES + 1);
      }
    };

    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(oversizedPayload, Map.of()));

    assertThat(exception.getMessage()).contains("Input payload string length");
  }

  @Test
  @DisplayName("Accepts String payload and returns it as-is when no encoding is provided")
  void decode_acceptsStringPayload_whenNoEncodingIsProvided() {
    String payload = "{\"event\":\"string-input\"}";
    String decoded = decodingProcessor.decode(payload, Map.of());
    assertEquals(payload, decoded);
  }

  @Test
  @DisplayName("Converts byte[] to String correctly when no encoding is provided")
  void decode_convertsByteArrayToString_whenNoEncodingIsProvided() {
    byte[] payload = "{\"event\":\"bytes\"}".getBytes(StandardCharsets.UTF_8);
    String decoded = decodingProcessor.decode(payload, Map.of());
    assertEquals("{\"event\":\"bytes\"}", decoded);
  }

  @Test
  @DisplayName("Rejects oversized String payload before encoding processing")
  void decode_throwsWebhookDecodingException_whenStringPayloadExceedsMaxSize() {
    // String that exceeds MAX_INPUT_PAYLOAD_BYTES (10 MB)
    String oversizedPayload = "X".repeat((int) IngestionConstants.MAX_DECOMPRESSED_BYTES + 1);
    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(oversizedPayload, Map.of()));

    assertThat(exception.getMessage()).contains("Input payload string length")
        .contains("exceeds maximum allowed size");
  }

  @Test
  @DisplayName("Rejects oversized byte[] payload before encoding processing")
  void decode_throwsWebhookDecodingException_whenByteArrayPayloadExceedsMaxSize() {
    byte[] oversizedPayload = new byte[(int) IngestionConstants.MAX_DECOMPRESSED_BYTES + 1];
    Map<String, Object> headers = Map.of();

    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(oversizedPayload, headers));

    assertThat(exception.getMessage()).contains("Input payload size")
        .contains("exceeds maximum allowed size");
  }

  @Test
  @DisplayName("Handles String payload with gzip encoding")
  void decode_acceptsStringPayloadWithGzipEncoding() throws Exception {
    String payload = "{\"event\":\"gzip-string\"}";
    byte[] gzipPayload = gzip(payload);
    // DecodingProcessor converts String→byte[] internally
    String decoded = decodingProcessor.decode(gzipPayload, GZIP_HEADERS);
    assertEquals(payload, decoded);
  }

  @Test
  @DisplayName("Accepts null headers and treats as no encoding")
  void decode_acceptsNullHeaders_andTreatsAsNoEncoding() {
    String payload = "{\"event\":\"null-headers\"}";
    String decoded = decodingProcessor.decode(payload, null);
    assertEquals(payload, decoded);
  }

  @Test
  @DisplayName("Handles multiple encoding separators correctly (gzip, identity)")
  void decode_handlesStackedEncodings_gzipThenIdentity() throws Exception {
    String payload = "{\"event\":\"stacked-encoding\"}";
    byte[] gzipPayload = gzip(payload);
    // Encodings are processed in reverse order: identity → gzip
    String decoded = decodingProcessor.decode(gzipPayload,
        Map.of("Content-Encoding", "gzip, identity"));
    assertEquals(payload, decoded);
  }

  @Test
  @DisplayName("Rejects payloads with only whitespace in Content-Encoding header")
  void decode_treatsWhitespaceOnlyContentEncodingAsEmpty() {
    byte[] payload = "{\"event\":\"test\"}".getBytes(StandardCharsets.UTF_8);
    String decoded = decodingProcessor.decode(payload, Map.of("Content-Encoding", "   "));
    assertEquals("{\"event\":\"test\"}", decoded);
  }

  @Test
  @DisplayName("Handles case-insensitive Content-Encoding header name")
  void decode_handlesCaseInsensitiveContentEncodingHeader() {
    byte[] payload = "{\"event\":\"test\"}".getBytes(StandardCharsets.UTF_8);
    String decoded = decodingProcessor.decode(payload, Map.of("content-ENCODING", "identity"));
    assertEquals("{\"event\":\"test\"}", decoded);
  }

  @Test
  @DisplayName("Rejects empty String payload when gzip encoding is declared")
  void decode_throwsWebhookDecodingException_whenEmptyStringPayloadWithGzipEncoding() {
    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode("", GZIP_HEADERS));

    assertEquals("Empty payload cannot be decoded as gzip", exception.getMessage());
  }

  @Test
  @DisplayName("Rejects empty byte[] payload when gzip encoding is declared")
  void decode_throwsWebhookDecodingException_whenEmptyByteArrayPayloadWithGzipEncoding() {
    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(new byte[0], GZIP_HEADERS));

    assertEquals("Empty payload cannot be decoded as gzip", exception.getMessage());
  }

  @Test
  @DisplayName("Sanitizes long header values in exception messages")
  void decode_truncatesLongSanitizedHeaderValuesInExceptions() {
    // Create a long Content-Encoding header that will be truncated
    String longEncoding = "unsupported" + "x".repeat(200);
    Map<String, Object> headers = Map.of("Content-Encoding", longEncoding);
    byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(payload, headers));

    assertThat(exception.getMessage()).contains("Unsupported Content-Encoding: '")
        .doesNotContain("x".repeat(200)); // Verify long value was truncated
  }

  private static Stream<Arguments> passThroughCases() {
    return Stream.of(Arguments.of("{\"event\":\"plain\"}", Map.of()),
        Arguments.of("{\"event\":\"identity\"}", Map.of("Content-Encoding", "identity")));
  }

  private static Stream<Arguments> gzipDecodingCases() {
    return Stream.of(Arguments.of("{\"event\":\"gzip\"}", Map.of("Content-Encoding", "gzip")),
        Arguments.of("{\"event\":\"stacked\"}", Map.of("Content-Encoding", "gzip, identity")),
        Arguments.of("{\"event\":\"gzip\"}", Map.of("content-encoding", "gzip")));
  }

  private static Stream<Arguments> unsupportedEncodingCases() {
    return Stream.of(Arguments.of("br"), Arguments.of("deflate"));
  }

  private byte[] gzip(String payload) throws Exception {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutput = new GZIPOutputStream(output)) {
      gzipOutput.write(payload.getBytes(StandardCharsets.UTF_8));
      gzipOutput.finish();
      return output.toByteArray();
    }
  }

  private record DecodeFailureCase(DecodingProcessor decodingProcessor, byte[] payload,
      Map<String, Object> headers) {

    void execute() {
      decodingProcessor.decode(payload, headers);
    }
  }
}
