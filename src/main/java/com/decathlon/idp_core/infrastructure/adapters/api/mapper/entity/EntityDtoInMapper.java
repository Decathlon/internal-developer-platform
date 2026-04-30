package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDtoIn;

import lombok.AllArgsConstructor;

/// Adapter mapper for converting API request DTOs to domain [Entity] objects.
///
/// **Infrastructure mapping responsibilities:**
/// - Request DTO-to-domain conversion for entity creation operations
/// - Type-safe property mapping with string value normalization
/// - Relationship mapping with template identifier injection
/// - Defensive handling of null collections and properties
///
/// **Mapping considerations:**
/// - All property values normalized to strings for consistent domain handling
/// - Target template identifiers resolved from API context rather than request body
/// - Empty collections preferred over null to prevent downstream null checks
///
/// **API contract support:** Enables clean separation between API request format
/// and internal domain model structure for maintainable API evolution.
@Component
@AllArgsConstructor
public class EntityDtoInMapper {

    public Entity fromEntityDtoInToEntity(EntityDtoIn entityDtoIn, String entityTemplateIdentifier) {

        List<Property> properties = entityDtoIn.getProperties() == null ? Collections.emptyList()
                : entityDtoIn.getProperties().entrySet().stream()
                .map((Map.Entry<String, Object> entry) -> {
                    String value;
                    if (entry.getValue() != null) {
                        value = String.valueOf(entry.getValue());
                    } else {
                        value = null;
                    }
                    return new Property(
                            null,
                            entry.getKey(),
                            value
                    );
                })
                .toList();

        List<Relation> relations = entityDtoIn.getRelations() == null ? Collections.emptyList()
                : entityDtoIn.getRelations().stream()
                .map(relDto -> new Relation(
                        null,
                        relDto.getName(),
                        null,
                        relDto.getTargetEntityIdentifiers()
                ))
                .toList();

        return new Entity(
                null,
                entityTemplateIdentifier,
                entityDtoIn.getName(),
                entityDtoIn.getIdentifier(),
                properties,
                relations
        );
    }
}
