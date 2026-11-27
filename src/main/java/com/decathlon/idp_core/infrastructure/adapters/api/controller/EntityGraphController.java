package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.BAD_REQUEST_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_GRAPH_FLAT_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_GRAPH_FLAT_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_RELATED_BY_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_RELATED_BY_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.NOT_FOUND_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.OK_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_DEPTH_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_INCLUDE_DATA_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_PAGE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_PROPERTIES_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_RELATED_BY_DEPTH_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_RELATED_BY_RELATION_NAME_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_RELATIONS_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_SIZE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_SORT_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_TRAVERSAL_MODE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_GRAPH_FLAT_SUCCESS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_OR_RELATION_NOT_FOUND;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_RELATED_BY_SUCCESS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_GRAPH_QUERY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INVALID_PAGINATION;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decathlon.idp_core.domain.constant.SearchConstraints;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphQueryOptions;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.model.search.PaginatedResult;
import com.decathlon.idp_core.domain.model.search.PaginationCriteria;
import com.decathlon.idp_core.domain.service.entity_graph.EntityGraphService;
import com.decathlon.idp_core.infrastructure.adapters.api.auth.AuthorizedResource;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SearchProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerConfiguration.EntitySummaryPageResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntitySummaryDto;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_graph.EntityGraphFlatDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_graph.EntityGraphFlatDtoOutMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_graph.EntityRelatedSummaryDtoOutMapper;
import com.decathlon.idp_core.infrastructure.adapters.common.model.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/// REST controller for entity relationship graph operations.
///
/// Provides endpoints to retrieve flat (nodes and edges) relationship graphs
/// starting from a specified entity, suitable for frontend visualization tools
/// such as React Flow, Vis.js, and Cytoscape.
@RestController
@AuthorizedResource("entity")
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
@Validated
@Tag(name = "Entity Graph", description = "Entity relationship graph operations")
public class EntityGraphController {

  private static final int DEPTH_MIN = EntityGraphService.MIN_DEPTH;
  private static final int DEPTH_MAX = EntityGraphService.MAX_DEPTH;
  private static final String DEPTH_MIN_MESSAGE = "depth must be at least " + DEPTH_MIN;
  private static final String DEPTH_MAX_MESSAGE = "depth cannot exceed " + DEPTH_MAX;

  private final EntityGraphService entityGraphService;
  private final SearchProperties searchProperties;

