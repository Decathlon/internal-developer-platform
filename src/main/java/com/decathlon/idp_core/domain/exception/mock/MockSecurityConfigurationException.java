package com.decathlon.idp_core.domain.exception.mock;

/// Infrastructure exception for mock security configuration failures.
///
/// **Purpose:** Raised when the mock security filter chain configuration fails during
/// initialization. This typically indicates issues with Spring Security bean setup or
/// filter chain assembly in the mock authentication environment.
///
/// **Why this exception exists:**
/// - Provides specific, meaningful error context for security configuration failures
/// - Distinguishes infrastructure setup errors from generic failures
/// - Improves debugging by clearly indicating the mock security layer as the source
/// - Follows infrastructure layer pattern of throwing specific exceptions for
///   technical concerns
///
/// **When to throw:**
/// - When HttpSecurity configuration operations fail in mock security setup
/// - During MockJwtAuthenticationFilter chain initialization
///
public class MockSecurityConfigurationException extends RuntimeException {

  /// Constructs a new exception with a message and cause.
  ///
  /// @param message descriptive message about the configuration failure
  /// @param cause the underlying exception that caused this failure
  public MockSecurityConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
