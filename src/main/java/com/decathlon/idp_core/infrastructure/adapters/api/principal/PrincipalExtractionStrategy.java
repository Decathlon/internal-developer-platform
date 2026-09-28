package com.decathlon.idp_core.infrastructure.adapters.api.principal;

import org.springframework.security.core.Authentication;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;

/// Strategy interface for extracting domain PrincipalInfo from various types of Authentication tokens.
///
/// **Business purpose:** This interface allows for different implementations to handle the extraction of principal information
/// from various authentication mechanisms, such as OAuth2, JWT, or API keys, enabling a flexible and extensible security architecture.
public interface PrincipalExtractionStrategy {

  /// Determines if this strategy knows how to handle the given Authentication
  /// token
  boolean supports(Authentication authentication);

  /// Extracts the domain PrincipalInfo from the specific token
  PrincipalInfo extract(Authentication authentication);
}
