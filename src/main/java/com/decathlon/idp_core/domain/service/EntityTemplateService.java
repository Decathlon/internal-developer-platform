package com.decathlon.idp_core.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/// Domain service orchestrating [EntityTemplate] business operations and lifecycle management.
///
/// **Business purpose:** Coordinates template creation, validation, and retrieval while enforcing
/// business rules around template uniqueness and structure integrity. Serves as the primary
/// entry point for template operations from application layer.
///
/// **Key responsibilities:**
/// - Template retrieval with pagination for management interfaces
/// - Template creation with business rule validation and duplicate prevention
/// - Template deletion with referential integrity checks
/// - Property and relation rule merging for template updates
/// - Domain-specific exception handling and error reporting
@Service
@Validated
@RequiredArgsConstructor
public class EntityTemplateService {

    private final EntityTemplateRepositoryPort entityTemplateRepositoryPort;

    /// Retrieves paginated entity templates for management interface display.
    ///
    /// **Contract:** Returns templates with pagination metadata for efficient UI rendering.
    /// Supports sorting and filtering through Spring's Pageable interface for flexible
    /// template browsing and administration.
    ///
    /// @param pageable pagination configuration including page size, number, and sorting
    /// @return paginated template results with metadata
    public Page<EntityTemplate> getEntityTemplates(Pageable pageable) {
        return entityTemplateRepositoryPort.findAll(pageable);
    }

    /// Retrieves a specific entity template by business identifier.
    ///
    /// **Contract:** Performs exact match lookup for template identification.
    /// Case-sensitive matching ensures precise template resolution for entity operations.
    ///
    /// @param identifier unique business identifier of the template
    /// @return the matching [EntityTemplate]
    /// @throws EntityTemplateNotFoundException when template doesn't exist
    public EntityTemplate getEntityTemplateByIdentifier(String identifier) {
        return entityTemplateRepositoryPort.findByIdentifier(identifier)
                .orElseThrow(() -> new EntityTemplateNotFoundException("identifier", identifier));
    }

    /// Creates and persists a new entity template.
    ///
    /// **Contract:** Validates the provided `EntityTemplate` and enforces uniqueness
    /// constraints on both `identifier` and `name` when present. If validation passes,
    /// the template is persisted and the persisted instance (including any generated
    /// identifiers) is returned.
    ///
    /// **Business rules enforced:**
    /// - If `identifier` is provided it must not already exist in the system.
    /// - If `name` is provided it must not already exist in the system.
    ///
    /// @param entityTemplate validated template to create and persist
    /// @return the persisted template with generated identifiers
    /// @throws EntityTemplateAlreadyExistsException when identifier already exists
    /// @throws EntityTemplateNameAlreadyExistsException when name already exists
    @Transactional
    public EntityTemplate createEntityTemplate(@Valid EntityTemplate entityTemplate) {
        if (entityTemplate.identifier() != null &&
                entityTemplateRepositoryPort.existsByIdentifier(entityTemplate.identifier())) {
            throw new EntityTemplateAlreadyExistsException(entityTemplate.identifier());
        }
        if (entityTemplate.name() != null &&
                entityTemplateRepositoryPort.existsByName(entityTemplate.name())) {
            throw new EntityTemplateNameAlreadyExistsException(entityTemplate.name());
        }
        return entityTemplateRepositoryPort.save(entityTemplate);
    }

