package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_PROPERTIES;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_RELATIONS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_RELATION_NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_RELATION_TARGETS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_ENTITY_IN;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_ENTITY_RELATION_IN;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NAME_MANDATORY_SIMPLE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_IDENTIFIERS_NOT_NULL;

import java.util.List;
import java.util.Map;

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
@Schema(description = SCHEMA_ENTITY_IN)
public class EntityDtoIn {

    @NotBlank(message = ENTITY_NAME_MANDATORY)
    @Schema(description = FIELD_ENTITY_NAME, example = "my-web-service")
    private String name;

    @NotBlank(message = ENTITY_IDENTIFIER_MANDATORY)
    @Schema(description = FIELD_ENTITY_IDENTIFIER, example = "my-web-service")
    private String identifier;

    @Schema(description = FIELD_ENTITY_PROPERTIES, example = "{\"port\": \"8080\", \"environment\": \"dev\"}")
    private Map<String, Object> properties;

    @Valid
    @Schema(description = FIELD_ENTITY_RELATIONS)
    private List<RelationDtoIn> relations;

    /// Input DTO for an entity relation instance.
    ///
    /// **Infrastructure validation:** Validates relation name presence and target
    /// identifiers at the API boundary before domain-level schema checks.
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(SnakeCaseStrategy.class)
    @Schema(description = SCHEMA_ENTITY_RELATION_IN)
    public static class RelationDtoIn {

        @NotBlank(message = RELATION_NAME_MANDATORY_SIMPLE)
        @Schema(description = FIELD_ENTITY_RELATION_NAME, example = "depends-on")
        private String name;

        @NotNull(message = RELATION_TARGET_IDENTIFIERS_NOT_NULL)
        @Schema(description = FIELD_ENTITY_RELATION_TARGETS, example = "[\"web-api-1\", \"web-api-2\"]")
        private List<String> targetEntityIdentifiers;
    }
}
