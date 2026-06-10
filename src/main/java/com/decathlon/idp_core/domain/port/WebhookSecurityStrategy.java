package com.decathlon.idp_core.domain.port;

import java.util.Map;

/// Unified strategy contract for webhook security handling.
///
/// This interface consolidates two responsibilities that were previously scattered:
/// 1. Validating security configuration at creation/update time
/// 2. Validating incoming webhook requests at runtime
///
/// Implementations should focus on security logic without side effects.
public interface WebhookSecurityStrategy {

  /// Checks if this strategy supports the given security type.
  ///
  /// @param securityType the security type to check (e.g., "BASIC_AUTH",
  /// "HMAC_SHA256")
  /// @return true if this strategy handles this security type
  boolean supports(String securityType);

  /// Validates the security configuration provided at creation/update time.
  ///
  /// @param config the security configuration map (e.g., username, secret_alias)
  /// @throws
  /// com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException
  /// if validation fails
  void validateConfiguration(Map<String, String> config);

}
