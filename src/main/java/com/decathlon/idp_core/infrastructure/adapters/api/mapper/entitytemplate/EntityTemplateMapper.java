package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entitytemplate;

import java.util.List;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityTemplateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.PropertyDefinitionDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.PropertyRulesDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.RelationDefinitionDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate.EntityTemplateDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate.PropertyDefinitionDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate.PropertyRulesDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate.RelationDefinitionDtoOut;

/// Mapper component for converting between [EntityTemplate] DTOs and domain entities.
///
/// **Mapping capabilities:**
/// - Input DTOs (from REST requests) to domain entities
/// - Domain entities to output DTOs (for REST responses)
///
/// **Hexagonal architecture alignment:**
/// - Maintains clear boundaries between API and domain layers
/// - Ensures domain entities remain pure without API concerns
/// - Provides null-safe conversions with appropriate defaults
/// - Supports both individual entity mapping and bulk list conversions
///
/// **Why this mapper exists:**
/// All mapping methods handle null inputs gracefully, returning null or empty collections
/// as appropriate. This follows DDD principles by keeping mapping concerns separate
/// from business logic and ensuring clean API contract implementation.
@Component
public class EntityTemplateMapper {

    ///
    /// Converts an EntityTemplate input DTO to a domain entity.
    /// This method maps all fields from the input DTO to create a new EntityTemplate
    /// domain entity.
    /// Nested collections (properties and relations) are recursively converted using
    /// their
    /// respective mapping methods.
    ///
    /// @param dto the input DTO to convert, may be null
    /// @return the converted EntityTemplate domain entity, or null if input is null
    public EntityTemplate fromDtoToEntityTemplate(EntityTemplateDtoIn dto) {
        if (dto == null) {
            return null;
        }

        return new EntityTemplate(
                null,
                dto.getIdentifier(),
                dto.getDescription(),
                toPropertyDefinitionEntities(dto.getPropertiesDefinitions()),
                toRelationDefinitionEntities(dto.getRelationsDefinitions())
        );
    }

    ///
    /// Converts an EntityTemplate domain entity to an output DTO.
    /// <p>
    /// This method maps all fields from the domain entity to create a new output DTO
    /// for API responses.
    /// The conversion includes the entity's UUID ID and all nested collections are
    /// recursively
    /// converted to their respective DTO representations.
    /// </p>
    ///
    /// @param entity the domain entity to convert, may be null
    /// @return the converted EntityTemplateDtoOut, or null if input is null
    ////
    public EntityTemplateDtoOut fromEntityTemplatetoDto(EntityTemplate entity) {
        if (entity == null) {
            return null;
        }

        return EntityTemplateDtoOut.builder()
                .identifier(entity.identifier())
                .description(entity.description())
                .propertiesDefinitions(toPropertyDefinitionDtos(entity.propertiesDefinitions()))
                .relationsDefinitions(toRelationDefinitionDtos(entity.relationsDefinitions()))
                .build();
    }

    ///
    /// Converts a list of EntityTemplate domain entities to a list of output DTOs.
    /// <p>
    /// This is a convenience method for bulk conversion operations, particularly
    /// useful
    /// for paginated results and list endpoints.
    /// </p>
    ///
    /// @param entities the list of domain entities to convert, may be null
    /// @return a list of converted EntityTemplateDtoOut objects, empty list if input
    ///         is null
    ////
    public List<EntityTemplateDtoOut> fromEntityTemplatesToDtos(List<EntityTemplate> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::fromEntityTemplatetoDto)
                .toList();
    }

    ///
    /// Converts a PropertyDefinition input DTO to a domain entity.
    ///
    /// @param dto the input DTO to convert, may be null
    /// @return the converted PropertyDefinition domain entity, or null if input is
    ///         null
    ////
    public PropertyDefinition toToPropertyDefinition(PropertyDefinitionDtoIn dto) {
        if (dto == null) {
            return null;
        }

        return new PropertyDefinition(
                null,
                dto.getName(),
                dto.getDescription(),
                dto.getType(),
                dto.isRequired(),
                toPropertyRules(dto.getRules())
        );
    }

    ///
    /// Converts a PropertyDefinition domain entity to an output DTO.
    ///
    /// @param entity the domain entity to convert, may be null
    /// @return the converted PropertyDefinitionDtoOut, or null if input is null
    ////
    public PropertyDefinitionDtoOut toDto(PropertyDefinition entity) {
        if (entity == null) {
            return null;
        }

        return PropertyDefinitionDtoOut.builder()
                .name(entity.name())
                .description(entity.description())
                .type(entity.type())
                .required(entity.required())
                .rules(toDto(entity.rules()))
                .build();
    }

    public List<PropertyDefinition> toPropertyDefinitionEntities(List<PropertyDefinitionDtoIn> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(this::toToPropertyDefinition)
                .toList();
    }

    public List<PropertyDefinitionDtoOut> toPropertyDefinitionDtos(List<PropertyDefinition> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDto)
                .toList();
    }

    public PropertyRules toPropertyRules(PropertyRulesDtoIn dto) {
        if (dto == null) {
            return null;
        }

        return new PropertyRules(
                null,
                dto.getFormat(),
                dto.getEnumValues() != null ? List.of(dto.getEnumValues()) : null,
                dto.getRegex(),
                dto.getMaxLength(),
                dto.getMinLength(),
                dto.getMaxValue(),
                dto.getMinValue()
        );
    }

    public PropertyRulesDtoOut toDto(PropertyRules entity) {
        if (entity == null) {
            return null;
        }

        return PropertyRulesDtoOut.builder()
                .id(entity.id())
                .format(entity.format())
                .enumValues(entity.enumValues() != null ? entity.enumValues().toArray(new String[0]) : null)
                .regex(entity.regex())
                .maxLength(entity.maxLength())
                .minLength(entity.minLength())
                .maxValue(entity.maxValue())
                .minValue(entity.minValue())
                .build();
    }

    public RelationDefinition toRelationDefinition(RelationDefinitionDtoIn dto) {
        if (dto == null) {
            return null;
        }

        return new RelationDefinition(
                null,
                dto.getName(),
                dto.getTargetEntityIdentifier(),
                dto.isRequired(),
                dto.isToMany()
        );
    }

    public RelationDefinitionDtoOut toDto(RelationDefinition entity) {
        if (entity == null) {
            return null;
        }

        return RelationDefinitionDtoOut.builder()
                .name(entity.name())
                .targetEntityIdentifier(entity.targetEntityIdentifier())
                .required(entity.required())
                .toMany(entity.toMany())
                .build();
    }

    public List<RelationDefinition> toRelationDefinitionEntities(List<RelationDefinitionDtoIn> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(this::toRelationDefinition)
                .toList();
    }

    public List<RelationDefinitionDtoOut> toRelationDefinitionDtos(List<RelationDefinition> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDto)
                .toList();
    }
}
