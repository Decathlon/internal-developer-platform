package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_ENTITY_CREATE_IN;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/// Input DTO for creating a new entity within a template scope.
///
/// **Infrastructure validation:** Performs syntactic validation at the API boundary
/// using Jakarta Validation annotations. Semantic validation (schema conformance
/// against template definitions) is handled by the domain service layer.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = SCHEMA_ENTITY_CREATE_IN)
public class EntityCreateDtoIn {

    @NotBlank(message = ENTITY_IDENTIFIER_MANDATORY)
    @Schema(description = FIELD_ENTITY_IDENTIFIER, example = "my-web-service")
    private String identifier;

    @Valid
    @JsonUnwrapped
    private EntityDtoInCommonFields entityDtoInCommonFields;
}
