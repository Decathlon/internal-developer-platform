package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Debug endpoint used to validate that OpenTelemetry spans are created and
 * exported.
 *
 * <p>
 * This endpoint intentionally creates a custom span with attributes and logs an
 * event so that Cloud Run logs can confirm whether the SDK is active and
 * whether the exporter is called.
 */
@RestController
@RequestMapping("/debug")
public class OtelDebugController {

  private static final Tracer TRACER = GlobalOpenTelemetry.get().getTracer("idp-core-debug");

  @GetMapping("/otel-test")
  public ResponseEntity<Map<String, String>> triggerOtelTrace() {
    Span span = TRACER.spanBuilder("debug.otel-test").setAttribute("debug.enabled", true)
        .setAttribute("debug.route", "/debug/otel-test").startSpan();

    try (Scope ignored = span.makeCurrent()) {
      span.addEvent("debug-otel-triggered");
      span.setStatus(StatusCode.OK);
      return ResponseEntity.ok(Map.of("status", "ok", "trace", "debug.otel-test"));
    } finally {
      span.end();
    }
  }
}
