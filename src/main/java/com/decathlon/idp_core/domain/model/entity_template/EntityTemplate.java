package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_DEFINITIONS_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.TEMPLATE_IDENTIFIER_MANDATORY;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity Template domain model representing a template for defining entity structures.
 * <p>
 * An EntityTemplate serves as a blueprint for creating entities with predefined properties
 * and relationships. It defines the structure, validation rules, and metadata that entities
 * of this template type should follow.
 * </p>
 * <p>
 * This class follows Domain-Driven Design (DDD) principles by:
 * <ul>
 *   <li>Representing a core domain concept in the IDP system</li>
 *   <li>Encapsulating business rules through validation annotations</li>
 *   <li>Maintaining referential integrity through cascade operations</li>
 *   <li>Providing a rich domain model with meaningful relationships</li>
 * </ul>
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li><strong>Unique Identification</strong>: Each template has a unique business identifier</li>
 *   <li><strong>Property Definitions</strong>: Defines the structure of entity properties</li>
 *   <li><strong>Relation Definitions</strong>: Specifies relationships to other entities</li>
 *   <li><strong>Validation</strong>: Built-in validation for required fields and constraints</li>
 * </ul>
 * </p>
 *
 * @author IDP Core Team
 * @since 1.0.0
 * @see PropertyDefinition
 * @see RelationDefinition
 */
@Entity
@Data
@Table
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class EntityTemplate {

    /**
     * The unique identifier (UUID) for this entity template.
     * <p>
     * This is the primary key used for database operations and internal references.
     * It is automatically generated when the entity is persisted.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The business identifier for this entity template.
     * <p>
     * This is a human-readable, unique identifier used for business operations
     * and external API interactions. It must be unique across all entity templates
     * and cannot be null or blank.
     * </p>
     * <p>
     * Examples: "user-profile", "product-catalog", "order-management"
     * </p>
     */
    @Column(nullable = false, unique = true)
    @NotBlank(message = TEMPLATE_IDENTIFIER_MANDATORY)
    private String identifier;

    /**
     * Optional description providing additional context about this entity template.
     * <p>
     * This field can contain detailed information about the template's purpose,
     * usage guidelines, or any other relevant documentation.
     * </p>
     */
    private String description;

    /**
     * The list of property definitions that define the structure of entities based on this template.
     * <p>
     * Each property definition specifies:
     * <ul>
     *   <li>Property name and description</li>
     *   <li>Data type and format requirements</li>
     *   <li>Validation rules and constraints</li>
     *   <li>Whether the property is required or optional</li>
     * </ul>
     * </p>
     * <p>
     * This collection cannot be empty as every entity template must define at least
     * one property. All property definitions are managed with cascade operations,
     * meaning they are automatically persisted, updated, and deleted with the template.
     * </p>
     */
    @OneToMany(cascade = CascadeType.ALL)
    @NotEmpty(message = PROPERTY_DEFINITIONS_MANDATORY)
    private List<PropertyDefinition> propertiesDefinitions;

    /**
     * The list of relation definitions that specify how entities based on this template
     * can relate to other entities.
     * <p>
     * Each relation definition specifies:
     * <ul>
     *   <li>The target entity template for the relationship</li>
     *   <li>The cardinality of the relationship (one-to-one, one-to-many)</li>
     *   <li>Whether the relationship is required or optional</li>
     *   <li>The semantic meaning of the relationship</li>
     * </ul>
     * </p>
     * <p>
     * This collection is optional and can be empty for templates that don't define
     * relationships to other entities. All relation definitions are managed with
     * cascade operations for consistency.
     * </p>
     */
    @OneToMany(cascade = CascadeType.ALL)
    private List<RelationDefinition> relationsDefinitions;

}