    /// Updates an existing entity template using full replacement with smart merging.
    ///
    /// **Contract:** Replaces the template's scalar fields (identifier, name, description) with the
    /// incoming values, while performing an intelligent merge on nested collections
    /// (properties and relations). Matching children (by name) preserve their existing UUIDs
    /// so the persistence layer treats them as updates rather than delete-and-recreate,
    /// avoiding unnecessary orphan removal and re-insertion.
    ///
    /// **Business rules enforced:**
    /// - The target template must already exist (looked up by the path `identifier`).
    /// - If the caller changes the identifier, the new value must not collide with another template.
    /// - Property and relation definitions are merged by name:
    ///   - *Matched by name* → existing ID is preserved, other fields are overwritten.
    ///   - *Not matched* → treated as a new definition (no ID yet).
    ///   - *Missing from update* → removed (handled downstream by the persistence adapter).
    ///
    /// @param identifier current business identifier of the template to update
    /// @param updatedTemplate validated template carrying the desired state
    /// @return the persisted template after merge, with generated or preserved identifiers
    /// @throws EntityTemplateNotFoundException when no template matches `identifier`
    /// @throws EntityTemplateAlreadyExistsException when renaming would cause a duplicate
    @Transactional
    public EntityTemplate putEntityTemplate(String identifier, @Valid EntityTemplate updatedTemplate) {
        EntityTemplate existingTemplate = getEntityTemplateByIdentifier(identifier);

        if (!identifier.equals(updatedTemplate.identifier()) &&
                entityTemplateRepositoryPort.existsByIdentifier(updatedTemplate.identifier())) {
            throw new EntityTemplateAlreadyExistsException(updatedTemplate.identifier());
        }

        if (updatedTemplate.name() != null &&
               !Objects.equals(existingTemplate.name(), updatedTemplate.name()) &&
               entityTemplateRepositoryPort.existsByName(updatedTemplate.name())) {
           throw new EntityTemplateNameAlreadyExistsException(updatedTemplate.name());
        }

        EntityTemplate mergedTemplate = new EntityTemplate(
                existingTemplate.id(),
                updatedTemplate.identifier(),
                updatedTemplate.name(),
                updatedTemplate.description(),
                mergePropertyDefinitions(existingTemplate.propertiesDefinitions(),
                        updatedTemplate.propertiesDefinitions()),
                mergeRelationDefinitions(existingTemplate.relationsDefinitions(),
                        updatedTemplate.relationsDefinitions())
        );

        return entityTemplateRepositoryPort.save(mergedTemplate);
    }

    private List<PropertyDefinition> mergePropertyDefinitions(
            List<PropertyDefinition> existing,
            List<PropertyDefinition> updated) {

        if (existing == null) existing = new ArrayList<>();
        if (updated == null) return existing;

        Map<String, PropertyDefinition> existingMap = existing.stream()
                .collect(Collectors.toMap(PropertyDefinition::name, Function.identity()));

        List<PropertyDefinition> result = new ArrayList<>();

        for (PropertyDefinition prop : updated) {
            PropertyDefinition existingProp = existingMap.get(prop.name());
            if (existingProp != null) {
                // Records are immutable - create a new instance
                PropertyDefinition merged = new PropertyDefinition(
                        existingProp.id(),
                        prop.name(),
                        prop.description(),
                        prop.type(),
                        prop.required(),
                        mergePropertyRules(existingProp.rules(), prop.rules())
                );
                result.add(merged);
            } else {
                result.add(prop);
            }
        }

        return result;
    }

    private PropertyRules mergePropertyRules(PropertyRules existingRules, PropertyRules newRules) {
        if (newRules == null) {
            return existingRules;
        }
        if (existingRules == null) {
            return newRules;
        }

        // Records are immutable - create a new instance
        return new PropertyRules(
                existingRules.id(),
                newRules.format(),
                newRules.enumValues(),
                newRules.regex(),
                newRules.maxLength(),
                newRules.minLength(),
                newRules.maxValue(),
                newRules.minValue()
        );
    }

    private List<RelationDefinition> mergeRelationDefinitions(
            List<RelationDefinition> existing,
            List<RelationDefinition> updated) {

        if (existing == null) existing = new ArrayList<>();
        if (updated == null) return existing;

        Map<String, RelationDefinition> existingMap = existing.stream()
                .collect(Collectors.toMap(RelationDefinition::name, Function.identity()));

        List<RelationDefinition> result = new ArrayList<>();

        for (RelationDefinition rel : updated) {
            RelationDefinition existingRel = existingMap.get(rel.name());
            if (existingRel != null) {
                // Records are immutable - create a new instance
                RelationDefinition merged = new RelationDefinition(
                        existingRel.id(),
                        rel.name(),
                        rel.targetEntityIdentifier(),
                        rel.required(),
                        rel.toMany()
                );
                result.add(merged);
            } else {
                result.add(rel);
            }
        }

        return result;
    }


    /// Deletes an entity template by business identifier with existence validation.
    ///
    /// **Contract:** Validates template existence before deletion to ensure referential
    /// integrity. Deletion cascades through persistence layer according to configured
    /// relationships. This operation is irreversible once committed.
    ///
    /// @param identifier unique business identifier of template to delete
    /// @throws EntityTemplateNotFoundException when template doesn't exist
    @Transactional
    public void deleteEntityTemplate(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Template identifier must not be null");
        }
        if (!entityTemplateRepositoryPort.existsByIdentifier(identifier)) {
            throw new EntityTemplateNotFoundException("identifier", identifier);
        }
        entityTemplateRepositoryPort.deleteByIdentifier(identifier);
    }

}
