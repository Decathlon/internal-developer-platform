package com.decathlon.idp_core.domain.exception.entity_template;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;

/// Domain exception for missing [EntityTemplate] business entities.
///
/// **Business purpose:** Represents the business rule violation when attempting
/// to access an EntityTemplate that doesn't exist in the system. This is a
/// critical business error since entities cannot be created without valid templates.
///
/// **Exception design rationale:**
/// - Multiple constructors support different lookup scenarios (ID, identifier, field-based)
/// - Meaningful error messages aid in debugging and API error responses
/// - Domain-level exception keeps business logic separate from HTTP concerns
///
/// **Usage patterns:**
/// - Template validation before entity operations
/// - Template-based entity queries
/// - Template management operations
public class EntityTemplateInUseByWebhookMappingException extends RuntimeException {

  /// Constructs a new exception with a custom error message.
  ///
  /// **Why this exists:** Allows for specific error messages that provide more
  /// context about the search criteria or operation that failed.
  ///
  /// @param message the detail message explaining what was not found
  public EntityTemplateInUseByWebhookMappingException(String message) {
    super(message);
  }

}
