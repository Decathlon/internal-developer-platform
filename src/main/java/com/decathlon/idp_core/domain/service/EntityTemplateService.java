package com.decathlon.idp_core.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.ports.EntityTemplateRepositoryPort;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Service class for managing Entity Templates in the IDP Core application.
 *
 * <p>This service provides business logic operations for Entity Template management,
 * including CRUD operations, validation, and business rule enforcement. It acts as
 * an intermediary between the controller layer and the repository layer, ensuring
 * proper domain logic is applied.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Template retrieval with pagination support</li>
 *   <li>Template creation with duplicate prevention</li>
 *   <li>Template deletion with existence validation</li>
 *   <li>Business rule enforcement and validation</li>
 *   <li>Exception handling for domain-specific errors</li>
 * </ul>
 *
 * <p>This service follows Domain-Driven Design (DDD) principles by working exclusively
 * with domain entities ({@link EntityTemplate}) rather than DTOs. The mapping between
 * DTOs and domain entities is handled at the controller layer.</p>
 *
 * @author GitHub Copilot
 * @version 1.0
 * @see EntityTemplate
 * @see EntityTemplateRepository
 * @see EntityTemplateNotFoundException
 * @see EntityTemplateAlreadyExistsException
 * @since 1.0
 */
@Service
@Validated
@RequiredArgsConstructor
public class EntityTemplateService {

    private final EntityTemplateRepositoryPort entityTemplateRepository;

    /**
     * Retrieves a paginated list of all entity templates.
     *
     * <p>This method supports pagination and sorting through the {@link Pageable} parameter.
     * The returned {@link Page} contains both the template data and pagination metadata
     * such as total elements, total pages, and current page information.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * Pageable pageable = PageRequest.of(0, 10, Sort.by("identifier"));
     * Page<EntityTemplate> templates = service.getEntityTemplates(pageable);
     * }</pre>
     *
     * @param pageable the pagination information including page number, size, and sorting criteria
     * @return a {@link Page} containing entity templates and pagination metadata
     * @throws IllegalArgumentException if pageable is null
     */
    public Page<EntityTemplate> getEntityTemplates(Pageable pageable) {
        return entityTemplateRepository.findAll(pageable);
    }

