package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationTargetJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

import lombok.RequiredArgsConstructor;

/// Custom mapper for Entity persistence layer.
///
/// Handles conversion between domain models and JPA entities, including
/// identifier-to-UUID resolution for relations. Uses custom logic instead of
/// MapStruct because relation mapping requires repository lookups to convert
/// business identifiers to technical UUIDs and vice versa.
@Component
@RequiredArgsConstructor
public class EntityPersistenceMapper {

  private final JpaEntityRepository entityRepository;

  // =========================================================================
  // Entity Mapping
  // =========================================================================

  public Entity toDomain(EntityJpaEntity jpa) {
    if (jpa == null) {
      return null;
    }

    List<Property> properties = jpa.getProperties() != null
        ? jpa.getProperties().stream().map(this::toDomain)
            .sorted(Comparator.comparing(Property::name)).toList()
        : List.of();

    List<Relation> relations = jpa.getRelations() != null
        ? jpa.getRelations().stream().map(this::toDomain)
            .sorted(Comparator.comparing(Relation::name)).toList()
        : List.of();

    return new Entity(jpa.getId(), jpa.getTemplateIdentifier(), jpa.getName(), jpa.getIdentifier(),
        properties, relations);
  }

  public EntityJpaEntity toJpa(Entity domain) {
    if (domain == null) {
      return null;
    }

    // Convert Domain List to JPA Set
    Set<PropertyJpaEntity> properties = domain.properties() != null
        ? domain.properties().stream().map(this::toJpa).collect(Collectors.toSet())
        : new HashSet<>();

    // Convert Domain List to JPA Set
    Set<RelationJpaEntity> relations = domain.relations() != null
        ? domain.relations().stream().map(this::toJpa).collect(Collectors.toSet())
        : new HashSet<>();

    return EntityJpaEntity.builder().id(domain.id()).templateIdentifier(domain.templateIdentifier())
        .name(domain.name()).identifier(domain.identifier()).updatedAt(Instant.now().toEpochMilli())
        .properties(properties).relations(relations).build();
  }

  /// Merges a domain entity into an existing JPA entity, preserving IDs of
  /// existing
  /// properties and relations. This is used for updates to avoid losing
  /// database-generated IDs.
  ///
  /// **Business purpose:** When updating an entity, we want to preserve the IDs
  /// of existing properties
  /// and relations in the database. This method merges the incoming domain entity
  /// into the existing JPA entity,
  /// updating values while keeping IDs intact for matching properties and
  /// relations.
  public EntityJpaEntity toJpaWithMerge(Entity entity, EntityJpaEntity existing) {
    if (entity == null) {
      return null;
    }
    existing.setTemplateIdentifier(entity.templateIdentifier());
    existing.setName(entity.name());
    existing.setIdentifier(entity.identifier());
    existing.setUpdatedAt(Instant.now().toEpochMilli());

    if (entity.properties() == null || entity.properties().isEmpty()) {
      existing.getProperties().clear();
    } else {
      mergeProperties(entity, existing);
    }

    if (entity.relations() == null || entity.relations().isEmpty()) {
      existing.getRelations().clear();
    } else {
      mergeRelations(entity, existing);
    }

    return existing;
  }

  /// Merges properties from the incoming domain entity into the existing JPA
  /// entity.
  /// Removes properties that are no longer present, updates existing ones, and
  /// adds
  /// new properties as needed. This ensures that the JPA entity accurately
  /// reflects the current state of the domain entity.
  private void mergeProperties(final Entity entity, final EntityJpaEntity existing) {
    existing.getProperties().removeIf(existingProp -> entity.properties().stream()
        .noneMatch(p -> p.name().equals(existingProp.getName())));

    for (Property domainProp : entity.properties()) {
      existing.getProperties().stream().filter(p -> p.getName().equals(domainProp.name()))
          .findFirst().ifPresentOrElse(existingProp -> existingProp.setValue(domainProp.value()),
              () -> existing.getProperties().add(toJpa(domainProp)));
    }
  }

