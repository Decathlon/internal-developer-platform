package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.BAD_REQUEST_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.CONFLICT_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.CREATED_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_DELETE_ENTITY_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_DELETE_ENTITY_SUMMARY;
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
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.NO_CONTENT_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.OK_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_PAGE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_QUERY_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_SIZE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_SORT_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITIES_PAGINATED_SUCCESS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_CONFLICT;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_CREATED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_DELETED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_FOUND;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_RELATION_CONFLICT;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_UPDATED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INSUFFICIENT_RIGHTS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_ENTITY_DATA;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_PAGINATION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_QUERY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_SEARCH_QUERY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_SEARCH_SUCCESS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_UNAUTHORIZED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_UNEXPECTED_SERVER_ERROR;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.UNAUTHORIZED_CODE;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.model.search.PaginatedResult;
import com.decathlon.idp_core.domain.model.search.PaginationCriteria;
import com.decathlon.idp_core.domain.model.search.RawSearchFilterNode;
import com.decathlon.idp_core.domain.model.search.SearchFilterNode;
import com.decathlon.idp_core.domain.service.entity.EntityService;
import com.decathlon.idp_core.domain.service.entity_graph.EntityGraphService;
import com.decathlon.idp_core.domain.service.filter.EntityFilterDslParser;
import com.decathlon.idp_core.domain.service.search.SearchFilterParser;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerConfiguration.EntityPageResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntitySearchDepthRequestDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntitySearchRequestDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityUpdateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.FilterNodeDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDepDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler.ErrorResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityDepDtoOutMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityDtoInMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityDtoOutMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.SearchFilterMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

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
@Validated
@RequiredArgsConstructor
public class EntityController {

  private final EntityService entityService;
  private final EntityGraphService entityGraphService;
  private final EntityDtoOutMapper entityDtoOutMapper;
  private final EntityDtoInMapper entityDtoInMapper;
  private final EntityFilterDslParser entityFilterDslParser;
  private final SearchFilterMapper searchFilterMapper;
  private final SearchFilterParser searchFilterParser;
  private final EntityDepDtoOutMapper entityDepDtoOutMapper;

