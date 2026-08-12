package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.decoder.DecodingProcessor;

@DisplayName("DecodingProcessor unit tests")
class DecodingProcessorTest {

  private final DecodingProcessor decodingProcessor = new DecodingProcessor();

  @ParameterizedTest(name = "[{index}]")
  @MethodSource("passThroughCases")
  @DisplayName("Returns raw payload for pass-through encodings")
  void decode_returnsRawPayload_forPassThroughEncodings(String payload, Map<String, Object> headers)
      throws Exception {
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
  void decode_returnsEmptyString_whenPayloadIsNullAndNoEncoding() throws Exception {
    String decoded = decodingProcessor.decode(null, Map.of());

    assertEquals("", decoded);
  }

  @Test
  @DisplayName("Throws ZipException when payload is null but gzip encoding is declared")
  void decode_throwsZipException_whenPayloadIsNullAndGzipEncoding() {
    assertThrows(ZipException.class,
        () -> decodingProcessor.decode(null, Map.of("Content-Encoding", "gzip")));
  }

  private static Stream<Arguments> passThroughCases() {
    return Stream.of(Arguments.of("{\"event\":\"plain\"}", Map.of()),
        Arguments.of("{\"event\":\"identity\"}", Map.of("Content-Encoding", "identity")),
        Arguments.of("{\"event\":\"base64\"}", Map.of("Content-Encoding", "base64")));
  }

  private static Stream<Arguments> gzipDecodingCases() {
    return Stream.of(Arguments.of("{\"event\":\"gzip\"}", Map.of("Content-Encoding", "gzip")),
        Arguments.of("{\"event\":\"stacked\"}", Map.of("Content-Encoding", "gzip, identity")),
        Arguments.of("{\"event\":\"gzip\"}", Map.of("content-encoding", "gzip")));
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
