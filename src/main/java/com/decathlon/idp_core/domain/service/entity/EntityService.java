package com.decathlon.idp_core.domain.service.entity;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityDeletionBlockedException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.EntityQueryParserService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;

import lombok.RequiredArgsConstructor;

/// Domain service orchestrating [Entity] business operations and validations.
///
/// **Business purpose:** Coordinates entity lifecycle management while enforcing
/// business rules and maintaining data consistency across the entity-template
/// domain. Serves as the primary entry point for entity operations from
/// application layer.
///
/// **Key responsibilities:**
/// - Entity retrieval with template validation
/// - Entity creation with business rule enforcement
/// - Entity data integrity validation (entity, properties, relations)
/// - Entity summary generation for efficient queries
@Service
@Validated
@RequiredArgsConstructor
public class EntityService {
  private final EntityRepositoryPort entityRepository;
  private final EntityValidationService entityValidationService;
  private final EntityTemplateValidationService entityTemplateValidationService;
  private final EntityTemplateService entityTemplateService;
  private final EntityQueryParserService entityQueryParserService;

  /// Retrieves entities filtered by template with optional query filter.
  ///
  /// **Contract:** Returns paginated entities conforming to the specified
  /// template
  /// that additionally satisfy all criteria in filter (when provided). Template
  /// existence is validated first. When filter is null or empty, the result
  /// includes all entities for the template.
  ///
  /// @param pageable pagination configuration for large entity sets
  /// @param templateIdentifier business identifier of the entity template
  /// @param entityFilter the parsed query filter; null or [EntityFilter#empty()]
  /// for no filtering
  /// @return paginated entities matching the template and all filter criteria
  /// @throws EntityTemplateNotFoundException when template doesn't exist
  @Transactional
  public Page<Entity> getEntitiesByTemplateIdentifier(Pageable pageable, String templateIdentifier,
      EntityFilter entityFilter) {
    EntityTemplate template = entityTemplateService
        .getEntityTemplateByIdentifier(templateIdentifier);
    EntityFilter filter = entityFilter != null ? entityFilter : EntityFilter.empty();
    entityQueryParserService.validateFilterPropertyTypes(filter, template);
    return entityRepository.findByTemplateIdentifierWithFilter(templateIdentifier, filter,
        pageable);
  }

  /// Provides lightweight entity summaries for efficient bulk operations.
  ///
  /// **Contract:** Returns summary projections without full entity data,
  /// optimized
  /// for UI lists and relationship resolution scenarios.
  ///
  /// @param identifiers business identifiers of entities to summarize
  /// @return lightweight entity summaries for the specified identifiers
  public List<EntitySummary> getEntitiesSummariesByIdentifiers(List<String> identifiers) {
    return entityRepository.findByIdentifierIn(identifiers);
  }

  /// Retrieves a specific entity with template and entity validation.
  ///
  /// **Contract:** Returns the entity identified by both template and entity
  /// identifiers. Validates template existence first, then entity existence,
  /// ensuring referential integrity.
  ///
  /// @param templateIdentifier business identifier of the entity template
  /// @param entityIdentifier unique business identifier of the entity within
  /// template
  /// @return the entity matching both identifiers
  /// @throws EntityTemplateNotFoundException when template doesn't exist
  /// @throws EntityNotFoundException when entity doesn't exist
  @Transactional
  public Entity getEntityByTemplateIdentifierAndIdentifier(String templateIdentifier,
      String entityIdentifier) {
    entityTemplateValidationService.validateTemplateExists(templateIdentifier);
    return retrieveEntity(templateIdentifier, entityIdentifier);
  }

  /// Creates and persists a new entity with business validation.
  ///
  /// **Contract:** Resolves the referenced template (single round-trip — combined
  /// existence check and fetch), enforces entity identifier uniqueness within the
  /// template scope, then validates entity/property data integrity against the
  /// resolved template before persisting.
  ///
  /// @param entity validated entity to create and persist
  /// @return the persisted entity with generated identifiers
  /// @throws EntityTemplateNotFoundException when the referenced template doesn't
  /// exist
  /// @throws EntityAlreadyExistsException when an entity with the same
  /// identifier already exists for this
  /// template
  /// @throws EntityValidationException when entity, property, or relation
  /// data is invalid
  @Transactional
  public Entity createEntity(@Valid Entity entity) {
    EntityTemplate template = entityTemplateService
        .getEntityTemplateByIdentifier(entity.templateIdentifier());
    entityValidationService.validateForCreation(entity, template);
    return entityRepository.save(entity);
  }

