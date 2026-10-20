package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

/// Controls whether authenticated principals are created or only linked to existing records.
public enum PrincipalProvisioningMode {
  JIT, LINK_ONLY
}