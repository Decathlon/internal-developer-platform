package com.decathlon.idp_core.domain.exception.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_DELETION_BLOCKED;

import java.util.List;

/// Domain exception for blocked Entity deletion due to required relations.
///
/// **Business purpose:** Represents the business rule violation when attempting
/// to delete an Entity that is still referenced by required relations in other entities.
/// This enforces the business invariant that required relations must be satisfied before
/// entity deletion is allowed.
///
/// **Why this exception exists:**
/// - Enforces referential integrity and required relation constraints
/// - Prevents dangling required references that would leave entities in invalid states
/// - Provides detailed context about which entities/relations block the deletion
/// - Guides users on how to resolve the blocking constraint (update template or remove required flag)
public class EntityDeletionBlockedException extends RuntimeException {

  /// Constructs a new exception with entity and blocking relation details.
  ///
  /// **Why this exists:** Provides comprehensive error message that includes:
  /// - The entity being deleted (identifier and template)
  /// - The list of entities that have required relations to it
  /// - Actionable guidance on how to resolve the issue
  ///
  /// @param templateIdentifier the template identifier of the entity being
  /// deleted
  /// @param entityIdentifier the identifier of the entity being deleted
  /// @param blockingEntities list of entity identifiers that have required
  /// relations to the deleted entity
  public EntityDeletionBlockedException(String templateIdentifier, String entityIdentifier,
      List<String> blockingEntities) {
    super(String.format(ENTITY_DELETION_BLOCKED, entityIdentifier, templateIdentifier,
        String.join(", ", blockingEntities)));
  }

}
