package com.decathlon.idp_core.domain.exception.principal;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PRINCIPAL_CREATION_FAILED;

/// Custom exception indicating that principal creation failed due to a transient provider issue.
///
/// **Business purpose:** Represents a transient failure in principal provisioning that does
/// not block IDP Core usage. The system will automatically retry creation on the next request
/// when the principal is accessed (Just-In-Time provisioning retry strategy).
public class PrincipalCreationException extends RuntimeException {

  private final String principalIdentifier;

  /// Constructs a new exception with the principal identifier.
  ///
  /// @param principalIdentifier the identifier of the principal that failed to be
  /// created
  public PrincipalCreationException(String principalIdentifier) {
    super(String.format(PRINCIPAL_CREATION_FAILED, principalIdentifier));
    this.principalIdentifier = principalIdentifier;
  }
}
