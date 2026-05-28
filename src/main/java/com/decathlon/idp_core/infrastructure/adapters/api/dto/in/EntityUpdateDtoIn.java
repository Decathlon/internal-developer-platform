package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_ENTITY_UPDATE_IN;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

/// Input DTO for updating an entity within a template scope.
///
/// **Infrastructure validation:** Performs syntactic validation at the API boundary
/// using Jakarta Validation annotations. Semantic validation (schema conformance
/// against template definitions) is handled by the domain service layer.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = SCHEMA_ENTITY_UPDATE_IN)
public class EntityUpdateDtoIn {

    @Valid
    @JsonUnwrapped
    private EntityDtoInCommonFields entityDtoInCommonFields;

}
