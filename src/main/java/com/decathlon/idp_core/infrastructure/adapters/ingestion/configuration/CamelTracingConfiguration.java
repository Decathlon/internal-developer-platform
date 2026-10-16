package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;

/// Exposes the auto-configured OpenTelemetry tracer to Camel.
///
/// `camel-opentelemetry2` looks for a `Tracer` and `ContextPropagators` bean first, then falls back
/// to `GlobalOpenTelemetry`. The official OpenTelemetry starter does not register the global
/// instance, so without these beans Camel gets a no-op tracer and produces no route spans.
/// No SDK is created here: both beans delegate to the instance built by the starter.
///
/// Active only with the official OpenTelemetry starter. Spring Boot's own OpenTelemetry starter
/// already defines a `Tracer` and `ContextPropagators`; redefining them there creates a bean cycle.
@Configuration
@ConditionalOnClass(name = "io.opentelemetry.instrumentation.spring.autoconfigure.OpenTelemetryAutoConfiguration")
public class CamelTracingConfiguration {

  /// Instrumentation scope used for spans created by Camel routes.
  static final String CAMEL_INSTRUMENTATION_SCOPE = "camel";

  /// Tracer backed by the starter SDK, resolved by Camel through its registry.
  @Bean
  Tracer camelTracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(CAMEL_INSTRUMENTATION_SCOPE);
  }

  /// Propagators of the starter SDK, used by Camel to inject and extract trace
  /// context.
  @Bean
  ContextPropagators camelContextPropagators(OpenTelemetry openTelemetry) {
    return openTelemetry.getPropagators();
  }
}
