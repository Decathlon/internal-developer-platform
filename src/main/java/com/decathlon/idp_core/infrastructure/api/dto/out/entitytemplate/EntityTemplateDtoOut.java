package com.decathlon.idp_core.infrastructure.api.dto.out.entitytemplate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = "Output for entity template")
public class EntityTemplateDtoOut {

    @Schema(description = FIELD_TEMPLATE_IDENTIFIER, example = "service")
    private String identifier;

    @Schema(description = FIELD_TEMPLATE_DESCRIPTION, example = "A comprehensive service template")
    private String description;

    @Schema(description = FIELD_TEMPLATE_PROPERTIES)
    private List<PropertyDefinitionDtoOut> propertiesDefinitions;

    @Schema(description = FIELD_TEMPLATE_RELATIONS)
    private List<RelationDefinitionDtoOut> relationsDefinitions;
}
