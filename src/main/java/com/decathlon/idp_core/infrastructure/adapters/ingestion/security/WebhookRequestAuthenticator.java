package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

import java.util.Map;

/// Infrastructure contract for runtime webhook request authentication.
///
/// This adapter-facing contract consumes inbound request data so security strategies can
/// validate HTTP headers and payload bytes without exposing transport details in the
/// domain layer.
public interface WebhookRequestAuthenticator {
  /// Validates an incoming webhook request at runtime.
  ///
  /// @param headers the inbound HTTP headers
  /// @param rawPayload the exact inbound payload bytes (before decoding)
  /// @param config the persisted security configuration
  /// @throws
  /// com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException
  /// when authentication is missing or malformed (401)
  /// @throws
  /// com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException
  /// when authentication is provided but rejected (403)
  void validateRequest(Map<String, Object> headers, byte[] rawPayload, Map<String, String> config);
}
