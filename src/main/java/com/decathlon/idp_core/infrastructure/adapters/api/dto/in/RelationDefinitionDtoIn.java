package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_TARGET_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_RELATION_NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_RELATION_REQUIRED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_RELATION_TARGET_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_RELATION_TO_MANY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_RELATION_DEFINITION_IN;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = SCHEMA_RELATION_DEFINITION_IN)
public class RelationDefinitionDtoIn {

    @NotBlank(message = RELATION_NAME_MANDATORY)
    @Schema(description = FIELD_RELATION_NAME, example = "dependencies")
    private String name;

    @NotBlank(message = RELATION_TARGET_IDENTIFIER_MANDATORY)
    @Schema(description = FIELD_RELATION_TARGET_IDENTIFIER, example = "service")
    private String targetEntityIdentifier;

    @Builder.Default
    @Schema(description = FIELD_RELATION_REQUIRED, example = "false", defaultValue = "false")
    private boolean required = false;

    @Builder.Default
    @Schema(description = FIELD_RELATION_TO_MANY, example = "true", defaultValue = "false")
    private boolean toMany = false;
}
