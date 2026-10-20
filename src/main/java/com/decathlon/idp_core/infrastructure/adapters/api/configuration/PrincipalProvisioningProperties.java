package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Principal provisioning settings bound from `app.security.principal-provisioning`.
@ConfigurationProperties(prefix = "app.security.principal-provisioning")
public record PrincipalProvisioningProperties(PrincipalProvisioningMode mode) {

  public PrincipalProvisioningProperties {
    mode = mode != null ? mode : PrincipalProvisioningMode.JIT;
  }
}