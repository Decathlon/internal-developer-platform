package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_DESCRIPTION_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_TYPE_MANDATORY;

import java.util.UUID;

import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Property Definition domain model representing a single property specification within an entity template.
 * <p>
 * A PropertyDefinition describes the characteristics, type, validation rules, and metadata
 * for a specific property that entities based on an EntityTemplate should have.
 * </p>
 * <p>
 * This class follows Domain-Driven Design (DDD) principles by:
 * <ul>
 *   <li>Encapsulating property specification logic within the domain</li>
 *   <li>Providing rich validation rules through Bean Validation annotations</li>
 *   <li>Maintaining consistency through type safety and constraints</li>
 *   <li>Supporting flexible property definitions through the PropertyRules association</li>
 * </ul>
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li><strong>Type Safety</strong>: Enforces property types through enums</li>
 *   <li><strong>Validation Rules</strong>: Supports complex validation through PropertyRules</li>
 *   <li><strong>Flexibility</strong>: Properties can be required or optional</li>
 *   <li><strong>Metadata</strong>: Rich descriptions for documentation and UI generation</li>
 * </ul>
 * </p>
 *
 * @author IDP Core Team
 * @since 1.0.0
 * @see EntityTemplate
 * @see PropertyType
 * @see PropertyRules
 */
@Entity
@Data
@Table
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class PropertyDefinition {

    /**
     * The unique identifier (UUID) for this property definition.
     * <p>
     * This is the primary key used for database operations and internal references.
     * It is automatically generated when the entity is persisted.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The name of the property.
     * <p>
     * This is the field name that will be used when creating entities based on this property definition.
     * It must be unique within the context of an EntityTemplate and cannot be null or blank.
     * </p>
     * <p>
     * Examples: "firstName", "email", "createdDate", "status"
     * </p>
     */
    @NotBlank(message = PROPERTY_NAME_MANDATORY)
    private String name;

    /**
     * A human-readable description of the property.
     * <p>
     * This provides context and documentation about the property's purpose, usage,
     * and any special considerations. It cannot be null or blank and is used for
     * API documentation and UI generation.
     * </p>
     */
    @NotBlank(message = PROPERTY_DESCRIPTION_MANDATORY)
    private String description;

    /**
     * The data type of this property.
     * <p>
     * Defines what kind of data this property can hold (string, number, boolean, etc.).
     * This is used for validation, serialization, and UI component generation.
     * The type cannot be null and must be one of the predefined {@link PropertyType} values.
     * </p>
     *
     * @see PropertyType
     */
    @Enumerated(EnumType.STRING)
    @NotNull(message = PROPERTY_TYPE_MANDATORY)
    private PropertyType type;

    /**
     * Indicates whether this property is required for entities based on the template.
     * <p>
     * When {@code true}, entities must provide a value for this property.
     * When {@code false}, the property is optional and can be null or omitted.
     * Defaults to {@code false} (optional) if not specified.
     * </p>
     */
    @Builder.Default
    private boolean required = false;

    /**
     * Additional validation rules and constraints for this property.
     * <p>
     * This optional association provides fine-grained validation rules such as:
     * <ul>
     *   <li>String length constraints (min/max length)</li>
     *   <li>Numeric range constraints (min/max values)</li>
     *   <li>Format validation (regex patterns)</li>
     *   <li>Enumeration of allowed values</li>
     * </ul>
     * </p>
     * <p>
     * The rules are managed with cascade operations, ensuring consistency
     * when the property definition is persisted, updated, or deleted.
     * </p>
     *
     * @see PropertyRules
     */
    @OneToOne(cascade = CascadeType.ALL)
    private PropertyRules rules;

}
