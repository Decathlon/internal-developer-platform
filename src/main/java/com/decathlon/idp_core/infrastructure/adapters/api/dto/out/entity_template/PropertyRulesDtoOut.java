package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_template;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_RULES_ENUM_VALUES;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_RULES_FORMAT;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_RULES_MAX_LENGTH;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_RULES_MAX_VALUE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_RULES_MIN_LENGTH;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_RULES_MIN_VALUE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_RULES_REGEX;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_PROPERTY_RULES_OUT;

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
@Schema(description = SCHEMA_PROPERTY_RULES_OUT)
public class PropertyRulesDtoOut {

    @Schema(description = FIELD_PROPERTY_RULES_FORMAT, example = "STRING")
    private PropertyFormat format;

    @Schema(description = FIELD_PROPERTY_RULES_ENUM_VALUES, example = "[\"VALUE1\", \"VALUE2\"]")
    private String[] enumValues;

    @Schema(description = FIELD_PROPERTY_RULES_REGEX, example = "^[A-Za-z0-9]+$")
    private String regex;

    @Schema(description = FIELD_PROPERTY_RULES_MAX_LENGTH, example = "255")
    private Integer maxLength;

    @Schema(description = FIELD_PROPERTY_RULES_MIN_LENGTH, example = "1")
    private Integer minLength;

    @Schema(description = FIELD_PROPERTY_RULES_MAX_VALUE, example = "100")
    private Integer maxValue;

    @Schema(description = FIELD_PROPERTY_RULES_MIN_VALUE, example = "0")
    private Integer minValue;

}
