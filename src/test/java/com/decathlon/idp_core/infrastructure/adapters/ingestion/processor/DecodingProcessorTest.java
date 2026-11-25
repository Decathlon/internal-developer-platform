package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.ATTRIBUTE_CONNECTOR;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.ATTRIBUTE_ENCODING;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.ATTRIBUTE_REASON;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.PAYLOAD_DECODED_TOTAL;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.PAYLOAD_DECODE_ERRORS_TOTAL;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.PAYLOAD_SIZE_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookDecodingException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.decoder.DecodingProcessor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

@DisplayName("DecodingProcessor unit tests")
class DecodingProcessorTest {

  private static final Map<String, Object> GZIP_HEADERS = Map.of("Content-Encoding", "gzip");
  private static final String TEST_CONNECTOR = "test-connector";
  private static final byte[] CORRUPTED_GZIP_PAYLOAD = "not-a-gzip-stream"
      .getBytes(StandardCharsets.UTF_8);

  private InMemoryMetricReader metricReader;
  private SdkMeterProvider meterProvider;
  private DecodingProcessor decodingProcessor;

  @BeforeEach
  void setUp() {
    metricReader = InMemoryMetricReader.create();
    meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
    Meter meter = meterProvider.get("com.decathlon.idp_core.tests");

    // Default processor with the production default limit (10 MB)
    decodingProcessor = new DecodingProcessor(IngestionConstants.MAX_DECOMPRESSED_BYTES, meter);
  }

  @AfterEach
  void tearDown() {
    if (meterProvider != null) {
      meterProvider.close();
    }
  }

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
  @DisplayName("Records decoded counter, payload size and connector attribution on gzip success")
  void decode_recordsMetrics_whenGzipDecodeSucceeds() throws Exception {
    String payload = "{\"event\":\"metrics\"}";
    byte[] gzipPayload = gzip(payload);

    String decoded = decodingProcessor.decode(gzipPayload, GZIP_HEADERS, TEST_CONNECTOR);

    assertEquals(payload, decoded);
    assertEquals(1L, findLongCounterValue(PAYLOAD_DECODED_TOTAL,
        Map.of(ATTRIBUTE_CONNECTOR, TEST_CONNECTOR, ATTRIBUTE_ENCODING, "gzip")));
    assertEquals(1L, findHistogramCount(Map.of(ATTRIBUTE_CONNECTOR, TEST_CONNECTOR)));
    assertEquals(payload.getBytes(StandardCharsets.UTF_8).length,
        (long) findHistogramSum(Map.of(ATTRIBUTE_CONNECTOR, TEST_CONNECTOR)));
  }

  @Test
  @DisplayName("Records decode error reason and connector attribution for unsupported encoding")
  void decode_recordsErrorMetrics_whenEncodingUnsupported() {
    Map<String, Object> headers = Map.of("Content-Encoding", "br");
    var decodeFailureCase = new DecodeFailureWithConnectorCase(decodingProcessor,
        "{}".getBytes(StandardCharsets.UTF_8), headers, TEST_CONNECTOR);

    assertThrows(WebhookDecodingException.class, decodeFailureCase::execute);

    assertEquals(1L, findLongCounterValue(PAYLOAD_DECODE_ERRORS_TOTAL,
        Map.of(ATTRIBUTE_CONNECTOR, TEST_CONNECTOR, ATTRIBUTE_REASON, "unsupported_encoding")));
  }

  @Test
  @DisplayName("Records decode error reason and connector attribution for corrupted gzip payload")
  void decode_recordsErrorMetrics_whenGzipPayloadIsCorrupted() {
    assertThrows(WebhookDecodingException.class,
        () -> decodingProcessor.decode(CORRUPTED_GZIP_PAYLOAD, GZIP_HEADERS, TEST_CONNECTOR));

    assertEquals(1L, findLongCounterValue(PAYLOAD_DECODE_ERRORS_TOTAL,
        Map.of(ATTRIBUTE_CONNECTOR, TEST_CONNECTOR, ATTRIBUTE_REASON, "corrupted_gzip_payload")));
  }