  /// Merges relations from the incoming domain entity into the existing JPA
  /// entity.
  /// Removes relations that are no longer present, updates existing ones, and
  /// adds
  /// new relations as needed. This ensures that the JPA entity accurately
  /// reflects the current state of the domain entity.
  private void mergeRelations(final Entity entity, final EntityJpaEntity existing) {

    existing.getRelations().removeIf(existingRel -> entity.relations().stream()
        .noneMatch(r -> r.name().equals(existingRel.getName())));

    for (Relation domainRel : entity.relations()) {
      existing.getRelations().stream().filter(r -> r.getName().equals(domainRel.name())).findFirst()
          .ifPresentOrElse(existingRel -> {
            existingRel.setTargetTemplateIdentifier(domainRel.targetTemplateIdentifier());
            existingRel.setTargetEntities(domainRel.targetEntityIdentifiers() != null
                ? resolveBatchTargetEntities(domainRel.targetTemplateIdentifier(),
                    domainRel.targetEntityIdentifiers())
                : Set.of());
          }, () -> existing.getRelations().add(toJpa(domainRel)));
    }
  }

  // =========================================================================
  // Property Mapping
  // =========================================================================

  public Property toDomain(PropertyJpaEntity jpa) {
    if (jpa == null) {
      return null;
    }

    return new Property(jpa.getId(), jpa.getName(), jpa.getValue());
  }

  public PropertyJpaEntity toJpa(Property domain) {
    if (domain == null) {
      return null;
    }

    return PropertyJpaEntity.builder().id(domain.id()).name(domain.name()).value(domain.value())
        .build();
  }

  // =========================================================================
  // Relation Mapping (with identifier ↔ UUID conversion)
  // =========================================================================

  public Relation toDomain(RelationJpaEntity jpa) {
    if (jpa == null) {
      return null;
    }

    // Pure in-memory transformation from the unified JPA row down to string list
    // Extract the cached text string
    List<String> targetIdentifiers = jpa.getTargetEntities() != null
        ? jpa.getTargetEntities().stream().map(RelationTargetJpaEntity::getTargetEntityIdentifier)
            .filter(Objects::nonNull).sorted().toList()
        : List.of();

    return new Relation(jpa.getId(), jpa.getName(), jpa.getTargetTemplateIdentifier(),
        targetIdentifiers);
  }

  /// Converts domain relation to JPA entity. Resolves business identifiers to
  /// UUIDs by querying the entity repository in a single batch operation. The JPA
  /// entity stores both UUIDs (for graph traversal) and identifiers (for
  /// Java mapping).
  public RelationJpaEntity toJpa(Relation domain) {
    if (domain == null) {
      return null;
    }

    // Batch resolve all target identifiers in a single query, then map in-memory
    Set<RelationTargetJpaEntity> targetEntities = domain.targetEntityIdentifiers() != null
        ? resolveBatchTargetEntities(domain.targetTemplateIdentifier(),
            domain.targetEntityIdentifiers())
        : Set.of();

    return RelationJpaEntity.builder().id(domain.id()).name(domain.name())
        .targetTemplateIdentifier(domain.targetTemplateIdentifier()).targetEntities(targetEntities)
        .build();
  }

  /// Resolves multiple target identifiers in a single batch query, then maps the
  /// results in-memory to RelationTargetJpaEntity records. This prevents N+1
  /// query
  /// patterns when relations have many targets.
  private Set<RelationTargetJpaEntity> resolveBatchTargetEntities(String targetTemplateIdentifier,
      List<String> targetIdentifiers) {
    if (targetIdentifiers == null || targetIdentifiers.isEmpty()) {
      return Set.of();
    }

    // Single batch query to resolve all targets at once
    List<EntityJpaEntity> resolvedEntities = entityRepository
        .findAllByTemplateIdentifierAndIdentifierIn(targetTemplateIdentifier, targetIdentifiers);

    // Map results in-memory, preserving only successfully resolved entities
    return resolvedEntities.stream()
        .map(entity -> new RelationTargetJpaEntity(entity.getId(), entity.getIdentifier()))
        .collect(Collectors.toSet());
  }
}
