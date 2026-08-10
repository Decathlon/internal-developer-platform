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
  public static final String RAW_PAYLOAD_BODY_PROPERTY = "rawPayloadBody";
  public static final String APPLICATION_JSON = "application/json";

  public static final String WEBHOOK_BASE_PATH = "/webhooks";
  public static final String WEBHOOK_IDENTIFIER_PATH = "/{webhookIdentifier}";
  public static final String WEBHOOK_IDENTIFIER_HEADER = "webhookIdentifier";

  public static final String DIRECT_WEBHOOK_INGESTION = "direct:webhook-ingestion";
  public static final String DIRECT_PROCESS_EVENT = "direct:process-event";
  public static final String DIRECT_FETCH_CONFIGURATION = "direct:fetch-configuration";
  public static final String DIRECT_DECODE_PAYLOAD = "direct:decode-payload";
  public static final String DIRECT_VALIDATE_ENABLED = "direct:validate-enabled";

  public static final String ROUTE_ID_GENERIC_WEBHOOK_ENTRYPOINT = "generic-webhook-entrypoint";
  public static final String ROUTE_ID_WEBHOOK_PIPELINE = "webhook-pipeline";
  public static final String ROUTE_ID_FETCH_WEBHOOK_CONFIG = "fetch-webhook-config";
  public static final String ROUTE_ID_DECODE_PAYLOAD = "decode-payload";
  public static final String ROUTE_ID_VALIDATE_WEBHOOK_ENABLED = "validate-webhook-enabled";

  public static final int HTTP_OK = 200;
  public static final String ERROR_DESCRIPTION_WEBHOOK_NOT_FOUND = "Webhook configuration not found";
  public static final String ERROR_DESCRIPTION_WEBHOOK_DISABLED = "Webhook connector is disabled";
  public static final String ERROR_DESCRIPTION_WEBHOOK_CONFIGURATION_MISSING = "Webhook configuration unavailable";
  public static final String ERROR_DESCRIPTION_INTERNAL_SERVER_ERROR = "Internal server error processing ingestion payload";
  public static final String SUCCESS_BODY_CONFIGURATION_LOADED = "{\"status\": \"SUCCESS\", \"message\": \"Webhook configuration loaded and enabled.\"}";

  public static final String CONTENT_ENCODING_GZIP = "gzip";
  public static final String CONTENT_ENCODING_IDENTITY = "identity";
  public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

  public static final String UNKNOWN_VALUE = "unknown";

  private IngestionConstants() {
  }
}
