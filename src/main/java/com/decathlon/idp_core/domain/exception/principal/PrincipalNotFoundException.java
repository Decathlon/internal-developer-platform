package com.decathlon.idp_core.domain.exception.principal;

/// Custom exception indicating that a principal was not found in the system.
///
/// **Business purpose:** Represents the business rule violation when attempting
/// to access a principal that doesn't exist in the catalog. This exception is
/// thrown to signal that the requested principal could not be located, which may
/// indicate a failure in Just-In-Time (JIT) provisioning or an invalid identifier.
public class PrincipalNotFoundException extends RuntimeException {

  /// Custom exception for principal not found scenarios.
  ///
  /// **Design rationale:** Specific exception enables tailored HTTP status
  /// mapping
  /// in ApiExceptionHandler (404 instead of generic 500).
  public PrincipalNotFoundException(String identifier) {
    super("Principal with identifier '" + identifier
        + "' not found in catalog. JIT provisioning may have failed.");
  }
}
