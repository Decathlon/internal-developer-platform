package com.decathlon.idp_core.domain.exception.entity;

/// Domain exception for audit modification flags retrieval failures.
///
/// **Business purpose:** Represents a non-critical failure when attempting to retrieve
/// modification flags from the audit tables. This is an optional feature for tracking
/// which specific fields were modified in an entity. If flag retrieval fails, the audit
/// history is still available but without the granular field-level change tracking.
///
/// **Why this exception exists:**
/// - Encapsulates modification flag retrieval errors as a domain exception
/// - Allows infrastructure layer to handle flag retrieval failures gracefully
/// - Enables proper logging and monitoring of optional audit features
/// - Prevents complete failure when optional metadata is unavailable
public class AuditModificationFlagsException extends RuntimeException {

  /// Constructs a new exception for modification flags retrieval failure.
  ///
  /// **Why this exists:** Provides context about which entity and revision failed
  /// to retrieve modification flags, aiding in debugging and monitoring.
  ///
  /// @param entityId the UUID of the audited entity
  /// @param revisionNumber the revision number where flag retrieval failed
  /// @param cause the underlying exception that caused the failure
  public AuditModificationFlagsException(String entityId, Number revisionNumber, Throwable cause) {
    super(String.format(
        "Failed to retrieve modification flags for entity '%s' at revision '%d'. "
            + "Modification tracking is optional and audit history remains available.",
        entityId, revisionNumber.longValue()), cause);
  }
}
