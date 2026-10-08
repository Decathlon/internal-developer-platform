package com.decathlon.idp_core.domain.model.entity;

import java.util.List;

/// Nullable fields representing the changes in a partial entity update.
///
/// A null field means that the existing value must be preserved. Validation is
/// performed on the complete [Entity] after this patch has been applied.
public record EntityPatch(String name, List<Property> properties, List<Relation> relations) {

  /// Creates a patch from the entity-shaped payload produced by ingestion.
  ///
  /// @param entity entity payload containing the fields to apply
  /// @return patch containing the entity's mutable fields
  public static EntityPatch fromEntity(Entity entity) {
    return new EntityPatch(entity.name(), entity.properties(), entity.relations());
  }
}
