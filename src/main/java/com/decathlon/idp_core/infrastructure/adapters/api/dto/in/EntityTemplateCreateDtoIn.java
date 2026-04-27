package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_TEMPLATE_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_ENTITY_TEMPLATE_CREATE_IN;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * **Input DTO for creating entity templates.**
 *
 * - Used as the request body for POST operations on entity templates.
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
@Schema(description = SCHEMA_ENTITY_TEMPLATE_CREATE_IN)
public class EntityTemplateCreateDtoIn {

    @NotBlank(message = TEMPLATE_IDENTIFIER_MANDATORY)
    @Schema(description = FIELD_TEMPLATE_IDENTIFIER, example = "service")
    private String identifier;

    @Valid
    @JsonUnwrapped
    private EntityTemplateDtoInCommonFields commonFields;
}
