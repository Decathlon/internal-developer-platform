package com.decathlon.idp_core.infrastructure.adapters.api.exception;

/// Indicates that an authenticated principal is missing its configured identifier claim.
public class MissingPrincipalIdentifierException extends RuntimeException {

  private final String claimName;

  /// Creates a failure for a missing or blank principal identifier.
  ///
  /// @param claimName configured claim expected to identify the principal
  public MissingPrincipalIdentifierException(String claimName) {
    super("Required principal identifier claim '" + claimName + "' is missing or blank");
    this.claimName = claimName;
  }

  /// Returns the configured claim that was missing or blank.
  ///
  /// @return principal identifier claim name
  public String getClaimName() {
    return claimName;
  }
}
