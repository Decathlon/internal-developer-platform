package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate;

import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = SCHEMA_PROPERTY_DEFINITION_OUT)
public class PropertyDefinitionDtoOut {

    @Schema(description = FIELD_PROPERTY_NAME, example = "applicationName")
    private String name;

    @Schema(description = FIELD_PROPERTY_DESCRIPTION, example = "Name of the application")
    private String description;

    @Schema(description = FIELD_PROPERTY_TYPE, example = "STRING")
    private PropertyType type;

    @Schema(description = FIELD_PROPERTY_REQUIRED, example = "true")
    private boolean required;

    @Schema(description = FIELD_PROPERTY_RULES, example = "Property validation rules")
    private PropertyRulesDtoOut rules;
}
