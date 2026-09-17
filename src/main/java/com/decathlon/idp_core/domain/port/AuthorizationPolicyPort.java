package com.decathlon.idp_core.domain.port;

import java.util.Set;

import com.decathlon.idp_core.domain.model.security.AuthorizationMode;

/// Provides authorization policy configuration to the domain without exposing
/// infrastructure configuration types.
public interface AuthorizationPolicyPort {

  AuthorizationMode mode();

  Set<String> globalPrincipalIdentifiers();
}
