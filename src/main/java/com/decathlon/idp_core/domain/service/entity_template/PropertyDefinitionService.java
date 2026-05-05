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

    /// Validates that property types are not changed on existing properties.
    ///
    /// **Contract:** Enforces the invariant that property types cannot be modified
    /// after initial creation. Any attempt to change a property type is forbidden.
    /// Users must delete and recreate the property if they need to change its type.
    ///
    /// @param existingProperties the existing property definitions
    /// @param updatedProperties  the new/updated property definitions
    /// @param templateIdentifier the template identifier (used for context only)
    /// @throws UnsafeTypeConversionException if any property type change is attempted
    public void validateTypeChanges(
            List<PropertyDefinition> existingProperties,
            List<PropertyDefinition> updatedProperties,
            String templateIdentifier) {

        if (existingProperties == null || existingProperties.isEmpty() ||
                updatedProperties == null || updatedProperties.isEmpty()) {
            return;
        }

        // Build map of updated properties by name
        Map<String, PropertyDefinition> updatedMap = updatedProperties.stream()
                .collect(Collectors.toMap(PropertyDefinition::name, p -> p));

        // Check each existing property for any type changes
        for (PropertyDefinition existing : existingProperties) {
            PropertyDefinition updated = updatedMap.get(existing.name());
            boolean propertyTypeChanged = updated != null && !existing.type().equals(updated.type());

            if (propertyTypeChanged) {
                throw new UnsafeTypeConversionException(
                        existing.name(),
                        existing.type(),
                        updated.type());
            }
        }
    }
}
