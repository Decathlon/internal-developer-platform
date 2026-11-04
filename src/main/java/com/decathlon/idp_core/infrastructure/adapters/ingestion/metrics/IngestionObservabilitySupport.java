package com.decathlon.idp_core.infrastructure.adapters.ingestion.metrics;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_GZIP;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_IDENTITY;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.ATTRIBUTE_CONNECTOR;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.PAYLOAD_DECODED_TOTAL;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.PAYLOAD_DECODE_ERRORS_TOTAL;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.PAYLOAD_SIZE_BYTES;
import static reactor.netty.http.HttpConnectionLiveness.log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;

public final class IngestionObservabilitySupport {

  private static final Map<String, LongCounter> COUNTER_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, LongHistogram> HISTOGRAM_CACHE = new ConcurrentHashMap<>();

  private IngestionObservabilitySupport() {
  }

  public static void incrementCounter(Meter meter, String metricName, String... attributes) {
    log.info(">>> [OTEL DEBUG] Increment counter: " + metricName + " with attributes: "
        + java.util.Arrays.toString(attributes));
    LongCounter counter = COUNTER_CACHE.computeIfAbsent(metricName,
        name -> meter.counterBuilder(name).build());
    counter.add(1, toAttributes(attributes));
  }

  public static void recordHistogram(Meter meter, String metricName, long value,
      String... attributes) {
    LongHistogram histogram = HISTOGRAM_CACHE.computeIfAbsent(metricName,
        name -> meter.histogramBuilder(name).ofLongs().build());
    histogram.record(value, toAttributes(attributes));
  }

  public static String resolveEncodingMetricLabel(List<String> encodingChain) {
    if (encodingChain == null || encodingChain.isEmpty() || (encodingChain.size() == 1
        && CONTENT_ENCODING_IDENTITY.equals(encodingChain.getFirst()))) {
      return "identity";
    }
    if (encodingChain.size() == 1 && CONTENT_ENCODING_GZIP.equals(encodingChain.getFirst())) {
      return "gzip";
    }
    return "chained";
  }

  public static void recordPayloadSizeMetric(Meter meter, String connector, long payloadSizeBytes) {
    LongHistogram histogram = HISTOGRAM_CACHE.computeIfAbsent(PAYLOAD_SIZE_BYTES,
        name -> meter.histogramBuilder(name).ofLongs().build());
    histogram.record(payloadSizeBytes,
        Attributes.of(AttributeKey.stringKey(ATTRIBUTE_CONNECTOR), connector));
  }

  public static void recordDecodedMetric(Meter meter, String connector, String encoding) {
    LongCounter counter = COUNTER_CACHE.computeIfAbsent(PAYLOAD_DECODED_TOTAL,
        name -> meter.counterBuilder(name).build());
    counter.add(1, Attributes.of(AttributeKey.stringKey(ATTRIBUTE_CONNECTOR), connector,
        AttributeKey.stringKey("encoding"), encoding));
  }

  public static void recordDecodeErrorMetric(Meter meter, String connector, String reason) {
    LongCounter counter = COUNTER_CACHE.computeIfAbsent(PAYLOAD_DECODE_ERRORS_TOTAL,
        name -> meter.counterBuilder(name).build());
    counter.add(1, Attributes.of(AttributeKey.stringKey(ATTRIBUTE_CONNECTOR), connector,
        AttributeKey.stringKey("reason"), reason));
  }

  public static void setSpanAttribute(Span span, String attributeName, String attributeValue) {
    if (span == null || attributeName == null || attributeName.isBlank()
        || attributeValue == null) {
      return;
    }
    log.info(">>> [OTEL DEBUG] Set span attribute: " + attributeName + " = " + attributeValue);
    span.setAttribute(attributeName, attributeValue);
  }

  public static Attributes toAttributes(String... keyValues) {
    if (keyValues == null || keyValues.length == 0) {
      return Attributes.empty();
    }
    if ((keyValues.length % 2) != 0) {
      throw new IllegalArgumentException("Metric attributes must be provided as key/value pairs");
    }
    var builder = Attributes.builder();
    for (int i = 0; i < keyValues.length; i += 2) {
      builder.put(AttributeKey.stringKey(keyValues[i]), keyValues[i + 1]);
    }
    return builder.build();
  }
}
