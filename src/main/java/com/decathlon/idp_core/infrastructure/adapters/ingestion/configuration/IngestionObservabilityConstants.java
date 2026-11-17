package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import io.opentelemetry.api.common.AttributeKey;

public final class IngestionObservabilityConstants {

  public static final String SECURITY_REQUESTS_TOTAL = "idp.webhook.security.requests.total";
  public static final String SECURITY_RESULT = "idp.webhook.security.result";
  public static final String SECURITY_VALIDATION_ERRORS_TOTAL = "idp.webhook.security.validation.errors.total";

  public static final String PAYLOAD_DECODED_TOTAL = "idp.webhook.payload.decoded.total";
  public static final String PAYLOAD_DECODE_ERRORS_TOTAL = "idp.webhook.payload.decode.errors.total";
  public static final String PAYLOAD_SIZE_BYTES = "idp.webhook.payload.size.bytes";

  public static final String MAPPING_APPLIED_TOTAL = "idp.webhook.mapping.applied.total";
  public static final String MAPPING_FILTERED_TOTAL = "idp.webhook.mapping.filtered.total";
  public static final String MAPPING_RESULT = "idp.webhook.mapping.result";
  public static final String MAPPING_ERRORS_TOTAL = "idp.webhook.mapping.errors.total";
  public static final String MAPPING_LATENCY_MS = "idp.webhook.mapping.latency.ms";

  public static final String ENTITY_CREATED_TOTAL = "idp.webhook.entity.created.total";
  public static final String ENTITY_UPDATED_TOTAL = "idp.webhook.entity.updated.total";
  public static final String ENTITY_DELETED_TOTAL = "idp.webhook.entity.deleted.total";
  public static final String ENTITY_PATCH_TOTAL = "idp.webhook.entity.patch.total";
  public static final String ENTITY_VALIDATION_ERRORS_TOTAL = "idp.webhook.entity.validation.errors.total";

  public static final String FUNCTIONAL_VALIDATION_FAILED_TOTAL = "idp.webhook.functional.validation.failed.total";
  public static final String ENTITY_NOT_FOUND_TOTAL = "idp.webhook.entity.notfound.total";
  public static final String ENTITY_CONFLICT_TOTAL = "idp.webhook.entity.conflict.total";

  public static final String ATTRIBUTE_CONNECTOR = "connector";
  public static final String ATTRIBUTE_MAPPING_ID = "mapping_id";
  public static final String ATTRIBUTE_ACTION = "action";
  public static final String ATTRIBUTE_REASON = "reason";
  public static final String ATTRIBUTE_TEMPLATE = "template";
  public static final String ATTRIBUTE_RESULT = "result";
  public static final String ATTRIBUTE_SECURITY_TYPE = "security_type";
  public static final String ATTRIBUTE_ENCODING = "encoding";
  public static final String ATTRIBUTE_ERROR_TYPE = "error_type";

  public static final AttributeKey<String> CONNECTOR_KEY = AttributeKey
      .stringKey(ATTRIBUTE_CONNECTOR);
  public static final AttributeKey<String> MAPPING_ID_KEY = AttributeKey
      .stringKey(ATTRIBUTE_MAPPING_ID);
  public static final AttributeKey<String> ACTION_KEY = AttributeKey.stringKey(ATTRIBUTE_ACTION);
  public static final AttributeKey<String> REASON_KEY = AttributeKey.stringKey(ATTRIBUTE_REASON);
  public static final AttributeKey<String> TEMPLATE_KEY = AttributeKey
      .stringKey(ATTRIBUTE_TEMPLATE);
  public static final AttributeKey<String> RESULT_KEY = AttributeKey.stringKey(ATTRIBUTE_RESULT);
  public static final AttributeKey<String> SECURITY_TYPE_KEY = AttributeKey
      .stringKey(ATTRIBUTE_SECURITY_TYPE);
  public static final AttributeKey<String> ENCODING_KEY = AttributeKey
      .stringKey(ATTRIBUTE_ENCODING);
  public static final AttributeKey<String> ERROR_TYPE_KEY = AttributeKey
      .stringKey(ATTRIBUTE_ERROR_TYPE);

  public static final String SPAN_ATTRIBUTE_WEBHOOK_IDENTIFIER = "idp.webhook.identifier";
  public static final String SPAN_ATTRIBUTE_SECURITY_RESULT = "idp.webhook.security.result";

  private IngestionObservabilityConstants() {
  }
}
