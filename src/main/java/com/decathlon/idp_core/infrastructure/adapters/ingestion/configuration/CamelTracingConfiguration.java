package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CamelTracingConfiguration {

  static final String CAMEL_INSTRUMENTATION_SCOPE = "camel";

  @Bean
  public Tracer camelTracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(CAMEL_INSTRUMENTATION_SCOPE);
  }

  @Bean
  public ContextPropagators camelContextPropagators(OpenTelemetry openTelemetry) {
    return openTelemetry.getPropagators();
  }

  @Bean
  public CamelContextConfiguration camelTracingBridge(Tracer camelTracer,
      ContextPropagators camelContextPropagators) {
    return new CamelContextConfiguration() {
      @Override
      public void beforeApplicationStart(CamelContext camelContext) {
        // Expose Spring SDK beans directly in Camel registry before route startup.
        camelContext.getRegistry().bind("camelTracer", camelTracer);
        camelContext.getRegistry().bind("camelContextPropagators", camelContextPropagators);
        log.info(
            "[OTEL CAMEL BRIDGE] Bound Tracer={} and ContextPropagators={} into Camel registry",
            camelTracer.getClass().getName(), camelContextPropagators.getClass().getName());
      }

      @Override
      public void afterApplicationStart(CamelContext camelContext) {
        // No-op
      }
    };
  }
}
