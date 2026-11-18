package com.decathlon.idp_core.domain.exception.authorization;

/// Indicates that a principal is not permitted to perform the requested operation.
public class PrincipalNotAuthorizedException extends RuntimeException {

  private final String identifier;

  /// Creates an authorization failure for the specified principal.
  ///
  /// @param identifier the identifier of the principal denied access
  public PrincipalNotAuthorizedException(String identifier) {
    super("Principal '" + identifier + "' is not authorized to perform this operation");
    this.identifier = identifier;
  }

  /// Returns the identifier of the principal denied access.
  ///
  /// @return the principal identifier
  public String getIdentifier() {
    return identifier;
  }
}
