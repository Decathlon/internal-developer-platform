package com.decathlon.idp_core.domain.model.entity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// Domain model representing audit information for an [Entity] revision.
///
/// **Business purpose:** Tracks when and who modified an entity throughout its lifecycle.
/// This information is essential for:
/// - Compliance and regulatory requirements
/// - Change tracking and auditability
/// - Debugging and root cause analysis
/// - Historical reconstruction of entity state
///
/// **Ubiquitous language:** An EntityAuditInfo represents a single point-in-time
/// snapshot of entity modification metadata, capturing the revision number, timestamp,
/// and the user responsible for the change.
///
/// @param revisionNumber unique identifier of the revision in the audit log
/// @param revisionDate timestamp when the revision was created
/// @param revisionType type of operation performed (CREATED, UPDATED, DELETED)
/// @param modifiedBy identifier of the user who performed the modification
/// @param snapshot optional snapshot of the entity's state at the time of revision
public record EntityAuditInfo(Number revisionNumber, Instant revisionDate, String revisionType,
    String modifiedBy, EntitySnapshot snapshot) {

  /// Snapshot of an entity's complete state at a specific point in time.
  ///
  /// **Business purpose:** Preserves the full entity state (core data,
  /// properties,
  /// and relations) at the time of an audit revision, enabling historical
  /// reconstruction
  /// and change tracking.
  ///
  /// @param id unique entity UUID from source table
  /// @param templateIdentifier identifier of the entity template this entity
  /// conforms to
  /// @param name human-readable name of the entity
  /// @param identifier identifier of the entity
  /// @param modifiedFlags map of field names to modification status (for example,
  /// "name_mod" -> true)
  /// @param properties list of property snapshots capturing property values at
  /// this revision
  /// @param relations list of relation snapshots capturing relationship targets
  /// at this revision
  public record EntitySnapshot(UUID id, String templateIdentifier, String name, String identifier,
      Map<String, Boolean> modifiedFlags, List<PropertySnapshot> properties,
      List<RelationSnapshot> relations) {
  }

  /// Snapshot of a property's state at a specific point in time during entity
  /// history.
  ///
  /// **Business purpose:** Captures the immutable state of a single entity
  /// property
  /// as it existed at a specific audit revision, enabling point-in-time
  /// reconstruction
  /// of entity data and change tracking.
  ///
  /// @param id unique UUID identifier of the property
  /// @param name name of the property matching a PropertyDefinition in the entity
  /// template
  /// @param value the value of the property as a string (preserves JSON-typed
  /// values as strings)
  /// @param modifiedFlags map of field names to modification status (for example,
  /// "name_mod" -> true, "value_mod" -> false)
  public record PropertySnapshot(UUID id, String name, String value,
      Map<String, Boolean> modifiedFlags) {
  }

  /// Snapshot of a relation's state at a specific point in time during entity
  /// history.
  ///
  /// **Business purpose:** Captures the immutable state of a single entity
  /// relation
  /// (relationship to other entities) as it existed at a specific audit revision,
  /// enabling point-in-time reconstruction of relationships and change tracking.
  ///
  /// @param id unique UUID identifier of the relation
  /// @param name name of the relation matching a RelationDefinition in the entity
  /// template
  /// @param targetTemplateIdentifier identifier of the target entity template
  /// @param targetEntityIdentifiers list of business identifiers of target
  /// entities
  /// @param modifiedFlags map of field names to modification status (for example,
  /// "name_mod" -> true, "target_template_identifier_mod" -> false)
  public record RelationSnapshot(UUID id, String name, String targetTemplateIdentifier,
      List<String> targetEntityIdentifiers, Map<String, Boolean> modifiedFlags) {
    public RelationSnapshot {
      targetEntityIdentifiers = targetEntityIdentifiers == null
          ? List.of()
          : List.copyOf(targetEntityIdentifiers);
    }
  }
}
