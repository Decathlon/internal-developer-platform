package com.decathlon.idp_core.domain.service.entity_template;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.PropertyNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.UnsafeTypeConversionException;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;

/// Domain service orchestrating property definition validations and
/// constraints.
///
/// **Business purpose:** Enforces invariants for property definitions within
/// entity templates, including uniqueness constraints and type conversion safety
/// rules. Acts as the single source of truth for all property-level business
/// logic.
///
/// **Key responsibilities:**
/// - Validate property name uniqueness within an entity template
/// - Enforce type conversion constraints considering existing entity data
/// - Apply type conversion safety rules (safe vs. unsafe conversions)
@Service
public class PropertyDefinitionService {

    private final EntityRepositoryPort entityRepositoryPort;

    private static final Set<String> SAFE_CONVERSIONS = Set.of(
            "NUMBER_to_STRING",
            "BOOLEAN_to_STRING");

    public PropertyDefinitionService(EntityRepositoryPort entityRepositoryPort) {
        this.entityRepositoryPort = entityRepositoryPort;
    }

    /// Validates that all property names are unique within a template.
    ///
    /// **Contract:** Enforces the invariant that property names must be unique. Used
    /// during template creation and updates to prevent duplicate property
    /// definitions.
    ///
    /// @param properties the list of property definitions to validate
    /// @throws PropertyNameAlreadyExistsException if duplicate property names
    ///                                            are found
    public void validateUniquePropertyNames(List<PropertyDefinition> properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }

        Set<String> names = new HashSet<>();
        for (PropertyDefinition property : properties) {
            if (property.name() != null && !names.add(property.name())) {
                throw new PropertyNameAlreadyExistsException(property.name());
            }
        }
    }

    /// Validates property type changes against data existence and conversion
    /// safety.
    ///
    /// **Contract:** Enforces type change guardrails where:
    /// - Safe conversions are always allowed
    /// - Unsafe conversions are only allowed when no entities exist for the template
    ///
    /// **Safe conversions:** NUMBER→STRING, BOOLEAN→STRING **Unsafe conversions:**
    /// STRING→NUMBER, STRING→BOOLEAN, NUMBER→BOOLEAN, BOOLEAN→NUMBER
    ///
    /// @param existingProperties the existing property definitions
    /// @param updatedProperties  the new/updated property definitions
    /// @param templateIdentifier the template identifier to check for entity
    ///                           existence
    /// @throws UnsafeTypeConversionException if an unsafe conversion is attempted on
    ///                                       existing entities
    public void validateTypeChanges(
            List<PropertyDefinition> existingProperties,
            List<PropertyDefinition> updatedProperties,
            String templateIdentifier) {

        if (existingProperties == null || existingProperties.isEmpty() ||
                updatedProperties == null || updatedProperties.isEmpty()) {
            return;
        }

        boolean entitiesExist = entityRepositoryPort.existsByTemplateIdentifier(templateIdentifier);

        // Build map of updated properties by name
        Map<String, PropertyDefinition> updatedMap = updatedProperties.stream()
                .collect(Collectors.toMap(PropertyDefinition::name, p -> p));

        // Check each existing property for unsafe type changes
        for (PropertyDefinition existing : existingProperties) {
            PropertyDefinition updated = updatedMap.get(existing.name());
            boolean propertyTypeChanged = updated != null && !existing.type().equals(updated.type());

            if (entitiesExist && propertyTypeChanged && !isConversionSafe(existing.type(), updated.type())) {
                throw new UnsafeTypeConversionException(
                        existing.name(),
                        existing.type(),
                        updated.type());

            }
        }
    }

    /// Determines if a type conversion is safe (can be done even with existing
    /// entities).
    ///
    /// @param fromType the current property type
    /// @param toType   the new property type
    /// @return true if the conversion is safe, false otherwise
    private boolean isConversionSafe(PropertyType fromType, PropertyType toType) {
        String conversionKey = fromType + "_to_" + toType;
        return SAFE_CONVERSIONS.contains(conversionKey);
    }
}
