package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONNECTOR_IDENTIFIER_PROPERTY;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_GZIP;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_ATTR_EXCEPTION_TYPE;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_ATTR_HTTP_CONTENT_TYPE;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_ATTR_HTTP_METHOD;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_ATTR_HTTP_RESPONSE_STATUS_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_ATTR_WEBHOOK_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_ATTR_WEBHOOK_PAYLOAD_SHA256;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_ATTR_WEBHOOK_PAYLOAD_SIZE;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_SPAN_CONFIGURATION_LOADED;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_SPAN_ENABLED_VALIDATION_PASSED;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_SPAN_REQUEST_FAILED;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_SPAN_REQUEST_RECEIVED;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.OTEL_TRACER_NAME;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.RAW_ENCODING_MODE;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.UNKNOWN_VALUE;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Adds OpenTelemetry spans for inbound webhook requests.
 *
 * <p>
 * This processor records only operational metadata (identifier, payload size,
 * content type, checksum) to avoid leaking sensitive payload content into
 * traces.
 */
@Component
public class WebhookIngestionTracingProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(WebhookIngestionTracingProcessor.class);
  private static final int MAX_LOGGED_BODY_LENGTH = 8_192;
  private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie",
      "set-cookie", "proxy-authorization", "x-api-key", "x-auth-token", "x-forwarded-access-token",
      "traceparent", "tracestate", "baggage");

  private final Tracer tracer = GlobalOpenTelemetry.getTracer(OTEL_TRACER_NAME);

  /**
   * Logs a sanitized inbound webhook payload with non-sensitive headers only.
   *
   * @param exchange
   *          Camel exchange carrying request headers and body
   */
  public void logInboundRequest(Exchange exchange) {
    String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY, String.class);
    String contentEncoding = exchange.getIn().getHeader(Exchange.CONTENT_ENCODING, String.class);
    String charset = exchange.getIn().getHeader(Exchange.CHARSET_NAME, String.class);
    String encodingMode = resolveEncodingMode(contentEncoding);
    String bodyPreview = buildBodyPreview(exchange.getIn().getBody());
    Map<String, Object> safeHeaders = extractSafeHeaders(exchange);

    LOGGER.info(
        "Inbound webhook received: identifier={}, encoding={}, encoding_mode={}, charset={}, headers={}, body={}"
            + " (truncated_if_needed)",
        connectorIdentifier == null ? UNKNOWN_VALUE : connectorIdentifier,
        contentEncoding == null ? UNKNOWN_VALUE : contentEncoding, encodingMode,
        charset == null ? UNKNOWN_VALUE : charset, safeHeaders, bodyPreview);
  }

  /**
   * Traces the HTTP webhook reception.
   *
   * @param exchange
   *          Camel exchange carrying request headers and body
   */
  public void traceInboundRequest(Exchange exchange) {
    Span span = tracer.spanBuilder(OTEL_SPAN_REQUEST_RECEIVED).startSpan();
    try (Scope scope = span.makeCurrent()) {
      String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY,
          String.class);
      String httpMethod = exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class);
      String contentType = exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);
      Object payload = exchange.getIn().getBody();

      span.setAttribute(OTEL_ATTR_WEBHOOK_IDENTIFIER,
          connectorIdentifier == null ? UNKNOWN_VALUE : connectorIdentifier);
      span.setAttribute(OTEL_ATTR_HTTP_METHOD, httpMethod == null ? UNKNOWN_VALUE : httpMethod);
      span.setAttribute(OTEL_ATTR_HTTP_CONTENT_TYPE,
          contentType == null ? UNKNOWN_VALUE : contentType);
      span.setAttribute(OTEL_ATTR_WEBHOOK_PAYLOAD_SIZE, estimatePayloadSize(payload));
      span.setAttribute(OTEL_ATTR_WEBHOOK_PAYLOAD_SHA256, toSha256(payload));
    } finally {
      span.end();
    }
  }

  /**
   * Traces successful webhook configuration loading.
   *
   * @param exchange
   *          Camel exchange with resolved `webhookConfig` property
   */
  public void traceConfigurationLoaded(Exchange exchange) {
    Span span = tracer.spanBuilder(OTEL_SPAN_CONFIGURATION_LOADED).startSpan();
    try (Scope scope = span.makeCurrent()) {
      String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY,
          String.class);
      span.setAttribute(OTEL_ATTR_WEBHOOK_IDENTIFIER,
          connectorIdentifier == null ? UNKNOWN_VALUE : connectorIdentifier);
    } finally {
      span.end();
    }
  }

  /**
   * Traces successful enabled-flag validation.
   *
   * @param exchange
   *          Camel exchange of the current webhook request
   */
  public void traceEnabledValidationPassed(Exchange exchange) {
    Span span = tracer.spanBuilder(OTEL_SPAN_ENABLED_VALIDATION_PASSED).startSpan();
    try (Scope scope = span.makeCurrent()) {
      String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY,
          String.class);
      span.setAttribute(OTEL_ATTR_WEBHOOK_IDENTIFIER,
          connectorIdentifier == null ? UNKNOWN_VALUE : connectorIdentifier);
    } finally {
      span.end();
    }
  }

  /**
   * Traces a failure translated by Camel exception handlers.
   *
   * @param exchange
   *          Camel exchange containing the caught exception and response code
   */
  public void traceFailure(Exchange exchange) {
    Span span = tracer.spanBuilder(OTEL_SPAN_REQUEST_FAILED).startSpan();
    try (Scope scope = span.makeCurrent()) {
      Throwable throwable = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
      String connectorIdentifier = exchange.getProperty(CONNECTOR_IDENTIFIER_PROPERTY,
          String.class);
      Integer responseCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE,
          Integer.class);

      span.setAttribute(OTEL_ATTR_WEBHOOK_IDENTIFIER,
          connectorIdentifier == null ? UNKNOWN_VALUE : connectorIdentifier);
      if (responseCode != null) {
        span.setAttribute(OTEL_ATTR_HTTP_RESPONSE_STATUS_CODE, responseCode);
      }
      if (throwable != null) {
        span.recordException(throwable);
        span.setStatus(StatusCode.ERROR, throwable.getMessage());
        span.setAttribute(OTEL_ATTR_EXCEPTION_TYPE, throwable.getClass().getName());
      }
    } finally {
      span.end();
    }
  }

  private static long estimatePayloadSize(Object payload) {
    if (payload == null) {
      return 0L;
    }
    return switch (payload) {
      case byte[] bytes -> bytes.length;
      case String string -> string.getBytes(StandardCharsets.UTF_8).length;
      default -> payload.toString().getBytes(StandardCharsets.UTF_8).length;
    };
  }

  private static String toSha256(Object payload) {
    if (payload == null) {
      return "";
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = switch (payload) {
        case byte[] payloadBytes -> payloadBytes;
        case String payloadString -> payloadString.getBytes(StandardCharsets.UTF_8);
        default -> payload.toString().getBytes(StandardCharsets.UTF_8);
      };
      byte[] hash = digest.digest(bytes);
      StringBuilder hexBuilder = new StringBuilder(hash.length * 2);
      for (byte currentByte : hash) {
        hexBuilder.append(String.format("%02x", currentByte));
      }
      return hexBuilder.toString();
    } catch (NoSuchAlgorithmException exception) {
      return "sha256-unavailable";
    }
  }

  private static Map<String, Object> extractSafeHeaders(Exchange exchange) {
    Map<String, Object> safeHeaders = new LinkedHashMap<>();
    for (Map.Entry<String, Object> headerEntry : exchange.getIn().getHeaders().entrySet()) {
      String headerName = headerEntry.getKey();
      if (headerName == null || isSensitiveHeader(headerName)) {
        continue;
      }
      Object headerValue = headerEntry.getValue();
      // Keep logging safe and stable even when Camel header values are null.
      safeHeaders.put(headerName, headerValue == null ? "<null>" : headerValue);
    }
    return safeHeaders;
  }

  private static boolean isSensitiveHeader(String headerName) {
    String normalizedHeaderName = headerName.toLowerCase(Locale.ROOT);
    return SENSITIVE_HEADERS.contains(normalizedHeaderName)
        || normalizedHeaderName.startsWith("x-forwarded-");
  }

  private static String buildBodyPreview(Object payload) {
    if (payload == null) {
      return "";
    }
    String body = switch (payload) {
      case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
      case String string -> string;
      default -> payload.toString();
    };
    if (body.length() <= MAX_LOGGED_BODY_LENGTH) {
      return body;
    }
    return body.substring(0, MAX_LOGGED_BODY_LENGTH) + "...[truncated]";
  }

  private static String resolveEncodingMode(String contentEncoding) {
    if (contentEncoding == null || contentEncoding.isBlank()) {
      return RAW_ENCODING_MODE;
    }
    String normalizedEncoding = contentEncoding.toLowerCase(Locale.ROOT);
    if (normalizedEncoding.contains(CONTENT_ENCODING_GZIP)) {
      return CONTENT_ENCODING_GZIP;
    }
    return normalizedEncoding;
  }
}
