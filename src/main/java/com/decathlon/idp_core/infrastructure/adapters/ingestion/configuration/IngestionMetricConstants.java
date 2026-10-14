package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

/**
 * Shared metric names for the ingestion workflow.
 *
 * <p>
 * These values are intentionally centralized so the routing and processing
 * layers can reuse the same business metric names without duplicating string
 * literals across processors.
 */
public final class IngestionMetricConstants {

  public static final String SECURITY_REQUESTS_TOTAL = "idp.webhook.security.requests.total";
  public static final String SECURITY_RESULT = "idp.webhook.security.result";
  public static final String SECURITY_VALIDATION_ERRORS_TOTAL = "idp.webhook.security.validation.errors.total";

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

  private IngestionMetricConstants() {
  }
}