  /// Returns paginated entities filtered by template with HTTP pagination
  /// support.
  ///
  /// **API contract:** Provides paginated entity listings for template-specific
  /// views. Supports standard REST pagination parameters and an optional `q`
  /// filter query. Template validation is handled by the domain service layer.
  ///
  /// @param page zero-based page index for pagination navigation
  /// @param size number of entities per page for response size
  /// control
  /// @param templateIdentifier template filter for entity scope limitation
  /// @param q optional filter query string (e.g.
  /// `name:API;property.language=JAVA`)
  /// @return paginated entity DTOs matching the template and optional filter
  @Operation(summary = ENDPOINT_GET_ENTITIES_SUMMARY, description = ENDPOINT_GET_ENTITIES_PAGINATED_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITIES_PAGINATED_SUCCESS, content = @Content(schema = @Schema(implementation = EntityPageResponse.class)))
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_PAGINATION, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_QUERY, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @Parameter(name = "page", description = PARAM_PAGE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "0")))
  @Parameter(name = "size", description = PARAM_SIZE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "20")))
  @Parameter(name = "sort", description = PARAM_SORT_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string", defaultValue = "identifier,asc")))
  @Parameter(name = "q", description = PARAM_QUERY_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string")))
  @ResponseStatus(OK)
  @GetMapping("/{templateIdentifier}")
  public Page<EntityDtoOut> getEntities(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size, @PathVariable String templateIdentifier,
      @RequestParam(required = false) String q) {
    Pageable pageable = PageRequest.of(page, size);
    EntityFilter filter = entityFilterDslParser.parse(q);
    Page<Entity> entities = entityService.getEntitiesByTemplateIdentifier(pageable,
        templateIdentifier, filter);
    return entityDtoOutMapper.fromEntitiesPageToDtoPage(entities, templateIdentifier);
  }

  /// Retrieves a single entity by template and entity identifiers.
  ///
  /// **API contract:** Provides specific entity lookup using compound identifier
  /// pattern. Returns HTTP 404 if either template or entity doesn't exist,
  /// maintaining REST semantics.
  ///
  /// @param templateIdentifier business template identifier for entity scope
  /// @param entityIdentifier unique business identifier within template context
  /// @return entity DTO with full property and relationship data
  @Operation(summary = ENDPOINT_GET_ENTITY_BY_IDENTIFIER_SUMMARY, description = ENDPOINT_GET_ENTITY_BY_IDENTIFIER_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_FOUND, content = {
      @Content(schema = @Schema(implementation = EntityDtoOut.class))})
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @GetMapping("/{templateIdentifier}/{entityIdentifier}")
  @ResponseStatus(OK)
  public EntityDtoOut getEntity(@PathVariable String templateIdentifier,
      @PathVariable String entityIdentifier) {
    Entity entity = entityService.getEntityByTemplateIdentifierAndIdentifier(templateIdentifier,
        entityIdentifier);
    return entityDtoOutMapper.fromEntity(entity);
  }

  /// Creates a new entity for the specified template with validation.
  ///
  /// **API contract:** Accepts entity creation payload and returns created entity
  /// with generated identifiers. Validates entity structure against template
  /// constraints and returns HTTP 201 on success, HTTP 400 for validation errors.
  ///
  /// @param templateIdentifier target template identifier for entity creation
  /// context
  /// @param entityCreateDtoIn entity creation payload with properties and
  /// relationships
  /// @return created entity DTO with server-generated identifiers
  @Operation(summary = ENDPOINT_POST_ENTITY_SUMMARY, description = ENDPOINT_POST_ENTITY_DESCRIPTION)
  @ApiResponse(responseCode = CREATED_CODE, description = RESPONSE_ENTITY_CREATED, content = {
      @Content(schema = @Schema(implementation = EntityDtoOut.class))})
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_ENTITY_DATA, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = UNAUTHORIZED_CODE, description = RESPONSE_UNAUTHORIZED, content = @Content)
  @ApiResponse(responseCode = FORBIDDEN_CODE, description = RESPONSE_INSUFFICIENT_RIGHTS, content = @Content)
  @ApiResponse(responseCode = CONFLICT_CODE, description = RESPONSE_ENTITY_CONFLICT, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = RESPONSE_UNEXPECTED_SERVER_ERROR, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @PostMapping("/{templateIdentifier}")
  @ResponseStatus(CREATED)
  public EntityDtoOut createEntity(@NotBlank @PathVariable String templateIdentifier,
      @Valid @RequestBody EntityCreateDtoIn entityCreateDtoIn) {

    Entity entity = entityDtoInMapper.fromPostEntityDtoInToEntity(entityCreateDtoIn,
        templateIdentifier);
    Entity savedEntity = entityService.createEntity(entity);
    return entityDtoOutMapper.fromEntity(savedEntity);
  }

  /// Updates an existing entity for the specified template.
  ///
  /// **API contract:** Accepts entity update payload and returns updated entity.
  /// Validates that the entity exists and that the update payload conforms to
  /// template constraints. Returns HTTP 200 on success, HTTP 400 for validation
  /// errors, HTTP 404 if entity doesn't exist.
  ///
  /// @param templateIdentifier target template identifier for entity update
  /// context
  /// @param entityIdentifier unique business identifier of the entity to update
  /// @param entityUpdateDtoIn entity update payload with properties and
  /// relationships to apply
  /// @return updated entity DTO reflecting persisted changes
  @Operation(summary = ENDPOINT_PUT_ENTITY_SUMMARY, description = ENDPOINT_PUT_ENTITY_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_UPDATED, content = {
      @Content(schema = @Schema(implementation = EntityDtoOut.class))})
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_ENTITY_DATA, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = UNAUTHORIZED_CODE, description = RESPONSE_UNAUTHORIZED, content = @Content)
  @ApiResponse(responseCode = FORBIDDEN_CODE, description = RESPONSE_INSUFFICIENT_RIGHTS, content = @Content)
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = RESPONSE_UNEXPECTED_SERVER_ERROR, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @PutMapping("/{templateIdentifier}/{entityIdentifier}")
  @ResponseStatus(OK)
  public EntityDtoOut updateEntity(@NotBlank @PathVariable String templateIdentifier,
      @NotBlank @PathVariable String entityIdentifier,
      @Valid @RequestBody EntityUpdateDtoIn entityUpdateDtoIn) {

    Entity entity = entityDtoInMapper.fromPutEntityDtoInToEntity(entityUpdateDtoIn,
        templateIdentifier, entityIdentifier);
    Entity updatedEntity = entityService.updateEntity(templateIdentifier, entityIdentifier, entity);
    return entityDtoOutMapper.fromEntity(updatedEntity);
  }

  /// Deletes an existing entity identified by template and entity identifiers.
  ///
  /// **API contract:** Validates the template and entity exist, cleans up
  /// relations in parent entities that reference the deleted entity, then deletes
  /// the entity. Returns HTTP 204 on successful deletion, HTTP 404 if entity
  /// doesn't exist, HTTP 400 if deletion is not allowed due to existing
  /// references.
  ///
  /// @param templateIdentifier the template identifier of the entity to delete
  /// @param entityIdentifier the identifier of the entity to delete
  @Operation(summary = ENDPOINT_DELETE_ENTITY_SUMMARY, description = ENDPOINT_DELETE_ENTITY_DESCRIPTION)
  @ApiResponse(responseCode = NO_CONTENT_CODE, description = RESPONSE_ENTITY_DELETED)
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_ENTITY_DATA, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = UNAUTHORIZED_CODE, description = RESPONSE_UNAUTHORIZED, content = @Content)
  @ApiResponse(responseCode = FORBIDDEN_CODE, description = RESPONSE_INSUFFICIENT_RIGHTS, content = @Content)
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = CONFLICT_CODE, description = RESPONSE_ENTITY_RELATION_CONFLICT, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = RESPONSE_UNEXPECTED_SERVER_ERROR, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @DeleteMapping("/{templateIdentifier}/{entityIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteEntity(@NotBlank @PathVariable String templateIdentifier,
      @NotBlank @PathVariable String entityIdentifier) {
    entityService.deleteEntity(templateIdentifier, entityIdentifier);
  }

  /// Retrieves paginated entities with their dependency graphs up to specified
  /// depth.
  ///
  /// **API contract:** Returns a paginated list of entities with their outbound
  /// and inbound relations merged into a single relations object. Each entity
  /// includes all reachable nodes up to the specified depth using DIRECT_LINEAGE
  /// traversal mode. Results are returned as EntityDepDtoOut DTOs with relations
  /// merged from both directions.
  ///
  /// @param page zero-based page index for pagination navigation
  /// @param size number of entities per page for response size
  /// control
  /// @param templateIdentifier template filter for entity scope limitation
  /// @param depth maximum relation traversal depth (default 1,
  /// Retrieves paginated entities with their dependency
  /// graphs up to specified depth.
  ///
  /// **API contract:** Returns a paginated list of
  /// entities with their outbound and inbound relations
  /// merged into a single relations object. Each entity
  /// includes all reachable nodes up to the specified
  /// depth using DIRECT_LINEAGE traversal mode. Results
  /// are returned as EntityDepDtoOut DTOs with relations
  /// merged from both directions. Supports filtering
  /// relations by name using a comma-separated list.
  ///
  /// @param page zero-based page index for pagination navigation
  /// @param size number of entities per page for response size
  /// control
  /// @param templateIdentifier template filter for entity scope limitation
  /// @param depth maximum relation traversal depth (default 1,
  /// clamped to 1-6)
  /// @param relationsFilter comma-separated list of relation names to include
  /// (optional, empty means all relations)
  /// @param q optional filter query string for entity filtering
  /// @return paginated entity dependency DTOs with merged relations up to depth
  @Operation(summary = "Get entity dependencies", description = "Retrieve entities with their relationship graphs up to specified depth")
  @ApiResponse(responseCode = OK_CODE, description = "Entity dependencies retrieved successfully", content = @Content(schema = @Schema(implementation = EntityPageResponse.class)))
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_PAGINATION, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @Parameter(name = "page", description = PARAM_PAGE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "0")))
  @Parameter(name = "size", description = PARAM_SIZE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "20")))
  @Parameter(name = "sort", description = PARAM_SORT_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string", defaultValue = "identifier,asc")))
  @Parameter(name = "depth", description = "Maximum relation traversal depth (1-6, default 1)", in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "1")))
  @Parameter(name = "relations_filter", description = "Comma-separated list of relation names to include (optional, empty means all relations)", in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string", example = "component-supported_by-support_group,component-owned_by-product")))
  @Parameter(name = "q", description = PARAM_QUERY_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string")))
  @ResponseStatus(OK)
  @GetMapping("/{templateIdentifier}/dependencies")
  public Page<EntityDepDtoOut> getEntitiesDependencies(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size, @PathVariable String templateIdentifier,
      @RequestParam(defaultValue = "1") int depth,
      @RequestParam(required = false, defaultValue = "") String relationsFilter,
      @RequestParam(required = false) String q) {
    Pageable pageable = PageRequest.of(page, size);
    EntityFilter filter = entityFilterDslParser.parse(q);
    Page<Entity> entities = entityService.getEntitiesByTemplateIdentifier(pageable,
        templateIdentifier, filter);

    // Extract entity identifiers for batch graph loading
    List<String> entityIdentifiers = entities.getContent().stream().map(Entity::identifier)
        .toList();

    if (entityIdentifiers.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    // Parse relations filter from comma-separated list
    java.util.Set<String> relationFilterSet = java.util.Arrays.stream(relationsFilter.split(","))
        .map(String::trim).filter(s -> !s.isBlank()).collect(java.util.stream.Collectors.toSet());

    // Load entity graphs with DIRECT_LINEAGE mode (includes outbound + inbound
    // relations)
    Map<String, EntityGraphNode> entityGraphs = entityGraphService
        .getBatchEntityGraphsByIdentifiers(templateIdentifier, entityIdentifiers, depth, false,
            relationFilterSet, java.util.Set.of(), EntityGraphTraversalMode.DIRECT_LINEAGE);

    // Map to EntityDepDtoOut with merged relations
    List<EntityDepDtoOut> dtoOutList = entities.getContent().stream().map(entity -> {
      EntityGraphNode graphNode = entityGraphs.getOrDefault(entity.identifier(), null);
      return entityDepDtoOutMapper.toDto(graphNode);
    }).toList();

    return new PageImpl<>(dtoOutList, pageable, entities.getTotalElements());
  }

  @ResponseStatus(OK)
  @GetMapping("/{templateIdentifier}/relation-template")
  public Page<EntityDepDtoOut> getEntitiesByRelationTemplate(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      @RequestParam List<String> filterIds, @PathVariable String templateIdentifier,
      @RequestParam(defaultValue = "1") int depth,
      @RequestParam(required = false) String startTemplate) {
    Pageable pageable = PageRequest.of(page, size);
    // EntityFilter filter = entityFilterDslParser.parse(q);
    // Page<Entity> entities =
    // entityService.getEntitiesByTemplateIdentifierIn(pageable,
    // templateIdentifier, filter);

    // Extract entity identifiers for batch graph loading
    // List<String> entityIdentifiers =
    // entities.getContent().stream().map(Entity::identifier)
    // .toList();

    // if (entityIdentifiers.isEmpty()) {
    // return new PageImpl<>(List.of(), pageable, 0);
    // }

    // // Parse relations filter from comma-separated list
    // java.util.Set<String>
    // relationFiltgetBatchEntityGraphsByTemplateIdentifierserSet =
    // java.util.Arrays.stream(relationsFilter.split(","))
    // .map(String::trim).filter(s ->
    // !s.isBlank()).collect(java.util.stream.Collectors.toSet());

    // Load entity graphs with DIRECT_LINEAGE mode (includes outbound + inbound
    // relations)
    Map<String, EntityGraphNode> entityGraphs = entityGraphService.getBatchEntityGraphsByTemplate(
        templateIdentifier, filterIds, depth, startTemplate, size, page);

    // Map to EntityDepDtoOut with merged relations
    List<EntityDepDtoOut> dtoOutList = entityGraphs.values().stream()
        .map(entityNode -> entityDepDtoOutMapper.toDto(entityNode)).toList();

    return new PageImpl<>(dtoOutList, pageable, entityGraphs.size());
  }

  /// Searches for entities across all templates using a nested filter query.
  ///
  /// **API contract:** Accepts a JSON body with a nested filter tree, pagination,
  /// and sorting parameters. Returns a paginated list of entities matching the
  /// filter. No template scoping is applied by default; include a template
  /// criterion in the filter to scope results to a specific template.
  ///
  /// @param searchRequest the search request body with filter, page, size, and
  /// sort
  /// @return paginated entity DTOs matching the filter
  @Operation(summary = ENDPOINT_POST_SEARCH_SUMMARY, description = ENDPOINT_POST_SEARCH_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_SEARCH_SUCCESS, content = @Content(schema = @Schema(implementation = EntityPageResponse.class)))
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_SEARCH_QUERY, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @PostMapping("/search")
  @ResponseStatus(OK)
  public Page<EntityDtoOut> searchEntities(@RequestBody EntitySearchRequestDtoIn searchRequest) {
    RawSearchFilterNode rawFilter = searchFilterMapper.toRaw(searchRequest.filter());
    SearchFilterNode filter = searchFilterParser.parse(rawFilter);
    PaginationCriteria paginationCriteria = new PaginationCriteria(searchRequest.page(),
        searchRequest.size(), searchRequest.sort());

    PaginatedResult<Entity> result = entityService.searchEntities(filter, searchRequest.query(),
        paginationCriteria);
    Page<Entity> page = toPageResponse(result.content(), paginationCriteria,
        result.totalElements());
    return entityDtoOutMapper.fromEntitiesSearchPageToDtoPage(page);
  }

  // @PostMapping("/search")
  // public ResponseEntity<EntityPageResponse> searchEntities(@RequestBody
  // EntitySearchDepthRequestDtoIn request) {

  // // 1. Establish the global execution strategy up front
  // int globalDepth = (request.depth() != null) ? request.depth() : 1;
  // List<String> globalWhitelist = request.allowedRelations() != null ?
  // request.allowedRelations() : List.of();

  // // 2. Pass these global parameters down into your criteria evaluation loop
  // Set<UUID> matchingRootIds = filterEngine.resolveAllCriteria(
  // request.filter(),
  // globalDepth,
  // globalWhitelist);

  // // 3. Complete the execution with the standard pagination and hydration pass
  // return ResponseEntity
  // .ok(hydrationService.buildPage(matchingRootIds, request.page(),
  // request.size(), globalDepth));
  // }

  @Operation(summary = "Search entities by lineage constraints", description = "Execute a paginated cross-template intersection search via graph lineage pipelines")
  @ApiResponse(responseCode = "200", description = "Lineage search executed successfully", content = @Content(schema = @Schema(implementation = EntityPageResponse.class)))
  @ApiResponse(responseCode = "400", description = "Invalid criteria filter tree arguments payload", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping("/{templateIdentifier}/search")
  @ResponseStatus(HttpStatus.OK)
  public Page<EntityDepDtoOut> searchEntitiesByLineage(@PathVariable String templateIdentifier,
      @Valid @RequestBody EntitySearchDepthRequestDtoIn searchRequest) {

    // Combine results from all filters into parallel arrays for cross-template
    // intersection
    List<UUID> combinedRoots = new ArrayList<>();
    List<String> rootGroupMapping = new ArrayList<>(); // Track which group each root belongs to

    // Iterate through each filter in the request
    for (int groupIndex = 0; groupIndex < searchRequest.filters().size(); groupIndex++) {
      FilterNodeDtoIn filterDto = searchRequest.filters().get(groupIndex);
      String groupId = "GRP_" + (groupIndex + 1); // GRP_1, GRP_2, etc.

      // Parse and evaluate each filter
      RawSearchFilterNode rawFilter = searchFilterMapper.toRaw(filterDto);
      SearchFilterNode filter = searchFilterParser.parse(rawFilter);

      // Search for entities matching this filter with pagination
      PaginationCriteria paginationCriteria = new PaginationCriteria(0, searchRequest.size(), null);
      PaginatedResult<Entity> result = entityService.searchEntities(filter, "", paginationCriteria);

      // Collect all matching entity IDs and their group identifier
      // All entities in a group share the same GRP_N identifier (Logical OR within
      // this axis)
      for (Entity entity : result.content()) {
        UUID entityId = entity.id();
        combinedRoots.add(entityId);
        rootGroupMapping.add(groupId);
      }
    }

    UUID[] rootArray = combinedRoots.toArray(new UUID[0]);
    String[] groupArray = rootGroupMapping.toArray(new String[0]);

    // Load full Entity objects for the paginated results
    Map<String, EntityGraphNode> entityGraphs = entityGraphService
        .getBatchEntityGraphsByAgnosticFilter(rootArray, groupArray, searchRequest.filters().size(),
            searchRequest.depth(), templateIdentifier, searchRequest.size(), searchRequest.page());

    List<EntityDepDtoOut> dtoOutList = entityGraphs.values().stream()
        .map(entityNode -> entityDepDtoOutMapper.toDto(entityNode)).toList();


    Pageable pageable = PageRequest.of(searchRequest.page(), searchRequest.size());

    return new PageImpl<>(dtoOutList, pageable, entityGraphs.size());
  }

  private <T> Page<T> toPageResponse(List<T> content, PaginationCriteria criteria,
      long totalElements) {
    Pageable pageable = PageRequest.of(criteria.page(), criteria.size());
    return new PageImpl<>(content, pageable, totalElements);
  }
}
