package com.decathlon.idp_core.domain.port;

import java.util.Map;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

/// Unified strategy contract for webhook security configuration handling.
///
/// This domain port validates the persisted security configuration at creation/update
/// time. Runtime authentication is handled by an infrastructure-specific contract so
/// the domain stays free of transport concerns.
public interface WebhookSecurityStrategy {

  /// Checks if this strategy supports the given security type.
  ///
  /// @param securityType the security type to check
  /// @return true if this strategy handles this security type
  boolean supports(WebhookSecurityType securityType);

  /// Validates the security configuration provided at creation/update time.
  ///
  /// @param config the security configuration map (e.g., username, secret_alias)
  /// @throws
  /// com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException
  /// if validation fails
  void validateConfiguration(Map<String, String> config);

}
