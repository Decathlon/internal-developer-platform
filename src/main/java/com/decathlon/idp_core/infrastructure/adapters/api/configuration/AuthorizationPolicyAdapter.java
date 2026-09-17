package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.security.AuthorizationMode;
import com.decathlon.idp_core.domain.port.AuthorizationPolicyPort;

import lombok.RequiredArgsConstructor;

/// Adapts Spring configuration properties to the domain authorization port.
@Component
@RequiredArgsConstructor
public class AuthorizationPolicyAdapter implements AuthorizationPolicyPort {

  private final AuthorizationProperties properties;

  @Override
  public AuthorizationMode mode() {
    return properties.mode();
  }

  @Override
  public Set<String> globalPrincipalIdentifiers() {
    return properties.globalPrincipalIdentifiers();
  }
}
