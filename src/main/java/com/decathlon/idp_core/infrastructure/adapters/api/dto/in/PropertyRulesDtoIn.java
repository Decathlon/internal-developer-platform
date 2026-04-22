package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_PROPERTY_RULES_IN;

import com.decathlon.idp_core.domain.model.enums.PropertyFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = SCHEMA_PROPERTY_RULES_IN)
public class PropertyRulesDtoIn {

    @Schema(description = "Property format validation", example = "EMAIL")
    private PropertyFormat format;

    @Schema(description = "Enumeration values for enum properties", example = "[\"ACTIVE\", \"INACTIVE\"]")
    private String[] enumValues;

    @Schema(description = "Regular expression pattern for validation", example = "^[a-zA-Z0-9]+$")
    private String regex;

    @Schema(description = "Maximum length for string properties", example = "255")
    private Integer maxLength;

    @Schema(description = "Minimum length for string properties", example = "1")
    private Integer minLength;

    @Schema(description = "Maximum value for numeric properties", example = "100")
    private Integer maxValue;

    @Schema(description = "Minimum value for numeric properties", example = "0")
    private Integer minValue;
}
