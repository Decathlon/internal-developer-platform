package com.decathlon.idp_core.domain.model.entity;

import java.util.Objects;

/**
 * Composite key for uniquely identifying an entity across templates. Since the
 * same identifier can exist in different templates, we need both fields.
 */
public record EntityCompositeKey(String templateIdentifier, String identifier) {
  public static EntityCompositeKey fromString(String compositeKey) {
    String[] parts = compositeKey.split(":", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid composite key format: " + compositeKey);
    }
    return new EntityCompositeKey(parts[0], parts[1]);
  }

  @Override
  public String toString() {
    return templateIdentifier + ":" + identifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    EntityCompositeKey that = (EntityCompositeKey) o;
    return Objects.equals(templateIdentifier, that.templateIdentifier)
        && Objects.equals(identifier, that.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(templateIdentifier, identifier);
  }
}
