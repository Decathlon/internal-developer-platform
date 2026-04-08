package com.decathlon.idp_core.domain.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.repository.EntityRepository;
import com.decathlon.idp_core.domain.repository.EntityTemplateRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

/**
 * Service class for managing Entity domain logic.
 * <p>
 * Provides methods for retrieving, creating, and querying entities and their summaries.
 * Handles validation and existence checks for entity templates.
 * </p>
 */
@Service
@AllArgsConstructor
public class EntityService {
    private final EntityRepository entityRepository;
    private final EntityTemplateRepository entityTemplateRepository;

    /**
     * Retrieves a paginated list of entities by template identifier.
     *
     * @param pageable the pagination information
     * @param templateIdentifier the identifier of the entity template
     * @return a page of entities matching the template identifier
     * @throws EntityTemplateNotFoundException if the template does not exist
     */
    @Transactional
    public Page<Entity> getEntitiesByTemplateIdentifier(Pageable pageable, String templateIdentifier) {

        if (!entityTemplateRepository.existsByIdentifier(templateIdentifier)) {
            throw new EntityTemplateNotFoundException("identifier", templateIdentifier);
        }
        return entityRepository.findByTemplateIdentifier(templateIdentifier, pageable);
    }

     /**
     * Retrieves summaries for a list of entity identifiers.
     *
     * @param identifiers the list of entity identifiers
     * @return a list of entity summaries
     */
    public List<EntitySummary> getEntitiesSummariesByIndentifiers(List<String> identifiers) {
        return entityRepository.findByIdentifierIn(identifiers);
    }

    /**
     * Retrieves an entity by its unique identifier.
     *
     * @param entityIdentifier the identifier of the entity
     * @return the entity
     * @throws EntityNotFoundException if not found
     */
    @Transactional
    public Entity getEntityByTemplateIdentifierAnIdentifier(String templateIdentifier, String entityIdentifier) {
        if (!entityTemplateRepository.existsByIdentifier(templateIdentifier)) {
            throw new EntityTemplateNotFoundException("identifier", templateIdentifier);
        }
        return entityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
                .orElseThrow(() -> new EntityNotFoundException(templateIdentifier,
                        entityIdentifier));
    }

     /**
     * Creates and persists a new entity.
     *
     * @param entity the entity to create (validated)
     * @return the persisted entity
     */
     @SuppressWarnings("null")
    public Entity createEntity(@Valid Entity entity) {
        // Add validations
        return entityRepository.save(entity);
    }
}
