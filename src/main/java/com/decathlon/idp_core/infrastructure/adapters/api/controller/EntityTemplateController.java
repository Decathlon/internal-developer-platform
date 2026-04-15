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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decathlon.idp_core.domain.exception.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNotFoundException;
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

/**
 * REST controller for managing Entity Template resources.
 * <p>
 * This controller provides REST API endpoints for Entity Template operations
 * including:
 * <ul>
 * <li>Retrieving paginated lists of templates</li>
 * <li>Finding templates by their unique identifier</li>
 * <li>Creating new templates with validation</li>
 * <li>Deleting templates by identifier</li>
 * </ul>
 * </p>
 * <p>
 * The controller follows Domain-Driven Design (DDD) principles by:
 * <ul>
 * <li>Working with domain entities through the service layer</li>
 * <li>Handling DTO mapping at the API boundary</li>
 * <li>Delegating business logic to the service layer</li>
 * </ul>
 * </p>
 * <p>
 * All endpoints are documented with OpenAPI/Swagger annotations and handle
 * validation errors through the centralized {@link ApiExceptionHandler}.
 * </p>
 *
 * @author IDP Core Team
 * @see EntityTemplateService
 * @see EntityTemplateMapper
 * @see ApiExceptionHandler
 * @since 1.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/entity-templates")
@Tag(name = "Entities Templates Management", description = "Operations related to entity template management")
public class EntityTemplateController {

        private final EntityTemplateService entityTemplateService;
        private final EntityTemplateMapper templateMapper;

        /**
         * Retrieves a paginated list of Entity Templates.
         * <p>
         * This endpoint supports pagination and sorting through the {@code Pageable}
         * parameter.
         * Default pagination is set to 20 items per page, sorted by identifier.
         * </p>
         * <p>
         * The response includes pagination metadata such as total elements, total
         * pages,
         * current page number, and the templates for the requested page.
         * </p>
         *
         * @param pageable pagination and sorting parameters with default size of 20 and
         *                 sort by identifier
         * @return {@link ResponseEntity} containing a {@link Page} of
         *         {@link EntityTemplateDtoOut} with HTTP 200 status
         */
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

        /**
         * Retrieves an Entity Template by its unique identifier.
         * <p>
         * This endpoint allows clients to fetch a specific template using its business
         * identifier
         * rather than its internal UUID. The identifier is case-sensitive and must
         * match exactly.
         * </p>
         *
         * @param identifier the unique business identifier of the template to retrieve,
         *                   must not be null or empty
         * @return {@link ResponseEntity} containing the {@link EntityTemplateDtoOut}
         *         with HTTP 200 status if found
         * @throws EntityTemplateNotFoundException if no template exists with the given
         *                                         identifier (returns HTTP 404)
         * @throws IllegalArgumentException        if identifier is null or empty
         */
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

        /**
         * Creates a new Entity Template.
         * <p>
         * This endpoint accepts a template definition and creates a new template in the
         * system. The request body is validated using Bean Validation annotations, and
         * the
         * template identifier must be unique across all existing templates.
         * </p>
         * <p>
         * The created template is returned in the response body with its generated UUID
         * and any system-assigned metadata.
         * </p>
         *
         * @param templateDto the template data to create, must not be null and must
         *                    pass validation
         * @return the created {@link EntityTemplateDtoOut} with HTTP 201 status
         * @throws EntityTemplateAlreadyExistsException if template identifier already
         *                                              exists (returns HTTP 409)
         */
        @Operation(summary = ENDPOINT_POST_TEMPLATE_SUMMARY, description = ENDPOINT_POST_TEMPLATE_DESCRIPTION)
        @ApiResponse(responseCode = CREATED_CODE, description = RESPONSE_TEMPLATE_CREATED, content = {
                        @Content(schema = @Schema(implementation = EntityTemplateDtoOut.class)) })
        @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_TEMPLATE_DATA, content = {
                        @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)) })
        @PostMapping
        @ResponseStatus(CREATED)
        public EntityTemplateDtoOut createTemplate(@Valid @RequestBody EntityTemplateDtoIn templateDto) {
                EntityTemplate entity = templateMapper.fromDtoToEntityTemplate(templateDto);
                EntityTemplate savedEntity = entityTemplateService.saveEntityTemplate(entity);
                return templateMapper.fromEntityTemplatetoDto(savedEntity);
        }

        /**
         * Update an existing Entity Template by its unique template identifier.
         *
         * @param identifier         The template identifier
         * @param updatedTemplateDto The entity data to update with
         */
        @Operation(summary = ENDPOINT_PUT_TEMPLATE_SUMMARY, description = ENDPOINT_PUT_TEMPLATE_DESCRIPTION)
        @ApiResponse(responseCode = OK_CODE, description = RESPONSE_TEMPLATE_UPDATED, content = {
                        @Content(schema = @Schema(implementation = EntityTemplateDtoOut.class)) })
        @ApiResponse(responseCode = "404", description = RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER, content = {
                        @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)) })
        @PutMapping("identifier/{identifier}")
        public EntityTemplateDtoOut updateTemplate(
                        @PathVariable(name = "identifier") String identifier,
                        @RequestBody @Valid EntityTemplateDtoIn updatedTemplateDto) {
                EntityTemplate entityTemplate = entityTemplateService.putEntityTemplate(identifier,
                                templateMapper.fromDtoToEntityTemplate(updatedTemplateDto));
                return templateMapper.fromEntityTemplatetoDto(entityTemplate);
        }

        /**
         * Deletes an Entity Template by its unique identifier.
         * <p>
         * This endpoint permanently removes the specified template from the system.
         * The operation is idempotent - if the template doesn't exist, it will still
         * return HTTP 204 (No Content) status.
         * </p>
         * <p>
         * <strong>Warning:</strong> This operation cannot be undone. Ensure that the
         * template is not referenced by other entities before deletion.
         * </p>
         *
         * @param identifier the unique business identifier of the template to delete,
         *                   must not be null or empty
         * @throws IllegalArgumentException if identifier is null or empty
         */
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
