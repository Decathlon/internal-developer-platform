package com.decathlon.idp_core.domain.model.principal;

/// Enumeration defining the type of principal (actor) in the system.
///
/// **Business meaning:**
/// - HUMAN: A human user authenticated via OAuth2/OIDC
/// - SERVICE_ACCOUNT: A machine client (webhook, API connector, service token)
public enum PrincipalKind {
  HUMAN, SERVICE_ACCOUNT
}
