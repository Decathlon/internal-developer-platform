package com.decathlon.idp_core.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;

/**
 * Repository interface for managing {@link EntityTemplate} entities.
 * <p>
 * This repository provides CRUD operations and custom query methods for EntityTemplate entities.
 * It extends Spring Data JPA's {@link JpaRepository} to inherit standard database operations
 * and defines additional methods for business-specific queries.
 * </p>
 * <p>
 * The repository follows Domain-Driven Design (DDD) principles by:
 * <ul>
 *   <li>Operating on domain entities rather than DTOs</li>
 *   <li>Providing methods that reflect business use cases</li>
 *   <li>Encapsulating data access logic</li>
 * </ul>
 * </p>
 *
 * @author IDP Core Team
 * @since 1.0.0
 */
@Repository
public interface EntityTemplateRepository extends JpaRepository<EntityTemplate, UUID> {

    /**
     * Finds an EntityTemplate by its unique identifier.
     * <p>
     * This method provides a business-friendly way to retrieve templates using their
     * identifier string rather than the internal UUID primary key.
     * </p>
     *
     * @param templateIdentifier the unique identifier of the template to find, must not be null
     * @return an {@link Optional} containing the EntityTemplate if found, empty otherwise
     * @throws IllegalArgumentException if templateIdentifier is null
     */
    Optional<EntityTemplate> findByIdentifier(String templateIdentifier);

    /**
     * Finds an EntityTemplate by its UUID primary key.
     * <p>
     * This method overrides the default JpaRepository findById method to provide
     * explicit null safety annotations and documentation.
     * </p>
     *
     * @param id the UUID primary key of the template to find, must not be null
     * @return an {@link Optional} containing the EntityTemplate if found, empty otherwise
     * @throws IllegalArgumentException if id is null
     */
    @SuppressWarnings("null")
    Optional<EntityTemplate> findById(UUID id);

    /**
     * Retrieves all EntityTemplates with pagination support.
     * <p>
     * This method overrides the default JpaRepository findAll method to provide
     * explicit pagination support and null safety annotations.
     * </p>
     *
     * @param pageable the pagination information including page number, size, and sorting, must not be null
     * @return a {@link Page} containing the EntityTemplates for the requested page
     * @throws IllegalArgumentException if pageable is null
     */
    @SuppressWarnings("null")
    Page<EntityTemplate> findAll(Pageable pageable);

    /**
     * Checks if an EntityTemplate exists with the given identifier.
     * <p>
     * This method is useful for validation purposes, particularly when creating new templates
     * to ensure identifier uniqueness without loading the entire entity.
     * </p>
     *
     * @param identifier the unique identifier to check for existence, must not be null
     * @return {@code true} if an EntityTemplate with the given identifier exists, {@code false} otherwise
     * @throws IllegalArgumentException if identifier is null
     */
    boolean existsByIdentifier(String identifier);

    /**
     * Deletes an EntityTemplate by its unique identifier.
     * <p>
     * This method provides a transactional delete operation using the business identifier
     * rather than the internal UUID. The {@code @Transactional} annotation ensures that
     * the delete operation is executed within a transaction context.
     * </p>
     * <p>
     * <strong>Note:</strong> This method uses a custom query derived from the method name.
     * Spring Data JPA will generate the appropriate DELETE query at runtime.
     * </p>
     *
     * @param identifier the unique identifier of the template to delete, must not be null
     * @throws IllegalArgumentException if identifier is null
     * @throws DataAccessException      if the delete operation fails
     */
    @Transactional
    void deleteByIdentifier(String identifier);
}
