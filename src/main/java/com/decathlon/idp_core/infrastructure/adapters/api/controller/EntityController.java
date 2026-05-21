package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.BAD_REQUEST_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.CONFLICT_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.CREATED_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITIES_PAGINATED_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITIES_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_BY_IDENTIFIER_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_BY_IDENTIFIER_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_POST_ENTITY_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_POST_ENTITY_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_POST_SEARCH_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_POST_SEARCH_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_PUT_ENTITY_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_PUT_ENTITY_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FORBIDDEN_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.INTERNAL_SERVER_ERROR_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.NOT_FOUND_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.OK_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_PAGE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_SIZE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_SORT_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITIES_PAGINATED_SUCCESS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_CONFLICT;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_CREATED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_FOUND;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_UPDATED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INSUFFICIENT_RIGHTS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_ENTITY_DATA;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_PAGINATION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_SEARCH_QUERY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_SEARCH_SUCCESS;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import java.util.Set;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_UNAUTHORIZED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_UNEXPECTED_SERVER_ERROR;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.UNAUTHORIZED_CODE;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.InvalidQueryException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.service.entity.EntityService;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerConfiguration.EntityPageResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntitySearchRequestDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler.ErrorResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityDtoInMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityDtoOutMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntitySearchDomainMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/// REST API adapter providing entity management endpoints.
///
/// **Infrastructure specifics:**
/// - Exposes HTTP endpoints for entity CRUD operations
/// - Handles REST API request/response mapping between DTOs and domain models
/// - Integrates with OpenAPI/Swagger for API documentation
/// - Provides paginated responses for efficient data transfer
/// - Maps domain exceptions to appropriate HTTP status codes
@RestController
@RequestMapping("/api/v1/entities")
@Tag(name = "Entities Management", description = "Operations related to entity management")
@AllArgsConstructor
@Validated
public class EntityController {

    private final EntityService entityService;
    private final EntityDtoOutMapper entityDtoOutMapper;
    private final EntityDtoInMapper entityDtoInMapper;
    private final EntitySearchDomainMapper entitySearchDomainMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("identifier", "name", "templateIdentifier");

