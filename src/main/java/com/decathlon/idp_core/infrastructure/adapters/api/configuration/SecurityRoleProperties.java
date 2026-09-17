package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration properties for security role assignment.
///
/// **Business purpose:** Externalizes baseline role configuration to enable
/// easy transition from V1 (global admin for all) to V2 (granular RBAC).
///
/// **Usage:** Bind via `@EnableConfigurationProperties` in SecurityConfiguration.
@ConfigurationProperties(prefix = "app.security.roles")
public record SecurityRoleProperties(String baselineRole) {

  /// Every authenticated principal is a reader unless the catalog marks it as an
  /// admin.
  public SecurityRoleProperties {
    if (baselineRole == null || baselineRole.isBlank()) {
      baselineRole = "ROLE_READER";
    }
  }
}
