package com.decathlon.idp_core.infrastructure.api.mapper.entitytemplate;

import java.util.List;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.infrastructure.api.dto.in.EntityTemplateDtoIn;
import com.decathlon.idp_core.infrastructure.api.dto.in.PropertyDefinitionDtoIn;
import com.decathlon.idp_core.infrastructure.api.dto.in.PropertyRulesDtoIn;
import com.decathlon.idp_core.infrastructure.api.dto.in.RelationDefinitionDtoIn;
import com.decathlon.idp_core.infrastructure.api.dto.out.entitytemplate.EntityTemplateDtoOut;
import com.decathlon.idp_core.infrastructure.api.dto.out.entitytemplate.PropertyDefinitionDtoOut;
import com.decathlon.idp_core.infrastructure.api.dto.out.entitytemplate.PropertyRulesDtoOut;
import com.decathlon.idp_core.infrastructure.api.dto.out.entitytemplate.RelationDefinitionDtoOut;

/**
 * Mapper component for converting between Entity Template DTOs and domain
 * entities.
 * <p>
 * This mapper provides bidirectional conversion between:
 * <ul>
 * <li>Input DTOs (from REST requests) to domain entities</li>
 * <li>Domain entities to output DTOs (for REST responses)</li>
 * </ul>
 * </p>
 * <p>
 * The mapper follows Domain-Driven Design (DDD) principles by:
 * <ul>
 * <li>Maintaining clear boundaries between API and domain layers</li>
 * <li>Ensuring domain entities remain pure without API concerns</li>
 * <li>Providing null-safe conversions with appropriate defaults</li>
 * </ul>
 * </p>
 * <p>
 * All mapping methods handle null inputs gracefully, returning null or empty
 * collections
 * as appropriate. The mapper supports both individual entity mapping and bulk
 * list conversions.
 * </p>
 *
 * @author IDP Core Team
 * @since 1.0.0
 * @see EntityTemplate
 * @see EntityTemplateDtoIn
 * @see EntityTemplateDtoOut
 */
@Component
public class EntityTemplateMapper {

    /**
     * Converts an EntityTemplate input DTO to a domain entity.
     * <p>
     * This method maps all fields from the input DTO to create a new EntityTemplate
     * domain entity.
     * Nested collections (properties and relations) are recursively converted using
     * their
     * respective mapping methods.
     * </p>
     *
     * @param dto the input DTO to convert, may be null
     * @return the converted EntityTemplate domain entity, or null if input is null
     */
    public EntityTemplate fromDtoToEntityTemplate(EntityTemplateDtoIn dto) {
        if (dto == null) {
            return null;
        }

        return EntityTemplate.builder()
                .identifier(dto.getIdentifier())
                .description(dto.getDescription())
                .propertiesDefinitions(toPropertyDefinitionEntities(dto.getPropertiesDefinitions()))
                .relationsDefinitions(toRelationDefinitionEntities(dto.getRelationsDefinitions()))
                .build();
    }

    /**
     * Converts an EntityTemplate domain entity to an output DTO.
     * <p>
     * This method maps all fields from the domain entity to create a new output DTO
     * for API responses.
     * The conversion includes the entity's UUID ID and all nested collections are
     * recursively
     * converted to their respective DTO representations.
     * </p>
     *
     * @param entity the domain entity to convert, may be null
     * @return the converted EntityTemplateDtoOut, or null if input is null
     */
    public EntityTemplateDtoOut fromEntityTemplatetoDto(EntityTemplate entity) {
        if (entity == null) {
            return null;
        }

        return EntityTemplateDtoOut.builder()
                .identifier(entity.getIdentifier())
                .description(entity.getDescription())
                .propertiesDefinitions(toPropertyDefinitionDtos(entity.getPropertiesDefinitions()))
                .relationsDefinitions(toRelationDefinitionDtos(entity.getRelationsDefinitions()))
                .build();
    }

    /**
     * Converts a list of EntityTemplate domain entities to a list of output DTOs.
     * <p>
     * This is a convenience method for bulk conversion operations, particularly
     * useful
     * for paginated results and list endpoints.
     * </p>
     *
     * @param entities the list of domain entities to convert, may be null
     * @return a list of converted EntityTemplateDtoOut objects, empty list if input
     *         is null
     */
    public List<EntityTemplateDtoOut> fromEntityTemplatesToDtos(List<EntityTemplate> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::fromEntityTemplatetoDto)
                .toList();
    }

    /**
     * Converts a PropertyDefinition input DTO to a domain entity.
     *
     * @param dto the input DTO to convert, may be null
     * @return the converted PropertyDefinition domain entity, or null if input is
     *         null
     */
    public PropertyDefinition toToPropertyDefinition(PropertyDefinitionDtoIn dto) {
        if (dto == null) {
            return null;
        }

        return PropertyDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .required(dto.isRequired())
                .rules(toPropertyRules(dto.getRules()))
                .build();
    }

    /**
     * Converts a PropertyDefinition domain entity to an output DTO.
     *
     * @param entity the domain entity to convert, may be null
     * @return the converted PropertyDefinitionDtoOut, or null if input is null
     */
    public PropertyDefinitionDtoOut toDto(PropertyDefinition entity) {
        if (entity == null) {
            return null;
        }

        return PropertyDefinitionDtoOut.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .type(entity.getType())
                .required(entity.isRequired())
                .rules(toDto(entity.getRules()))
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

        return PropertyRules.builder()
                .format(dto.getFormat())
                .enumValues(dto.getEnumValues())
                .regex(dto.getRegex())
                .maxLength(dto.getMaxLength())
                .minLength(dto.getMinLength())
                .maxValue(dto.getMaxValue())
                .minValue(dto.getMinValue())
                .build();
    }

    public PropertyRulesDtoOut toDto(PropertyRules entity) {
        if (entity == null) {
            return null;
        }

        return PropertyRulesDtoOut.builder()
                .id(entity.getId())
                .format(entity.getFormat())
                .enumValues(entity.getEnumValues())
                .regex(entity.getRegex())
                .maxLength(entity.getMaxLength())
                .minLength(entity.getMinLength())
                .maxValue(entity.getMaxValue())
                .minValue(entity.getMinValue())
                .build();
    }

    public RelationDefinition toRelationDefinition(RelationDefinitionDtoIn dto) {
        if (dto == null) {
            return null;
        }

        return RelationDefinition.builder()
                .name(dto.getName())
                .targetEntityIdentifier(dto.getTargetEntityIdentifier())
                .required(dto.isRequired())
                .toMany(dto.isToMany())
                .build();
    }

    public RelationDefinitionDtoOut toDto(RelationDefinition entity) {
        if (entity == null) {
            return null;
        }

        return RelationDefinitionDtoOut.builder()
                .name(entity.getName())
                .targetEntityIdentifier(entity.getTargetEntityIdentifier())
                .required(entity.isRequired())
                .toMany(entity.isToMany())
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