    /// Returns paginated entities filtered by template with HTTP pagination support.
    ///
    /// **API contract:** Provides paginated entity listings for template-specific views.
    /// Supports standard REST pagination parameters and returns appropriate HTTP status codes.
    /// Template validation is handled by the domain service layer.
    ///
    /// @param page               zero-based page index for pagination navigation
    /// @param size               number of entities per page for response size control
    /// @param templateIdentifier template filter for entity scope limitation
    /// @return paginated entity DTOs optimized for API consumers
    @Operation(summary = ENDPOINT_GET_ENTITIES_SUMMARY, description = ENDPOINT_GET_ENTITIES_PAGINATED_DESCRIPTION)
    @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITIES_PAGINATED_SUCCESS, content = @Content(schema = @Schema(implementation = EntityPageResponse.class)))
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_PAGINATION, content = {
            @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
    @Parameter(name = "page", description = PARAM_PAGE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "0")))
    @Parameter(name = "size", description = PARAM_SIZE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "20")))
    @Parameter(name = "sort", description = PARAM_SORT_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string", defaultValue = "identifier,asc")))
    @ResponseStatus(OK)
    @GetMapping("/{templateIdentifier}")
    public Page<EntityDtoOut> getEntities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @PathVariable String templateIdentifier) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Entity> entities = entityService.getEntitiesByTemplateIdentifier(pageable, templateIdentifier);
        return entityDtoOutMapper.fromEntitiesPageToDtoPage(entities, templateIdentifier);
    }

    /// Retrieves a single entity by template and entity identifiers.
    ///
    /// **API contract:** Provides specific entity lookup using compound identifier pattern.
    /// Returns HTTP 404 if either template or entity doesn't exist, maintaining REST semantics.
    ///
    /// @param templateIdentifier business template identifier for entity scope
    /// @param entityIdentifier   unique business identifier within template context
    /// @return entity DTO with full property and relationship data
    @Operation(summary = ENDPOINT_GET_ENTITY_BY_IDENTIFIER_SUMMARY, description = ENDPOINT_GET_ENTITY_BY_IDENTIFIER_DESCRIPTION)
    @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_FOUND, content = {
            @Content(schema = @Schema(implementation = EntityDtoOut.class))})
    @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER, content = {
            @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
    @GetMapping("/{templateIdentifier}/{entityIdentifier}")
    @ResponseStatus(OK)
    public EntityDtoOut getEntity(
            @PathVariable String templateIdentifier,
            @PathVariable String entityIdentifier) {
        Entity entity = entityService.getEntityByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier);
        return entityDtoOutMapper.fromEntity(entity);
    }

    /// Creates a new entity for the specified template with validation.
    ///
    /// **API contract:** Accepts entity creation payload and returns created entity with
    /// generated identifiers. Validates entity structure against template constraints
    /// and returns HTTP 201 on success, HTTP 400 for validation errors.
    ///
    /// @param templateIdentifier target template identifier for entity creation context
    /// @param entityDtoIn        entity creation payload with properties and relationships
    /// @return created entity DTO with server-generated identifiers
    @Operation(summary = ENDPOINT_POST_ENTITY_SUMMARY, description = ENDPOINT_POST_ENTITY_DESCRIPTION)
    @ApiResponse(responseCode = CREATED_CODE, description = RESPONSE_ENTITY_CREATED, content = {@Content(schema = @Schema(implementation = EntityDtoOut.class))})
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_ENTITY_DATA, content = {@Content(schema = @Schema(implementation = ErrorResponse.class))})
    @ApiResponse(responseCode = UNAUTHORIZED_CODE, description = RESPONSE_UNAUTHORIZED, content = @Content)
    @ApiResponse(responseCode = FORBIDDEN_CODE, description = RESPONSE_INSUFFICIENT_RIGHTS, content = @Content)
    @ApiResponse(responseCode = CONFLICT_CODE, description = RESPONSE_ENTITY_CONFLICT, content = {@Content(schema = @Schema(implementation = ErrorResponse.class))})
    @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER, content = {@Content(schema = @Schema(implementation = ErrorResponse.class))})
    @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = RESPONSE_UNEXPECTED_SERVER_ERROR, content = {@Content(schema = @Schema(implementation = ErrorResponse.class))})
    @PostMapping("/{templateIdentifier}")
    @ResponseStatus(CREATED)
    public EntityDtoOut createEntity(
            @NotBlank @PathVariable String templateIdentifier,
            @Valid @RequestBody EntityDtoIn entityDtoIn) {

        Entity entity = entityDtoInMapper.fromEntityDtoInToEntity(entityDtoIn, templateIdentifier);
        Entity savedEntity = entityService.createEntity(entity);
        return entityDtoOutMapper.fromEntity(savedEntity);
    }

    /// Updates an existing entity for the specified template.
    @Operation(summary = ENDPOINT_PUT_ENTITY_SUMMARY, description = ENDPOINT_PUT_ENTITY_DESCRIPTION)
    @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_UPDATED, content = {@Content(schema = @Schema(implementation = EntityDtoOut.class))})
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_ENTITY_DATA, content = {@Content(schema = @Schema(implementation = ErrorResponse.class))})
    @ApiResponse(responseCode = UNAUTHORIZED_CODE, description = RESPONSE_UNAUTHORIZED, content = @Content)
    @ApiResponse(responseCode = FORBIDDEN_CODE, description = RESPONSE_INSUFFICIENT_RIGHTS, content = @Content)
    @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER, content = {@Content(schema = @Schema(implementation = ErrorResponse.class))})
    @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = RESPONSE_UNEXPECTED_SERVER_ERROR, content = {@Content(schema = @Schema(implementation = ErrorResponse.class))})
    @PutMapping("/{templateIdentifier}/{entityIdentifier}")
    @ResponseStatus(OK)
    public EntityDtoOut updateEntity(
            @NotBlank @PathVariable String templateIdentifier,
            @NotBlank @PathVariable String entityIdentifier,
            @Valid @RequestBody EntityDtoIn entityDtoIn) {

        Entity entity = entityDtoInMapper.fromEntityDtoInToEntity(entityDtoIn, templateIdentifier);
        Entity updatedEntity = entityService.updateEntity(templateIdentifier, entityIdentifier, entity);
        return entityDtoOutMapper.fromEntity(updatedEntity);
    }

    /// Searches for entities across all templates using a nested filter query.
    ///
    /// **API contract:** Accepts a JSON body with a nested filter tree, pagination, and
    /// sorting parameters. Returns a paginated list of entities matching the filter.
    /// No template scoping is applied by default; include a template criterion
    /// in the filter to scope results to a specific template.
    ///
    /// @param searchRequest the search request body with filter, page, size, and sort
    /// @return paginated entity DTOs matching the filter
    @Operation(summary = ENDPOINT_POST_SEARCH_SUMMARY, description = ENDPOINT_POST_SEARCH_DESCRIPTION)
    @ApiResponse(responseCode = OK_CODE, description = RESPONSE_SEARCH_SUCCESS, content = @Content(schema = @Schema(implementation = EntityPageResponse.class)))
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_SEARCH_QUERY, content = {
            @Content(schema = @Schema(implementation = ErrorResponse.class)) })
    @PostMapping("/search")
    @ResponseStatus(OK)
    public Page<EntityDtoOut> searchEntities(@RequestBody EntitySearchRequestDtoIn searchRequest) {
        entitySearchDomainMapper.validateQuery(searchRequest.query());
        SearchFilterNode filter = entitySearchDomainMapper.toDomain(searchRequest.filter());
        Pageable pageable = buildPageable(searchRequest);
        Page<Entity> entities = entityService.searchEntities(filter, searchRequest.query(), pageable);
        return entityDtoOutMapper.fromEntitiesSearchPageToDtoPage(entities);
    }

    private Pageable buildPageable(EntitySearchRequestDtoIn searchRequest) {
        int page = searchRequest.page();
        int size = searchRequest.size() > 0 ? searchRequest.size() : 20;
        if (size > EntitySearchDomainMapper.MAX_PAGE_SIZE) {
            throw new InvalidQueryException(
                    ValidationMessages.SEARCH_PAGE_SIZE_TOO_LARGE.formatted(EntitySearchDomainMapper.MAX_PAGE_SIZE));
        }
        if (searchRequest.sort() == null || searchRequest.sort().isBlank()) {
            return PageRequest.of(page, size);
        }
        Sort sort = parseSortExpression(searchRequest.sort());
        return PageRequest.of(page, size, sort);
    }

    private Sort parseSortExpression(String sortExpression) {
        String[] parts = sortExpression.split(":");
        String property = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(property)) {
            throw new InvalidQueryException(
                    ValidationMessages.SEARCH_INVALID_SORT_FIELD.formatted(property));
        }
        Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}
