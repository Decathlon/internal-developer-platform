package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_DESCRIPTION_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_TYPE_MANDATORY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_REQUIRED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_RULES;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_PROPERTY_TYPE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_PROPERTY_DEFINITION_IN;

import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = SCHEMA_PROPERTY_DEFINITION_IN)
public class PropertyDefinitionDtoIn {
    @NotBlank(message = PROPERTY_NAME_MANDATORY)
    @Schema(description = FIELD_PROPERTY_NAME, example = "applicationName")
    private String name;

    @NotBlank(message = PROPERTY_DESCRIPTION_MANDATORY)
    @Schema(description = FIELD_PROPERTY_DESCRIPTION, example = "Name of the application")
    private String description;

    @NotNull(message = PROPERTY_TYPE_MANDATORY)
    @Schema(description = FIELD_PROPERTY_TYPE, example = "STRING")
    private PropertyType type;

    @Builder.Default
    @Schema(description = FIELD_PROPERTY_REQUIRED, example = "true", defaultValue = "false")
    private boolean required = false;

    @Valid
    @Schema(description = FIELD_PROPERTY_RULES)
    private PropertyRulesDtoIn rules;
}
