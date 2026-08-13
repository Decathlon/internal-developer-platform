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
    String decoded = decodingProcessor.decode(payload, headers);
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

    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(payload, headers));

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

    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode("{}", headers));

    assertThat(exception.getMessage()).doesNotContain("\r").doesNotContain("\n");
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
}
