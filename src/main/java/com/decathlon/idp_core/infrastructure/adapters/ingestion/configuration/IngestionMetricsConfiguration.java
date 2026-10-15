package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;

@Configuration
public class IngestionMetricsConfiguration {

  @Bean("ingestionMeter")
  public Meter ingestionMeter(OpenTelemetry openTelemetry) {
    return openTelemetry.getMeterProvider().meterBuilder("com.decathlon.idp_core.ingestion")
        .build();
  }

  @Bean("securityMeter")
  public Meter securityMeter(OpenTelemetry openTelemetry) {
    return openTelemetry.getMeterProvider().meterBuilder("com.decathlon.idp_core.security").build();
  }

  @Bean("decodingMeter")
  public Meter decodingMeter(OpenTelemetry openTelemetry) {
    return openTelemetry.getMeterProvider().meterBuilder("com.decathlon.idp_core.decoding").build();
  }
}
