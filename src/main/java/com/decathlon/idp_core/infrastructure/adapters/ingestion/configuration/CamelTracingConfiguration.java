package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import lombok.extern.slf4j.Slf4j;

/// Configures the Camel/OpenTelemetry tracing bridge for webhook ingestion routes.
///
/// This configuration:
/// - creates a Camel-scoped `Tracer` from the application `OpenTelemetry` instance,
/// - exposes `ContextPropagators` to Camel so context propagation remains consistent,
/// - binds both objects into the Camel registry before route startup.
///
/// The bridge keeps Camel route spans connected to the application trace context
/// without introducing additional metric instrumentation.
@Configuration
@Slf4j
public class CamelTracingConfiguration {

  static final String CAMEL_INSTRUMENTATION_SCOPE = "camel";

  /// Creates the Camel-scoped OpenTelemetry tracer used by Camel route
  /// instrumentation.
  ///
  /// The bean resolves `OpenTelemetry` from Spring's context. If no OpenTelemetry
  /// bean is available (for example in sliced tests), it falls back to
  /// `OpenTelemetry.noop()` to keep the bridge null-safe.
  ///
  /// @param openTelemetryProvider provides the runtime `OpenTelemetry` instance
  /// from Spring
  @Bean
  public Tracer camelTracer(ObjectProvider<OpenTelemetry> openTelemetryProvider) {
    OpenTelemetry openTelemetry = openTelemetryProvider.getIfAvailable(OpenTelemetry::noop);
    return openTelemetry.getTracer(CAMEL_INSTRUMENTATION_SCOPE);
  }

  /// Exposes OpenTelemetry context propagators for Camel context hand-off.
  ///
  /// Camel uses these propagators to preserve trace/span correlation when
  /// processing
  /// exchanges across route steps and integrations. If no OpenTelemetry bean is
  /// available, this method uses `OpenTelemetry.noop()` propagators.
  ///
  /// @param openTelemetryProvider provides the runtime `OpenTelemetry` instance
  /// from Spring
  @Bean
  public ContextPropagators camelContextPropagators(
      ObjectProvider<OpenTelemetry> openTelemetryProvider) {
    OpenTelemetry openTelemetry = openTelemetryProvider.getIfAvailable(OpenTelemetry::noop);
    return openTelemetry.getPropagators();
  }

  /// Registers tracing objects in Camel before route startup.
  ///
  /// The bridge binds both `Tracer` and `ContextPropagators` into the Camel
  /// registry
  /// so Camel OpenTelemetry components can resolve them during initialization.
  ///
  /// @param camelTracer the Camel-scoped tracer bound into Camel registry
  /// @param camelContextPropagators context propagators bound into Camel registry
  @Bean
  public CamelContextConfiguration camelTelemetryBridge(Tracer camelTracer,
      ContextPropagators camelContextPropagators) {
    return new CamelContextConfiguration() {
      @Override
      public void beforeApplicationStart(CamelContext camelContext) {
        // 1. Preserve the native OTel tracing bridge.
        camelContext.getRegistry().bind("camelTracer", camelTracer);
        camelContext.getRegistry().bind("camelContextPropagators", camelContextPropagators);

        log.info("[TELEMETRY BRIDGE] Registered Camel OTel Tracer and context propagators.");
      }

      @Override
      public void afterApplicationStart(CamelContext camelContext) {
        // No-op.
      }
    };
  }
}
