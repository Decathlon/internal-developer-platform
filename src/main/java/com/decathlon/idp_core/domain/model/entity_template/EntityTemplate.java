package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_DEFINITIONS_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.TEMPLATE_IDENTIFIER_MANDATORY;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pure domain model representing an EntityTemplate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityTemplate {

    private UUID id;

    @NotBlank(message = TEMPLATE_IDENTIFIER_MANDATORY)
    private String identifier;

    private String description;

    @NotEmpty(message = PROPERTY_DEFINITIONS_MANDATORY)
    private List<PropertyDefinition> propertiesDefinitions;

    private List<RelationDefinition> relationsDefinitions;
}
