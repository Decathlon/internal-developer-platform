package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.opentelemetry.api.OpenTelemetry;

/// Inspects the auto-configured telemetry instance without constructing a provider.
@Component
public final class OtelLoggingDebugConfig {

  private static final Logger log = LoggerFactory.getLogger(OtelLoggingDebugConfig.class);
  private static final String INSTRUMENTATION_SCOPE = "com.decathlon.idp_core.debug";
  private final OpenTelemetry openTelemetry;

  /// Uses the instance created by the OpenTelemetry starter.
  public OtelLoggingDebugConfig(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /// Reports a no-op meter without claiming successful delivery to a backend.
  @EventListener(ApplicationReadyEvent.class)
  public void debugOtelMeterInitialization() {
    var meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);
    log.info("[OTEL SDK CHECK] OpenTelemetry: {}; MeterProvider: {}; Meter: {}",
        openTelemetry.getClass().getName(), openTelemetry.getMeterProvider().getClass().getName(),
        meter.getClass().getName());
    // The API fallback is not public; compare runtime types instead of SDK internal
    // names.
    if (meter.getClass().equals(OpenTelemetry.noop().getMeter(INSTRUMENTATION_SCOPE).getClass())) {
      log.warn(
          "[OTEL SDK CHECK] No-op Meter: custom metrics are not recorded. Check SDK autoconfiguration and OTEL_SDK_DISABLED.");
    } else {
      log.info(
          "[OTEL SDK CHECK] Meter is not the API no-op. Exporter delivery must still be verified.");
    }
  }
}
