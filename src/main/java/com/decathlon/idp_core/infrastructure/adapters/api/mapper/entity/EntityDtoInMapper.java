package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDtoInCommonFields;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityUpdateDtoIn;

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
public class EntityDtoInMapper {

    /// Converts an entity creation request DTO to a domain entity.
    ///
    /// @param entityCreateDtoIn        the entity creation request payload
    /// @param entityTemplateIdentifier the target template identifier
    /// @return the mapped domain entity with audit fields populated
    public Entity fromPostEntityDtoInToEntity(EntityCreateDtoIn entityCreateDtoIn, String entityTemplateIdentifier) {
        return buildEntity(
                entityCreateDtoIn.getEntityDtoInCommonFields(),
                entityTemplateIdentifier,
                entityCreateDtoIn.getIdentifier()
        );
    }

    /// Converts an entity update request DTO to a domain entity.
    ///
    /// @param entityUpdateDtoIn        the entity update request payload
    /// @param entityTemplateIdentifier the target template identifier
    /// @param entityIdentifier         the target entity identifier from request path
    /// @return the mapped domain entity with audit fields populated
    public Entity fromPutEntityDtoInToEntity(EntityUpdateDtoIn entityUpdateDtoIn,
            String entityTemplateIdentifier,
            String entityIdentifier) {
        return buildEntity(
                entityUpdateDtoIn.getEntityDtoInCommonFields(),
                entityTemplateIdentifier,
                entityIdentifier
        );
    }

    /// Shared helper method to build the domain entity from common fields.
    private Entity buildEntity(EntityDtoInCommonFields commonFields, String entityTemplateIdentifier, String identifier) {
        List<Property> properties = commonFields.getProperties() == null
                ? Collections.emptyList()
                : commonFields.getProperties().entrySet().stream()
                .map(entry -> new Property(null, entry.getKey(), entry.getValue()))
                .toList();

        List<Relation> relations = commonFields.getRelations() == null
                ? Collections.emptyList()
                : commonFields.getRelations().stream()
                .map(relDto -> new Relation(null, relDto.getName(), null, relDto.getTargetEntityIdentifiers()))
                .toList();

        return new Entity(
                null,
                entityTemplateIdentifier,
                commonFields.getName(),
                identifier,
                properties,
                relations
        );
    }
}
