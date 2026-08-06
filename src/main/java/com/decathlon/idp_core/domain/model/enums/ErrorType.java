package com.decathlon.idp_core.domain.model.enums;

/// Types of errors that can occur during a dynamic mapping dry-run.
///
/// Used by `DryRunResult.DryRunError` to categorize the failure reason.
public enum ErrorType {

  /// JSLT expression evaluation error (syntax or evaluation failure).
  JSLT_ERROR,

  /// Entity validation error (required fields, property type mismatch, etc.).
  VALIDATION_ERROR,

  /// Mapping was skipped because the filter expression returned false or null.
  SKIPPED
}
