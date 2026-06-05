package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import java.util.List;
import java.util.Objects;

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
        ? jpa.getProperties().stream().map(this::toDomain).toList()
        : List.of();

    List<Relation> relations = jpa.getRelations() != null
        ? jpa.getRelations().stream().map(this::toDomain).toList()
        : List.of();

    return new Entity(jpa.getId(), jpa.getTemplateIdentifier(), jpa.getName(), jpa.getIdentifier(),
        properties, relations);
  }

  public EntityJpaEntity toJpa(Entity domain) {
    if (domain == null) {
      return null;
    }

    List<PropertyJpaEntity> properties = domain.properties() != null
        ? domain.properties().stream().map(this::toJpa).toList()
        : List.of();

    List<RelationJpaEntity> relations = domain.relations() != null
        ? domain.relations().stream().map(this::toJpa).toList()
        : List.of();

    return EntityJpaEntity.builder().id(domain.id()).templateIdentifier(domain.templateIdentifier())
        .name(domain.name()).identifier(domain.identifier()).properties(properties)
        .relations(relations).build();
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

    // Pure in-memory transformation from the unified JPA row down to your string
    // list
    List<String> targetIdentifiers = jpa.getTargetEntities() != null
        ? jpa.getTargetEntities().stream().map(RelationTargetJpaEntity::getTargetEntityIdentifier) // Extract
                                                                                                   // the
                                                                                                   // cached
                                                                                                   // text
                                                                                                   // string
            .filter(Objects::nonNull).toList()
        : List.of();

    return new Relation(jpa.getId(), jpa.getName(), jpa.getTargetTemplateIdentifier(),
        targetIdentifiers); // Matches your current domain model signature perfectly!
  }

  /// Converts domain relation to JPA entity. Resolves business identifiers to
  /// UUIDs by querying the entity repository. The JPA entity stores only UUIDs;
  /// identifiers are not persisted in the infrastructure layer.
  public RelationJpaEntity toJpa(Relation domain) {
    if (domain == null) {
      return null;
    }

    // Look up matching entities to bind both fields concurrently into single table
    // rows
    List<RelationTargetJpaEntity> targetEntities = domain.targetEntityIdentifiers() != null
        ? domain.targetEntityIdentifiers().stream().map(identifier -> entityRepository
            .findByTemplateIdentifierAndIdentifier(domain.targetTemplateIdentifier(), identifier)
            .map(entity -> new RelationTargetJpaEntity(entity.getId(), // The binary UUID used for
                                                                       // Graph CTE crawls
                entity.getIdentifier() // The immutable string cached for Java mapping
            )).orElse(null)).filter(Objects::nonNull).toList()
        : List.of();

    // Return the unified entity mapping to prevent column nullability errors
    return RelationJpaEntity.builder().id(domain.id()).name(domain.name())
        .targetTemplateIdentifier(domain.targetTemplateIdentifier()).targetEntities(targetEntities) // The
                                                                                                    // single
                                                                                                    // unified
                                                                                                    // collection
                                                                                                    // table
        .build();
  }
}