    /**
     * Retrieves a specific entity template by its unique identifier.
     *
     * <p>This method performs a case-sensitive lookup for the template with the
     * specified identifier. The identifier must be an exact match.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * EntityTemplate template = service.getEntityTemplateByIdentifier("web-service");
     * }</pre>
     *
     * @param identifier the unique identifier of the template to retrieve
     * @return the {@link EntityTemplate} with the specified identifier
     * @throws EntityTemplateNotFoundException if no template with the given identifier exists
     * @throws IllegalArgumentException        if identifier is null or empty
     */
    public EntityTemplate getEntityTemplateByIdentifier(String identifier) {
        return entityTemplateRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new EntityTemplateNotFoundException("identifier", identifier));
    }

    @Transactional
    public EntityTemplate putEntityTemplate(String identifier, @Valid EntityTemplate updatedTemplate) {
        EntityTemplate existingTemplate = getEntityTemplateByIdentifier(identifier);

        if (!identifier.equals(updatedTemplate.getIdentifier()) &&
                entityTemplateRepository.findByIdentifier(updatedTemplate.getIdentifier()).isPresent()) {
            throw new EntityTemplateAlreadyExistsException(updatedTemplate.getIdentifier());
        }

        existingTemplate.setIdentifier(updatedTemplate.getIdentifier());
        existingTemplate.setDescription(updatedTemplate.getDescription());

        existingTemplate.setPropertiesDefinitions(
                mergePropertyDefinitions(existingTemplate.getPropertiesDefinitions(),
                        updatedTemplate.getPropertiesDefinitions())
        );

        existingTemplate.setRelationsDefinitions(
                mergeRelationDefinitions(existingTemplate.getRelationsDefinitions(),
                        updatedTemplate.getRelationsDefinitions())
        );

        return entityTemplateRepository.save(existingTemplate);
    }

    private List<PropertyDefinition> mergePropertyDefinitions(
            List<PropertyDefinition> existing,
            List<PropertyDefinition> updated) {

        if (existing == null) existing = new ArrayList<>();
        if (updated == null) return existing;

        Map<String, PropertyDefinition> existingMap = existing.stream()
                .collect(Collectors.toMap(PropertyDefinition::getName, Function.identity()));

        List<PropertyDefinition> result = new ArrayList<>();

        for (PropertyDefinition prop : updated) {
            PropertyDefinition existingProp = existingMap.get(prop.getName());
            if (existingProp != null) {
                existingProp.setDescription(prop.getDescription());
                existingProp.setType(prop.getType());
                existingProp.setRequired(prop.isRequired());

                existingProp.setRules(mergePropertyRules(existingProp.getRules(), prop.getRules()));

                result.add(existingProp);
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

        existingRules.setFormat(newRules.getFormat());
        existingRules.setEnumValues(newRules.getEnumValues());
        existingRules.setRegex(newRules.getRegex());
        existingRules.setMinLength(newRules.getMinLength());
        existingRules.setMaxLength(newRules.getMaxLength());
        existingRules.setMinValue(newRules.getMinValue());
        existingRules.setMaxValue(newRules.getMaxValue());

        return existingRules;
    }

    private List<RelationDefinition> mergeRelationDefinitions(
            List<RelationDefinition> existing,
            List<RelationDefinition> updated) {

        if (existing == null) existing = new ArrayList<>();
        if (updated == null) return existing;

        Map<String, RelationDefinition> existingMap = existing.stream()
                .collect(Collectors.toMap(RelationDefinition::getName, Function.identity()));

        List<RelationDefinition> result = new ArrayList<>();

        for (RelationDefinition rel : updated) {
            RelationDefinition existingRel = existingMap.get(rel.getName());
            if (existingRel != null) {
                existingRel.setTargetEntityIdentifier(rel.getTargetEntityIdentifier());
                existingRel.setRequired(rel.isRequired());
                existingRel.setToMany(rel.isToMany());
                result.add(existingRel);
            } else {
                result.add(rel);
            }
        }

        return result;
    }

    /**
     * Creates and saves a new entity template to the repository.
     *
     * <p>This method performs the following validations before saving:</p>
     * <ul>
     *   <li>Validates the entity template using Bean Validation annotations</li>
     *   <li>Checks for duplicate identifiers to prevent conflicts</li>
     *   <li>Ensures all required fields are properly populated</li>
     * </ul>
     *
     * <p>If a template with the same identifier already exists, an
     * {@link EntityTemplateAlreadyExistsException} will be thrown to maintain
     * data integrity and prevent duplicate entries.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * EntityTemplate newTemplate = new EntityTemplate();
     * newTemplate.setIdentifier("my-service");
     * // ... set other properties
     * EntityTemplate savedTemplate = service.saveEntityTemplate(newTemplate);
     * }</pre>
     *
     * @param entityTemplate the valid {@link EntityTemplate} entity to be saved
     * @return the saved {@link EntityTemplate} with generated fields populated
     * @throws EntityTemplateAlreadyExistsException            if a template with the same identifier already exists
     * @throws jakarta.validation.ConstraintViolationException if the entity fails validation
     * @throws IllegalArgumentException                        if entityTemplate is null
     */
    @Transactional
    public EntityTemplate saveEntityTemplate(@Valid EntityTemplate entityTemplate) {
        if (entityTemplate.getIdentifier() != null &&
                entityTemplateRepository.findByIdentifier(entityTemplate.getIdentifier()).isPresent()) {
            throw new EntityTemplateAlreadyExistsException(entityTemplate.getIdentifier());
        }
        return entityTemplateRepository.save(entityTemplate);
    }

    /**
     * Deletes an entity template by its unique identifier.
     *
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Validates that the identifier is not null</li>
     *   <li>Checks if a template with the given identifier exists</li>
     *   <li>Deletes the template if found</li>
     * </ol>
     *
     * <p>The deletion is performed using the identifier rather than the internal ID
     * to maintain consistency with the public API interface. This method ensures
     * data integrity by validating existence before attempting deletion.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * service.deleteEntityTemplate("web-service");
     * }</pre>
     *
     * <p><strong>Note:</strong> This operation is irreversible. Once a template
     * is deleted, it cannot be recovered through this service.</p>
     *
     * @param identifier the unique identifier of the template to delete
     * @throws EntityTemplateNotFoundException if no template with the given identifier exists
     * @throws IllegalArgumentException        if identifier is null
     */
    @Transactional
    public void deleteEntityTemplate(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Template identifier must not be null");
        }
        if (!entityTemplateRepository.existsByIdentifier(identifier)) {
            throw new EntityTemplateNotFoundException("identifier", identifier);
        }
        entityTemplateRepository.deleteByIdentifier(identifier);
    }

}
