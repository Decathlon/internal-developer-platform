package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

/**
 * Shared constants for the ingestion infrastructure adapter.
 *
 * <p>
 * Centralizing these values avoids duplicated literals across route
 * configuration, processors, and exception mapping.
 */
public final class IngestionConstants {

  public static final String CONNECTOR_IDENTIFIER_PROPERTY = "connectorIdentifier";
  public static final String WEBHOOK_CONFIG_PROPERTY = "webhookConfig";
  public static final String APPLICATION_JSON = "application/json";

  public static final String WEBHOOK_BASE_PATH = "/webhooks";
  public static final String WEBHOOK_IDENTIFIER_PATH = "/{webhookIdentifier}";
  public static final String WEBHOOK_IDENTIFIER_HEADER = "webhookIdentifier";

  public static final String DIRECT_WEBHOOK_INGESTION = "direct:webhook-ingestion";
  public static final String DIRECT_PROCESS_EVENT = "direct:process-event";
  public static final String DIRECT_FETCH_CONFIGURATION = "direct:fetch-configuration";
  public static final String DIRECT_VALIDATE_ENABLED = "direct:validate-enabled";

  public static final String ROUTE_ID_GENERIC_WEBHOOK_ENTRYPOINT = "generic-webhook-entrypoint";
  public static final String ROUTE_ID_WEBHOOK_PIPELINE = "webhook-pipeline";
  public static final String ROUTE_ID_FETCH_WEBHOOK_CONFIG = "fetch-webhook-config";
  public static final String ROUTE_ID_VALIDATE_WEBHOOK_ENABLED = "validate-webhook-enabled";

  public static final String TRACE_METHOD_FAILURE = "traceFailure";
  public static final String TRACE_METHOD_ENABLED_VALIDATION_PASSED = "traceEnabledValidationPassed";
  public static final String TRACE_METHOD_CONFIGURATION_LOADED = "traceConfigurationLoaded";
  public static final String TRACE_METHOD_LOG_INBOUND_REQUEST = "logInboundRequest";

  public static final String OTEL_TRACER_NAME = "com.decathlon.idp_core.ingestion.webhook";
  public static final String OTEL_SPAN_REQUEST_RECEIVED = "webhook.ingestion.request.received";
  public static final String OTEL_SPAN_CONFIGURATION_LOADED = "webhook.configuration.loaded";
  public static final String OTEL_SPAN_ENABLED_VALIDATION_PASSED = "webhook.enabled.validation.passed";
  public static final String OTEL_SPAN_REQUEST_FAILED = "webhook.ingestion.request.failed";

  public static final String OTEL_ATTR_WEBHOOK_IDENTIFIER = "webhook.identifier";
  public static final String OTEL_ATTR_HTTP_METHOD = "http.method";
  public static final String OTEL_ATTR_HTTP_CONTENT_TYPE = "http.content_type";
  public static final String OTEL_ATTR_WEBHOOK_PAYLOAD_SIZE = "webhook.payload.size_bytes";
  public static final String OTEL_ATTR_WEBHOOK_PAYLOAD_SHA256 = "webhook.payload.sha256";
  public static final String OTEL_ATTR_HTTP_RESPONSE_STATUS_CODE = "http.response.status_code";
  public static final String OTEL_ATTR_EXCEPTION_TYPE = "exception.type";

  public static final String CONTENT_ENCODING_GZIP = "gzip";
  public static final String CONTENT_ENCODING_IDENTITY = "identity";
  public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

  public static final String UNKNOWN_VALUE = "unknown";
  public static final String RAW_ENCODING_MODE = "raw";

  private IngestionConstants() {
  }
}
