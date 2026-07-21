package com.decathlon.idp_core.domain.model.entity_mapping;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.DRY_RUN_FILTER_SKIPPED;

import java.util.List;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.enums.ErrorType;

/**
 * Result of a dynamic mapping dry-run operation. Contains the list of mapping
 * outcomes (success, skipped, failure) without any transport-specific
 * semantics. Shared by every dry-run flow (single stateless mapping and
 * webhook-level aggregation).
 */
public record DryRunResult(List<DryRunEntityResult> entityResults) {

  public DryRunResult {
    entityResults = entityResults != null ? List.copyOf(entityResults) : List.of();
  }

  /**
   * Result for a single entity mapping attempt.
   */
  public record DryRunEntityResult(String mappingTemplateIdentifier, boolean success, Entity entity,
      DryRunError error) {

    /** Creates a successful result. */
    public static DryRunEntityResult success(String templateIdentifier, Entity entity) {
      return new DryRunEntityResult(templateIdentifier, true, entity, null);
    }

    /**
     * Creates a skipped result when the filter expression returned false or null.
     */
    public static DryRunEntityResult skipped(String templateIdentifier) {
      return new DryRunEntityResult(templateIdentifier, true, null,
          new DryRunError(ErrorType.SKIPPED, DRY_RUN_FILTER_SKIPPED));
    }

    /** Creates a failure result with a typed error category and message. */
    public static DryRunEntityResult failure(String templateIdentifier, ErrorType type,
        String message) {
      return new DryRunEntityResult(templateIdentifier, false, null,
          new DryRunError(type, message));
    }
  }

  /**
   * Error details for dry-run failures or skips.
   */
  public record DryRunError(ErrorType type, String message) {
  }
}