  @Test
  @DisplayName("Records decode error reason for payload too large before decoding")
  void decode_recordsErrorMetrics_whenPayloadTooLarge() {
    byte[] oversizedPayload = new byte[(int) IngestionConstants.MAX_DECOMPRESSED_BYTES + 1];
    var decodeFailureCase = new DecodeFailureWithConnectorCase(decodingProcessor, oversizedPayload,
        Map.of(), TEST_CONNECTOR);

    assertThrows(WebhookDecodingException.class, decodeFailureCase::execute);

    assertEquals(1L, findLongCounterValue(PAYLOAD_DECODE_ERRORS_TOTAL,
        Map.of(ATTRIBUTE_CONNECTOR, TEST_CONNECTOR, ATTRIBUTE_REASON, "payload_too_large")));
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
    Meter meter = meterProvider.get("com.decathlon.idp_core.tests.restricted");
    DecodingProcessor restrictedProcessor = new DecodingProcessor(10L, meter);
    byte[] gzipPayload = gzip("This payload will exceed the 10 bytes decompression limit");
    var failureCase = new DecodeFailureCase(restrictedProcessor, gzipPayload, GZIP_HEADERS);

    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        failureCase::execute);

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

    var failureCase = new DecodeFailureObjectCase(decodingProcessor, oversizedPayload, Map.of());
    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        failureCase::execute);

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
    var failureCase = new DecodeFailureObjectCase(decodingProcessor, oversizedPayload, Map.of());
    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        failureCase::execute);

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

    var failureCase = new DecodeFailureCase(decodingProcessor, payload, headers);
    WebhookDecodingException exception = assertThrows(WebhookDecodingException.class,
        failureCase::execute);

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

  private record DecodeFailureWithConnectorCase(DecodingProcessor decodingProcessor, Object payload,
      Map<String, Object> headers, String connector) {

    void execute() {
      decodingProcessor.decode(payload, headers, connector);
    }
  }

  private record DecodeFailureObjectCase(DecodingProcessor decodingProcessor, Object payload,
      Map<String, Object> headers) {

    void execute() {
      decodingProcessor.decode(payload, headers);
    }
  }

  private long findLongCounterValue(String metricName, Map<String, String> expectedAttributes) {
    return snapshotMetrics().stream().filter(metric -> metricName.equals(metric.getName()))
        .flatMap(metric -> metric.getLongSumData().getPoints().stream())
        .filter(point -> hasAttributes(point.getAttributes(), expectedAttributes))
        .mapToLong(LongPointData::getValue).sum();
  }

  private long findHistogramCount(Map<String, String> expectedAttributes) {
    return snapshotMetrics().stream().filter(metric -> PAYLOAD_SIZE_BYTES.equals(metric.getName()))
        .flatMap(metric -> metric.getHistogramData().getPoints().stream())
        .filter(point -> hasAttributes(point.getAttributes(), expectedAttributes))
        .mapToLong(HistogramPointData::getCount).sum();
  }

  private double findHistogramSum(Map<String, String> expectedAttributes) {
    return snapshotMetrics().stream().filter(metric -> PAYLOAD_SIZE_BYTES.equals(metric.getName()))
        .flatMap(metric -> metric.getHistogramData().getPoints().stream())
        .filter(point -> hasAttributes(point.getAttributes(), expectedAttributes))
        .mapToDouble(HistogramPointData::getSum).sum();
  }

  private List<MetricData> snapshotMetrics() {
    meterProvider.forceFlush().join(10, TimeUnit.SECONDS);
    return List.copyOf(metricReader.collectAllMetrics());
  }

  private boolean hasAttributes(Attributes attributes, Map<String, String> expectedAttributes) {
    return expectedAttributes.entrySet().stream().allMatch(
        entry -> entry.getValue().equals(attributes.get(AttributeKey.stringKey(entry.getKey()))));
  }
}
