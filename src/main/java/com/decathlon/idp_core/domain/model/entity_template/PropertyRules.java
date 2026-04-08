package com.decathlon.idp_core.domain.model.entity_template;

import java.util.UUID;

import com.decathlon.idp_core.domain.model.enums.PropertyFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Property Rules domain model representing validation constraints and formatting rules for properties.
 * <p>
 * PropertyRules defines fine-grained validation and formatting constraints that can be applied
 * to properties within an EntityTemplate. These rules provide flexible validation beyond
 * basic type checking, allowing for business-specific constraints and data quality enforcement.
 * </p>
 * <p>
 * This class follows Domain-Driven Design (DDD) principles by:
 * <ul>
 *   <li>Encapsulating validation logic within the domain model</li>
 *   <li>Providing flexible constraint definitions for different property types</li>
 *   <li>Supporting both format-based and value-based validation</li>
 *   <li>Enabling business rule enforcement at the domain level</li>
 * </ul>
 * </p>
 * <p>
 * Supported validation types:
 * <ul>
 *   <li><strong>Format Validation</strong>: Predefined formats like email, URL, phone number</li>
 *   <li><strong>Length Constraints</strong>: Minimum and maximum string length validation</li>
 *   <li><strong>Value Constraints</strong>: Minimum and maximum numeric value validation</li>
 *   <li><strong>Enumeration</strong>: Restricted set of allowed values</li>
 *   <li><strong>Pattern Matching</strong>: Regular expression-based validation</li>
 * </ul>
 * </p>
 *
 * @author IDP Core Team
 * @since 1.0.0
 * @see PropertyDefinition
 * @see PropertyFormat
 */
@Entity
@Data
@Table
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class PropertyRules {

    /**
     * The unique identifier (UUID) for this property rules definition.
     * <p>
     * This is the primary key used for database operations and internal references.
     * It is automatically generated when the entity is persisted.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The predefined format type for this property.
     * <p>
     * Specifies a standard format that the property value must conform to.
     * This is useful for common validation patterns like email addresses,
     * URLs, phone numbers, dates, etc.
     * </p>
     * <p>
     * This field is optional and can be null if no specific format is required.
     * When specified, it provides built-in validation logic for the property.
     * </p>
     *
     * @see PropertyFormat
     */
    @Enumerated(EnumType.STRING)
    PropertyFormat format;

    /**
     * An array of allowed values for enumeration-based properties.
     * <p>
     * When specified, the property value must be one of the values in this array.
     * This is useful for creating dropdown lists, status fields, or any property
     * with a restricted set of valid values.
     * </p>
     * <p>
     * Examples: ["active", "inactive", "pending"], ["small", "medium", "large"]
     * </p>
     */
    String[] enumValues;

    /**
     * A regular expression pattern for custom validation.
     * <p>
     * Provides flexible validation through custom regular expressions.
     * The property value must match this pattern to be considered valid.
     * This allows for business-specific validation rules that go beyond
     * standard format validation.
     * </p>
     * <p>
     * Example: "^[A-Z]{2}[0-9]{4}$" for a custom code format
     * </p>
     */
    String regex;

    /**
     * The maximum allowed length for string properties.
     * <p>
     * When specified, string values cannot exceed this length.
     * This field is optional and only applies to string-type properties.
     * </p>
     */
    Integer maxLength;

    /**
     * The minimum required length for string properties.
     * <p>
     * When specified, string values must be at least this length.
     * This field is optional and only applies to string-type properties.
     * </p>
     */
    Integer minLength;

    /**
     * The maximum allowed value for numeric properties.
     * <p>
     * When specified, numeric values cannot exceed this limit.
     * This field is optional and only applies to numeric-type properties.
     * </p>
     */
    Integer maxValue;

    /**
     * The minimum allowed value for numeric properties.
     * <p>
     * When specified, numeric values must be at least this value.
     * This field is optional and only applies to numeric-type properties.
     * </p>
     */
    Integer minValue;
}
