package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DecodingProcessor unit tests")
class DecodingProcessorTest {

  private final DecodingProcessor decodingProcessor = new DecodingProcessor();

  @Test
  @DisplayName("Returns raw payload when Content-Encoding header is missing")
  void decode_returnsRawPayload_whenEncodingHeaderMissing() throws Exception {
    String payload = "{\"event\":\"plain\"}";

    String decoded = decodingProcessor.decode(payload, Map.of());

    assertEquals(payload, decoded);
  }

  @Test
  @DisplayName("Returns raw payload when Content-Encoding is identity")
  void decode_returnsRawPayload_whenIdentityEncoding() throws Exception {
    String payload = "{\"event\":\"identity\"}";

    String decoded = decodingProcessor.decode(payload, Map.of("Content-Encoding", "identity"));

    assertEquals(payload, decoded);
  }

  @Test
  @DisplayName("Decodes gzip payload")
  void decode_decodesGzipPayload() throws Exception {
    String rawPayload = "{\"event\":\"gzip\"}";
    byte[] gzipPayload = gzip(rawPayload);

    String decoded = decodingProcessor.decode(gzipPayload, Map.of("Content-Encoding", "gzip"));

    assertEquals(rawPayload, decoded);
  }

  @Test
  @DisplayName("Decodes stacked encodings in reverse order")
  void decode_decodesStackedEncodings() throws Exception {
    String rawPayload = "{\"event\":\"stacked\"}";
    byte[] gzipPayload = gzip(rawPayload);

    String decoded = decodingProcessor.decode(gzipPayload,
        Map.of("Content-Encoding", "gzip, identity"));

    assertEquals(rawPayload, decoded);
  }

  @Test
  @DisplayName("Returns raw payload for unsupported encoding")
  void decode_returnsRawPayload_whenEncodingUnsupported() throws Exception {
    String payload = "{\"event\":\"base64\"}";

    String decoded = decodingProcessor.decode(payload, Map.of("Content-Encoding", "base64"));

    assertEquals(payload, decoded);
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
