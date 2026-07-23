package com.decathlon.idp_core.domain.model.entity;

/// Composite key for uniquely identifying an entity across templates.
///
/// **Design:** Encapsulates the composite uniqueness constraint enforced by
/// the database schema. Since the same identifier can exist across different
/// templates, this record ensures correct entity resolution in multi-template
/// scenarios.
///
/// **Business Context:** The database enforces uniqueness on the combination
/// of (templateIdentifier, identifier), not on identifier alone. This allows
/// different entity types to reuse the same identifier value (e.g., "my-service"
/// could exist as both a "service" template entity and a "database" template entity).
///
/// @param templateIdentifier the template scope
/// @param identifier the entity identifier within the template
public record EntityCompositeKey(String templateIdentifier, String identifier) {

  public EntityCompositeKey {
    if (templateIdentifier == null || templateIdentifier.isBlank()) {
      throw new IllegalArgumentException("Template identifier cannot be null or blank");
    }
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("Entity identifier cannot be null or blank");
    }
  }

  /// Creates a composite key from a colon-separated string.
  ///
  /// @param compositeKey string in format "templateIdentifier:identifier"
  /// @return parsed composite key
  /// @throws IllegalArgumentException if format is invalid
  public static EntityCompositeKey fromString(String compositeKey) {
    if (compositeKey == null || !compositeKey.contains(":")) {
      throw new IllegalArgumentException(
          "Composite key must be in format 'templateIdentifier:identifier'");
    }
    String[] parts = compositeKey.split(":", 2);
    return new EntityCompositeKey(parts[0], parts[1]);
  }

  @Override
  public String toString() {
    return templateIdentifier + ":" + identifier;
  }
}
