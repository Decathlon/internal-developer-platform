package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_NAME_MANDATORY_SIMPLE;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE;

import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Relation Definition domain model representing a relationship specification between entity templates.
 * <p>
 * A RelationDefinition describes how entities based on one template can relate to entities
 * based on another template. It defines the cardinality, requirements, and semantic meaning
 * of the relationship between different entity types.
 * </p>
 * <p>
 * This class follows Domain-Driven Design (DDD) principles by:
 * <ul>
 *   <li>Modeling relationships as first-class domain concepts</li>
 *   <li>Encapsulating relationship rules and constraints</li>
 *   <li>Providing type-safe references through entity identifiers</li>
 *   <li>Supporting flexible relationship definitions with cardinality control</li>
 * </ul>
 * </p>
 * <p>
 * Key relationship features:
 * <ul>
 *   <li><strong>Semantic Naming</strong>: Human-readable relationship names</li>
 *   <li><strong>Type Safety</strong>: References target entities by their business identifiers</li>
 *   <li><strong>Cardinality Control</strong>: Support for one-to-one and one-to-many relationships</li>
 *   <li><strong>Requirement Levels</strong>: Required vs. optional relationships</li>
 * </ul>
 * </p>
 *
 * @author IDP Core Team
 * @since 1.0.0
 * @see EntityTemplate
 */
@Entity
@Data
@Table
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class RelationDefinition {

    /**
     * The unique identifier (UUID) for this relation definition.
     * <p>
     * This is the primary key used for database operations and internal references.
     * It is automatically generated when the entity is persisted.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The semantic name of this relationship.
     * <p>
     * This provides a human-readable name that describes the meaning of the relationship.
     * It should clearly indicate what the relationship represents in business terms.
     * The name cannot be null or blank.
     * </p>
     * <p>
     * Examples: "owns", "belongsTo", "manages", "partOf", "assignedTo"
     * </p>
     */
    @NotBlank(message = RELATION_NAME_MANDATORY_SIMPLE)
    private String name;

    /**
     * The business identifier of the target entity template for this relationship.
     * <p>
     * This references another EntityTemplate by its business identifier (not UUID).
     * The target template defines what kind of entities can be related through
     * this relationship. The identifier cannot be null or blank.
     * </p>
     * <p>
     * Examples: "user-profile", "department", "product-category"
     * </p>
     */
    @NotBlank(message = RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE)
    private String targetEntityIdentifier;

    /**
     * Indicates whether this relationship is required for entities based on the template.
     * <p>
     * When {@code true}, entities must have at least one relationship of this type.
     * When {@code false}, the relationship is optional and entities may or may not
     * have this relationship. Defaults to {@code false} (optional) if not specified.
     * </p>
     */
    @Builder.Default
    private boolean required = false;

    /**
     * Indicates the cardinality of this relationship.
     * <p>
     * When {@code true}, entities can have multiple relationships of this type (one-to-many).
     * When {@code false}, entities can have at most one relationship of this type (one-to-one).
     * Defaults to {@code false} (one-to-one) if not specified.
     * </p>
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code toMany = false}: A user has one profile</li>
     *   <li>{@code toMany = true}: A user can have multiple roles</li>
     * </ul>
     * </p>
     */
    @Builder.Default
    private boolean toMany = false;

}