  /// Updates an existing entity identified by template and entity identifiers.
  ///
  /// **Contract:** Validates template existence, then entity existence within the
  /// template scope. Validates updated entity data against the template
  /// constraints
  /// before persisting changes.
  ///
  /// @param templateIdentifier template identifier from the request path
  /// @param entityIdentifier entity identifier from the request path
  /// @param entity validated entity payload
  /// @return persisted updated entity
  /// @throws EntityTemplateNotFoundException when template doesn't exist
  /// @throws EntityNotFoundException when target entity doesn't exist
  /// @throws EntityValidationException when payload violates template constraints
  @Transactional
  public Entity updateEntity(String templateIdentifier, String entityIdentifier,
      @Valid Entity entity) {
    EntityTemplate template = entityTemplateService
        .getEntityTemplateByIdentifier(templateIdentifier);
    Entity existingEntity = retrieveEntity(templateIdentifier, entityIdentifier);

    Entity entityToSave = new Entity(existingEntity.id(), templateIdentifier, entity.name(),
        entityIdentifier, entity.properties(), entity.relations());

    entityValidationService.validateForUpdate(entityToSave, template);
    return entityRepository.save(entityToSave);
  }

  /// Deletes an existing entity identified by template and entity identifiers.
  ///
  /// **Contract:** Validates the template and entity exist, cleans up relations
  /// in parent
  /// entities that reference the deleted entity (to prevent dangling references),
  /// and then removes it.
  ///
  /// @param templateIdentifier template identifier from the request path
  /// @param entityIdentifier entity identifier from the request path
  /// @throws EntityTemplateNotFoundException when template doesn't exist
  /// @throws EntityNotFoundException when target entity doesn't exist
  @Transactional
  public void deleteEntity(String templateIdentifier, String entityIdentifier) {
    entityTemplateValidationService.validateTemplateExists(templateIdentifier);
    Entity entityToDelete = retrieveEntity(templateIdentifier, entityIdentifier);
    removedRelationRelated(entityToDelete);
    entityRepository.deleteByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier);
  }

  /// Cleans up relations in parent entities that reference the deleted entity to
  /// maintain referential integrity.
  ///
  /// **Contract:** Finds all entities that have relations targeting the deleted
  /// entity. First validates that no required single-target relations would be
  /// violated,
  /// throwing EntityDeletionBlockedException if any are found. Then removes those
  /// relations
  /// to prevent dangling references. This is necessary to maintain data integrity
  /// after an entity is deleted.
  ///
  /// @param entityToDelete the identifier of the entity that was deleted, used
  /// to find and clean up related entities
  /// @throws EntityDeletionBlockedException if the entity is referenced by
  /// required relations
  private void removedRelationRelated(final Entity entityToDelete) {
    List<Entity> parentEntities = entityRepository.findEntitiesRelated(entityToDelete.identifier());

    Map<String, EntityTemplate> parentTemplates = parentEntities.stream()
        .map(Entity::templateIdentifier).distinct()
        .collect(toMap(id -> id, entityTemplateService::getEntityTemplateByIdentifier));

    hasBlockingEntities(entityToDelete, parentEntities, parentTemplates);

    parentEntities.forEach(parent -> {
      EntityTemplate parentTemplate = parentTemplates.get(parent.templateIdentifier());
      Entity cleanedParent = cleanUpRelations(parent, parentTemplate, entityToDelete.identifier());
      if (!cleanedParent.relations().equals(parent.relations())) {
        entityRepository.save(cleanedParent);
      }
    });
  }

  /// Validates that no parent entities have required relations to the entity
  /// being deleted.
  ///
  /// **Contract:** Iterates through the parent entities and their templates to
  /// check if any
  /// relations would be left empty and are defined as required and single-target.
  /// If such blocking relations are found,
  /// an EntityDeletionBlockedException is thrown with details about the blocking
  /// entities and relations.
  /// This ensures that entities with required dependencies cannot be deleted
  /// without first addressing those dependencies, maintaining referential
  /// integrity
  /// and preventing invalid states in the domain model.
  ///
  /// @param entityToDelete the entity that is being deleted, used for context in
  /// error messages
  /// @param parentEntities the list of entities that have relations targeting the
  /// entity being deleted
  /// @param parentTemplates a map of template identifiers to their corresponding
  /// EntityTemplate instances for the parent entities, used to check relation
  /// definitions
  /// @throws EntityDeletionBlockedException if any parent entities have required
  /// relations to the entity being deleted, providing details about the blocking
  /// entities and relations
  private void hasBlockingEntities(final Entity entityToDelete, final List<Entity> parentEntities,
      final Map<String, EntityTemplate> parentTemplates) {
    List<String> blockingEntities = parentEntities.stream().map(parent -> {
      EntityTemplate parentTemplate = parentTemplates.get(parent.templateIdentifier());
      String blockingNames = getBlockingRelationNames(parent, parentTemplate,
          entityToDelete.identifier());

      if (!blockingNames.isEmpty()) {
        return String.format("'%s' (template: '%s', relation(s): %s)", parent.identifier(),
            parent.templateIdentifier(), blockingNames);
      }
      return null;
    }).filter(Objects::nonNull).toList();

    if (!blockingEntities.isEmpty()) {
      throw new EntityDeletionBlockedException(entityToDelete.templateIdentifier(),
          entityToDelete.identifier(), blockingEntities);
    }
  }

  /// Gets the names of all relations that would block deletion.
  ///
  /// @param linkedEntity the entity whose relations are being checked
  /// @param parentTemplate the template of the parent entity
  /// @param entityIdentifierToRemove the identifier being removed
  /// @return comma-separated list of blocking relation names
  private String getBlockingRelationNames(final Entity linkedEntity,
      final EntityTemplate parentTemplate, final String entityIdentifierToRemove) {

    return linkedEntity.relations().stream()
        .filter(relation -> isBlockingRelation(relation, parentTemplate, entityIdentifierToRemove))
        .map(relation -> "'" + relation.name() + "'").collect(joining(", "));
  }

  private boolean isBlockingRelation(final Relation relation, final EntityTemplate parentTemplate,
      final String idToRemove) {
    var targets = relation.targetEntityIdentifiers();
    if (targets == null || !targets.contains(idToRemove)) {
      return false;
    }
    boolean becomesEmpty = targets.stream().allMatch(idToRemove::equals);
    if (!becomesEmpty) {
      return false;
    }
    var definition = getRelationDefinition(parentTemplate, relation.name());
    return definition != null && definition.required();
  }

  /// Removes the specified entity identifier from the relations of the parent
  /// entity, ensuring that required single-target relations are not left empty.
  ///
  /// **Contract:** Iterates through the relations of the parent entity, removing
  /// the target identifier from any relation
  /// that contains it. If a relation becomes empty as a result, checks the
  /// relation definition to determine if it is required and single-target;
  /// if so, the relation is not removed to avoid leaving the parent entity in an
  /// invalid state.
  ///
  /// @param parent the entity whose relations are being cleaned up
  /// @param parentTemplate the template of the parent entity, used to check
  /// relation definitions
  /// @param entityIdentifierToRemove the identifier of the entity being deleted,
  /// which should be removed from the relations of the parent entity
  /// @return a new Entity instance with updated relations, reflecting the removal
  /// of the specified entity identifier
  /// **Note:** This method assumes that the parent entity and its template are
  /// valid and exist, as it is called in the context of cleaning up after a known
  /// entity deletion.
  /// It focuses solely on relation cleanup and does not perform additional
  /// validations or checks beyond what is necessary for maintaining referential
  /// integrity.
  private Entity cleanUpRelations(final Entity parent, final EntityTemplate parentTemplate,
      final String entityIdentifierToRemove) {
    List<Relation> updatedRelations = new ArrayList<>();
    List<Relation> currentRelations = parent.relations() != null ? parent.relations() : List.of();
    currentRelations
        .forEach(relation -> retrieveAndCleanTargetEntitiesAgainstRelation(parentTemplate,
            entityIdentifierToRemove, relation, updatedRelations));

    return new Entity(parent.id(), parent.templateIdentifier(), parent.name(), parent.identifier(),
        parent.properties(), updatedRelations);
  }

  private void retrieveAndCleanTargetEntitiesAgainstRelation(final EntityTemplate parentTemplate,
      final String entityIdentifierToRemove, final Relation relation,
      final List<Relation> updatedRelations) {
    List<String> currentTargets = relation.targetEntityIdentifiers() != null
        ? relation.targetEntityIdentifiers()
        : List.of();

    if (!currentTargets.contains(entityIdentifierToRemove)) {
      updatedRelations.add(relation);
      return;
    }

    cleanLinkedRelation(parentTemplate, entityIdentifierToRemove, relation, currentTargets,
        updatedRelations);
  }

  private void cleanLinkedRelation(final EntityTemplate parentTemplate,
      final String entityIdentifierToRemove, final Relation relation,
      final List<String> currentTargets, final List<Relation> updatedRelations) {
    List<String> updatedTargets = currentTargets.stream()
        .filter(target -> !entityIdentifierToRemove.equals(target)).toList();
    if (updatedTargets.isEmpty()) {
      RelationDefinition definition = getRelationDefinition(parentTemplate, relation.name());
      if (definition != null && definition.required()) {
        return;
      }
    }
    updatedRelations.add(new Relation(relation.id(), relation.name(),
        relation.targetTemplateIdentifier(), updatedTargets));
  }

  private RelationDefinition getRelationDefinition(final EntityTemplate template,
      final String relationName) {
    if (template.relationsDefinitions() == null) {
      return null;
    }
    return template.relationsDefinitions().stream()
        .filter(definition -> relationName.equals(definition.name())).findFirst().orElse(null);
  }

  /// Validates that an entity with the specified template and identifier exists,
  /// throwing an exception if not found.
  ///
  /// **Contract:** Checks the existence of the entity using the repository. If
  /// the entity is
  /// not found, throws an EntityNotFoundException with the relevant template and
  /// entity identifiers for error reporting.
  ///
  /// @param templateIdentifier the identifier of the template to which the entity
  /// belongs, used for lookup and error reporting
  /// @param entityIdentifier the unique identifier of the entity within the
  /// template, used for lookup and error reporting
  /// @return the Entity instance that matches the specified template and entity
  /// identifiers, if found.
  /// @throws EntityNotFoundException if no entity matching the template and
  /// entity identifiers is found in the repository, indicating that the entity
  /// does not exist
  private Entity retrieveEntity(final String templateIdentifier, final String entityIdentifier) {
    return entityRepository
        .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
        .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));
  }
}
