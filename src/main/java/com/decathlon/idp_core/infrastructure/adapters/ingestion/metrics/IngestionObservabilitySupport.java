package com.decathlon.idp_core.infrastructure.adapters.ingestion.metrics;

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

  public static void setSpanAttribute(Span span, String attributeName, String attributeValue) {
    if (span == null || attributeName == null || attributeName.isBlank()
        || attributeValue == null) {
      return;
    }
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
