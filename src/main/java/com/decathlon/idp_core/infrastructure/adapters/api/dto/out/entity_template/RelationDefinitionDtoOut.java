package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_template;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_RELATION_NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_RELATION_REQUIRED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_RELATION_TARGET_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_RELATION_TO_MANY;

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
@Schema(description = "Output DTO for relation definition")
public class RelationDefinitionDtoOut {

    @Schema(description = FIELD_RELATION_NAME, example = "dependencies")
    private String name;

    @Schema(description = FIELD_RELATION_TARGET_IDENTIFIER, example = "component-template")
    private String targetTemplateIdentifier;

    @Schema(description = FIELD_RELATION_REQUIRED, example = "false")
    private boolean required;

    @Schema(description = FIELD_RELATION_TO_MANY, example = "true")
    private boolean toMany;

}