  /// Retrieves the entity relationship graph as a flat nodes-and-edges structure.
  ///
  /// Returns all entities as nodes and all directed relations as edges. Nodes are
  /// deduplicated; edges encode directionality. Suitable for React Flow, Vis.js,
  /// Cytoscape, and similar frontend graph visualization libraries.
  ///
  /// @param templateIdentifier the template identifier of the root entity
  /// @param entityIdentifier the business identifier of the root entity
  /// @param depth the maximum traversal depth (default 1, clamped between 1 and
  /// 6 by the service)
  /// @param includeData when true, each node includes a data object with entity
  /// property values
  /// @param relations when provided, only relations with matching names are
  /// included
  /// @param properties when provided, each node's data object is restricted to
  /// the listed property names
  /// @return flat DTO containing nodes and edges arrays
  @GetMapping("/{templateIdentifier}/{entityIdentifier}/graph")
  @ResponseStatus(OK)
  @Operation(summary = ENDPOINT_GET_ENTITY_GRAPH_FLAT_SUMMARY, description = ENDPOINT_GET_ENTITY_GRAPH_FLAT_DESCRIPTION, responses = {
      @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_GRAPH_FLAT_SUCCESS, content = @Content(schema = @Schema(implementation = EntityGraphFlatDtoOut.class))),
      @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_GRAPH_QUERY, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  public EntityGraphFlatDtoOut getEntityGraph(@PathVariable @NotBlank String templateIdentifier,
      @PathVariable @NotBlank String entityIdentifier,
      @Parameter(description = PARAM_DEPTH_DESCRIPTION) @RequestParam(defaultValue = "1") int depth,
      @Parameter(description = PARAM_INCLUDE_DATA_DESCRIPTION) @RequestParam(name = "include_data", defaultValue = "false") boolean includeData,
      @Parameter(description = PARAM_TRAVERSAL_MODE_DESCRIPTION) @RequestParam(name = "traversal_mode", defaultValue = "DIRECT_LINEAGE") EntityGraphTraversalMode mode,
      @Parameter(description = PARAM_RELATIONS_DESCRIPTION) @RequestParam(required = false) List<String> relations,
      @Parameter(description = PARAM_PROPERTIES_DESCRIPTION) @RequestParam(required = false) List<String> properties) {

    // Convert the nullable lists to Sets for O(1) lookup; empty set means no filter
    Set<String> relationFilter = relations != null ? Set.copyOf(relations) : Set.of();
    Set<String> propertyFilter = properties != null ? Set.copyOf(properties) : Set.of();

    EntityGraphNode graphNode = entityGraphService.getEntityGraph(templateIdentifier,
        entityIdentifier, new EntityGraphQueryOptions(depth, includeData, includeData,
            relationFilter, propertyFilter, mode));
    return EntityGraphFlatDtoOutMapper.toFlatDto(graphNode);
  }

  /// Retrieves a paginated list of entity summaries reached via a terminal
  /// relation matching `relationName` within `depth` hops using `DIRECT_LINEAGE`
  /// traversal.
  ///
  /// @param templateIdentifier template identifier of the root entity
  /// @param entityIdentifier business identifier of the root entity
  /// @param relationName terminal relation name filtering returned entities
  /// @param depth maximum traversal depth (`1..6`, default `1`)
  /// @param page zero-based page index (default `0`)
  /// @param size page size (defaults to `20`, capped by configured max page size)
  /// @param sort sort expression in `field[,asc|desc]` format (default
  /// `identifier,asc`)
  /// @return paginated [EntitySummaryDto] items reached by `relationName`
  @GetMapping("/{templateIdentifier}/{entityIdentifier}/related-by/{relationName}")
  @ResponseStatus(OK)
  @Operation(summary = ENDPOINT_GET_ENTITY_RELATED_BY_SUMMARY, description = ENDPOINT_GET_ENTITY_RELATED_BY_DESCRIPTION, responses = {
      @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_RELATED_BY_SUCCESS, content = @Content(schema = @Schema(implementation = EntitySummaryPageResponse.class))),
      @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_PAGINATION, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_OR_RELATION_NOT_FOUND, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
  @Parameter(name = "page", description = PARAM_PAGE_DESCRIPTION, in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0"))
  @Parameter(name = "size", description = PARAM_SIZE_DESCRIPTION, in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "20"))
  @Parameter(name = "sort", description = PARAM_SORT_DESCRIPTION, in = ParameterIn.QUERY, schema = @Schema(type = "string", defaultValue = "identifier,asc"))
  public Page<EntitySummaryDto> getEntitiesRelatedByRelation(
      @PathVariable @NotBlank String templateIdentifier,
      @PathVariable @NotBlank String entityIdentifier,
      @Parameter(description = PARAM_RELATED_BY_RELATION_NAME_DESCRIPTION) @PathVariable @NotBlank String relationName,
      @Parameter(description = PARAM_RELATED_BY_DEPTH_DESCRIPTION, schema = @Schema(type = "integer", defaultValue = "1", minimum = "1", maximum = "6")) @Min(value = DEPTH_MIN, message = DEPTH_MIN_MESSAGE) @Max(value = DEPTH_MAX, message = DEPTH_MAX_MESSAGE) @RequestParam(defaultValue = "1") int depth,
      @RequestParam(defaultValue = "0") int page, @RequestParam(required = false) Integer size,
      @RequestParam(defaultValue = "identifier,asc") String sort) {
    PaginationCriteria paginationCriteria = new PaginationCriteria(page, resolvePageSize(size),
        sort);
    PaginatedResult<EntitySummary> result = entityGraphService.getEntitiesRelatedByRelation(
        templateIdentifier, entityIdentifier, relationName, depth, paginationCriteria,
        searchProperties.maxPageSize());
    return EntityRelatedSummaryDtoOutMapper.toSummaryPage(result, paginationCriteria);
  }

  private int resolvePageSize(Integer requestedSize) {
    return requestedSize != null
        ? requestedSize
        : Math.min(SearchConstraints.DEFAULT_PAGE_SIZE, searchProperties.maxPageSize());
  }

}
