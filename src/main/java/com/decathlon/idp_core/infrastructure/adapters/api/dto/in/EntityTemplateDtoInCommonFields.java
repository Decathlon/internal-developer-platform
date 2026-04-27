package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_FORMAT;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_MAX_SIZE;
import static com.decathlon.idp_core.domain.constant.ValidationRegex.ENTITY_TEMPLATE_NAME_REGEX;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_TEMPLATE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_TEMPLATE_NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_TEMPLATE_PROPERTIES;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_TEMPLATE_RELATIONS;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common fields shared between EntityTemplateCreateDtoIn (POST) and EntityTemplateUpdateDtoIn (PUT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class EntityTemplateDtoInCommonFields {

    @Size(max = 255, message = TEMPLATE_NAME_MAX_SIZE)
    @Schema(description = FIELD_TEMPLATE_NAME, example = "Service")
    @NotBlank(message = TEMPLATE_NAME_MANDATORY)
    @Pattern(regexp = ENTITY_TEMPLATE_NAME_REGEX, message = TEMPLATE_NAME_FORMAT)
    private String name;

    @Schema(description = FIELD_TEMPLATE_DESCRIPTION, example = "A comprehensive service template")
    private String description;

    @Valid
    @Schema(description = FIELD_TEMPLATE_PROPERTIES)
    private List<PropertyDefinitionDtoIn> propertiesDefinitions;

    @Valid
    @Schema(description = FIELD_TEMPLATE_RELATIONS)
    private List<RelationDefinitionDtoIn> relationsDefinitions;
}
