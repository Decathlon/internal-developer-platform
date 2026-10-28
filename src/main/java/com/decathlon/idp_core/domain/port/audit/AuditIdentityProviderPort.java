package com.decathlon.idp_core.domain.port.audit;

/// Provides the authenticated identity associated with an audit revision.
public interface AuditIdentityProviderPort {

  /// Returns the identifier recorded for the authenticated user.
  ///
  /// @return authenticated user identifier
  String getAuthId();

  /// Returns the name associated with the authenticated user.
  ///
  /// @return authenticated user name
  String getName();
}
