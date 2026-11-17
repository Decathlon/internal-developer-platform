package com.decathlon.idp_core.infrastructure.adapters.ingestion.metrics;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_GZIP;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_IDENTITY;

import java.util.List;

import io.opentelemetry.api.trace.Span;

/// Provides shared stateless observability helpers for ingestion processors.
///
/// This utility intentionally does not keep metric instruments or provider state.
/// Processors own their counters/histograms as singleton fields built from their
/// injected `Meter` instances.
public final class IngestionObservabilitySupport {

  private IngestionObservabilitySupport() {
  }

  /// Resolves the payload decoding label used by ingestion decode metrics.
  ///
  /// @param encodingChain normalized content-encoding chain
  /// @return `identity`, `gzip`, or `chained`
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

  /// Sets a string span attribute when both name and value are valid.
  ///
  /// @param span current span
  /// @param attributeName attribute key name
  /// @param attributeValue attribute value
  public static void setSpanAttribute(Span span, String attributeName, String attributeValue) {
    if (span == null || attributeName == null || attributeName.isBlank()
        || attributeValue == null) {
      return;
    }
    span.setAttribute(attributeName, attributeValue);
  }
}
