package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.BAD_REQUEST_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.CREATED_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_DELETE_TEMPLATE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_DELETE_TEMPLATE_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_TEMPLATES_PAGINATED_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_TEMPLATES_PAGINATED_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_TEMPLATE_BY_IDENTIFIER_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_TEMPLATE_BY_IDENTIFIER_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_POST_TEMPLATE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_POST_TEMPLATE_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_PUT_TEMPLATE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_PUT_TEMPLATE_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.NOT_FOUND_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.NO_CONTENT_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.OK_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_PAGE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_SIZE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_SORT_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_PAGINATION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_TEMPLATE_DATA;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_TEMPLATES_PAGINATED_SUCCESS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_TEMPLATE_CREATED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_TEMPLATE_DELETED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_TEMPLATE_FOUND;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_TEMPLATE_UPDATED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.service.EntityTemplateService;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerConfiguration.TemplatePageResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityTemplateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate.EntityTemplateDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler.ErrorResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entitytemplate.EntityTemplateMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/// REST API adapter for Entity Template management operations.
///
/// **Infrastructure responsibilities:**
/// - HTTP endpoint exposure for template CRUD operations with OpenAPI documentation
/// - Request/response DTO mapping between API layer and domain services
/// - REST-compliant status codes and pagination support for management interfaces
/// - Validation integration with centralized exception handling
///
/// **API design principles:**
/// - Resource-based URLs following REST conventions for template management
/// - Comprehensive OpenAPI documentation for interactive testing and client generation
/// - Consistent error responses through centralized [ApiExceptionHandler]
/// - Domain-driven design separation with business logic in service layer
///
/// **Template management workflow:** Supports full lifecycle operations including
/// paginated listing, identifier-based lookup, creation with uniqueness validation,
/// updates with conflict resolution, and safe deletion with referential checks.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/entity-templates")
@Tag(name = "Entities Templates Management", description = "Operations related to entity template management")
public class EntityTemplateController {

        private final EntityTemplateService entityTemplateService;
        private final EntityTemplateMapper templateMapper;

        /// Retrieves paginated entity templates for administrative interfaces.
        ///
        /// **API contract:** Provides paginated template listings with configurable sorting
        /// and page size. Defaults to 20 templates per page sorted by identifier for
        /// consistent management interface display.
        @Operation(summary = ENDPOINT_GET_TEMPLATES_PAGINATED_SUMMARY, description = ENDPOINT_GET_TEMPLATES_PAGINATED_DESCRIPTION)
        @ApiResponse(responseCode = OK_CODE, description = RESPONSE_TEMPLATES_PAGINATED_SUCCESS, content = @Content(schema = @Schema(implementation = TemplatePageResponse.class)))
        @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_PAGINATION, content = {
                        @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)) })
        @Parameter(name = "page", description = PARAM_PAGE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "0")))
        @Parameter(name = "size", description = PARAM_SIZE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "20")))
        @Parameter(name = "sort", description = PARAM_SORT_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string", defaultValue = "identifier,asc")))
        @GetMapping
        @ResponseStatus(OK)
        public Page<EntityTemplateDtoOut> getTemplatesPaginated(
                        @PageableDefault(size = 20, sort = "identifier") @Parameter(hidden = true) Pageable pageable) {
                Page<EntityTemplate> templates = entityTemplateService.getEntityTemplates(pageable);
                return templates.map(templateMapper::fromEntityTemplatetoDto);
        }

        /// Retrieves specific entity template by business identifier.
        ///
        /// **API contract:** Returns complete template definition using case-sensitive
        /// business identifier lookup. Provides HTTP 404 for non-existent templates
        /// with meaningful error messages for API consumers.
        @Operation(summary = ENDPOINT_GET_TEMPLATE_BY_IDENTIFIER_SUMMARY, description = ENDPOINT_GET_TEMPLATE_BY_IDENTIFIER_DESCRIPTION)
        @ApiResponse(responseCode = OK_CODE, description = RESPONSE_TEMPLATE_FOUND, content = {
                        @Content(schema = @Schema(implementation = EntityTemplateDtoOut.class)) })
        @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER, content = {
                        @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)) })
        @GetMapping("/identifier/{identifier}")
        @ResponseStatus(OK)
        public EntityTemplateDtoOut getTemplateByIdentifier(@PathVariable String identifier) {
                EntityTemplate entity = entityTemplateService.getEntityTemplateByIdentifier(identifier);
                return templateMapper.fromEntityTemplatetoDto(entity);
        }

        /// Creates new entity template with validation and uniqueness checks.
        ///
        /// **API contract:** Accepts template creation payload with comprehensive validation.
        /// Returns HTTP 201 with created template including generated identifiers, or
        /// HTTP 409 for duplicate identifier conflicts.
        @Operation(summary = ENDPOINT_POST_TEMPLATE_SUMMARY, description = ENDPOINT_POST_TEMPLATE_DESCRIPTION)
        @ApiResponse(responseCode = CREATED_CODE, description = RESPONSE_TEMPLATE_CREATED, content = {
                        @Content(schema = @Schema(implementation = EntityTemplateDtoOut.class)) })
        @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_TEMPLATE_DATA, content = {
                        @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)) })
        @PostMapping
        @ResponseStatus(CREATED)
        public EntityTemplateDtoOut createTemplate(@Valid @RequestBody EntityTemplateDtoIn templateDto) {
                EntityTemplate entityTemplate = entityTemplateService.createEntityTemplate(templateMapper.fromDtoToEntityTemplate(templateDto));
                return templateMapper.fromEntityTemplatetoDto(entityTemplate);
        }

        /// Updates existing entity template with complete replacement strategy.
        ///
        /// **API contract:** Replaces entire template definition while preserving identifier.
        /// Returns updated template with HTTP 200, or HTTP 404 for non-existent templates.
        @Operation(summary = ENDPOINT_PUT_TEMPLATE_SUMMARY, description = ENDPOINT_PUT_TEMPLATE_DESCRIPTION)
        @ApiResponse(responseCode = OK_CODE, description = RESPONSE_TEMPLATE_UPDATED, content = {
                        @Content(schema = @Schema(implementation = EntityTemplateDtoOut.class)) })
        @ApiResponse(responseCode = "404", description = RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER, content = {
                        @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)) })
        @PutMapping("identifier/{identifier}")
        public EntityTemplateDtoOut updateTemplate(
                        @PathVariable(name = "identifier") String identifier,
                        @RequestBody @Valid EntityTemplateDtoIn updatedTemplateDto) {
                EntityTemplate entityTemplate = entityTemplateService.putEntityTemplate(identifier, templateMapper.fromDtoToEntityTemplate(updatedTemplateDto));
                return templateMapper.fromEntityTemplatetoDto(entityTemplate);
        }

        /// Deletes entity template by business identifier with safety checks.
        ///
        /// **API contract:** Permanently removes template with HTTP 204 response.
        /// Operation is idempotent - returns success even for non-existent templates.
        /// **Warning:** Irreversible operation requiring referential integrity validation.
        @Operation(summary = ENDPOINT_DELETE_TEMPLATE_SUMMARY, description = ENDPOINT_DELETE_TEMPLATE_DESCRIPTION)
        @ApiResponse(responseCode = NO_CONTENT_CODE, description = RESPONSE_TEMPLATE_DELETED)
        @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER, content = {
                        @Content(schema = @Schema(implementation = ErrorResponse.class))
        })
        @ResponseStatus(NO_CONTENT)
        @DeleteMapping("/identifier/{identifier}")
        public void deleteTemplate(@PathVariable String identifier) {
                entityTemplateService.deleteEntityTemplate(identifier);
        }
}
