package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import org.apache.camel.CamelContext;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerExchangeEventNotifier;
import org.apache.camel.component.micrometer.eventnotifier.MicrometerRouteEventNotifier;
import org.apache.camel.component.micrometer.messagehistory.MicrometerMessageHistoryFactory;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CamelTracingConfiguration {

  static final String CAMEL_INSTRUMENTATION_SCOPE = "camel";

  @Bean
  public Tracer camelTracer(ObjectProvider<OpenTelemetry> openTelemetryProvider) {
    return openTelemetryProvider.getIfAvailable().getTracer(CAMEL_INSTRUMENTATION_SCOPE);
  }

  @Bean
  public ContextPropagators camelContextPropagators(
      ObjectProvider<OpenTelemetry> openTelemetryProvider) {
    return openTelemetryProvider.getIfAvailable().getPropagators();
  }

  @Bean
  public CamelContextConfiguration camelTelemetryBridge(Tracer camelTracer,
      ContextPropagators camelContextPropagators, MeterRegistry meterRegistry) {
    return new CamelContextConfiguration() {
      @Override
      public void beforeApplicationStart(CamelContext camelContext) {
        // 1. Preserve the native OTel tracing bridge.
        camelContext.getRegistry().bind("camelTracer", camelTracer);
        camelContext.getRegistry().bind("camelContextPropagators", camelContextPropagators);

        // 2. Emit camel.* metrics into Micrometer so they can be exported through the
        // OTLP registry.
        MicrometerRoutePolicyFactory routePolicyFactory = new MicrometerRoutePolicyFactory();
        routePolicyFactory.setMeterRegistry(meterRegistry);
        camelContext.addRoutePolicyFactory(routePolicyFactory);

        MicrometerMessageHistoryFactory messageHistoryFactory = new MicrometerMessageHistoryFactory();
        messageHistoryFactory.setMeterRegistry(meterRegistry);
        camelContext.setMessageHistoryFactory(messageHistoryFactory);

        MicrometerExchangeEventNotifier exchangeNotifier = new MicrometerExchangeEventNotifier();
        exchangeNotifier.setMeterRegistry(meterRegistry);
        camelContext.getManagementStrategy().addEventNotifier(exchangeNotifier);

        MicrometerRouteEventNotifier routeNotifier = new MicrometerRouteEventNotifier();
        routeNotifier.setMeterRegistry(meterRegistry);
        camelContext.getManagementStrategy().addEventNotifier(routeNotifier);

        log.info("[TELEMETRY BRIDGE] Registered Camel OTel Tracer and Micrometer route metrics.");
      }

      @Override
      public void afterApplicationStart(CamelContext camelContext) {
        // No-op.
      }
    };
  }
}
