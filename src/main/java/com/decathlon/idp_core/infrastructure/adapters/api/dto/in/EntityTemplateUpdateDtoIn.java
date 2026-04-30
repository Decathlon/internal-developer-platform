package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_ENTITY_TEMPLATE_UPDATE_IN;

/**
 * **Input DTO for updating entity templates.**
 *
 * - Used as the request body for PUT operations on entity templates.
 * - Composes all updatable fields from {@link EntityTemplateCommonFields} and flattens them into the top-level JSON using {@code @JsonUnwrapped}.
 * - Fields are validated using Jakarta Validation annotations.
 * - Follows composition over inheritance for maintainability and clarity.
 *
 * @see EntityTemplateCommonFields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = SCHEMA_ENTITY_TEMPLATE_UPDATE_IN)
public class EntityTemplateUpdateDtoIn {

    @Valid
    @JsonUnwrapped
    private EntityTemplateDtoInCommonFields commonFields;
}
