package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

/// Declares named OpenTelemetry `Meter` beans for webhook ingestion observability.
///
/// Each meter uses a dedicated instrumentation scope name so emitted metrics can be
/// grouped by concern (`ingestion`, `security`, `decoding`) while sharing the same
/// runtime OpenTelemetry SDK.
@Configuration
public class IngestionMetricsConfiguration {

  /// Creates the meter used by ingestion flow processors and mapping/entity
  /// metrics.
  ///
  /// @param openTelemetry the runtime OpenTelemetry instance managed by Spring
  @Bean("ingestionMeter")
  public Meter ingestionMeter(OpenTelemetry openTelemetry) {
    return openTelemetry.getMeter("com.decathlon.idp_core.ingestion");
  }

  /// Creates the meter used by webhook security validation and decision metrics.
  ///
  /// @param openTelemetry the runtime OpenTelemetry instance managed by Spring
  @Bean("securityMeter")
  public Meter securityMeter(OpenTelemetry openTelemetry) {
    return openTelemetry.getMeter("com.decathlon.idp_core.security");
  }

  /// Creates the meter used by payload decoding metrics.
  ///
  /// @param openTelemetry the runtime OpenTelemetry instance managed by Spring
  @Bean("decodingMeter")
  public Meter decodingMeter(OpenTelemetry openTelemetry) {
    return openTelemetry.getMeter("com.decathlon.idp_core.decoding");
  }
}
