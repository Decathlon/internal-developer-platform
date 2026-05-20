package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_GRAPH_FLAT_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_GRAPH_FLAT_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.NOT_FOUND_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.OK_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_DEPTH_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_INCLUDE_DATA_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_GRAPH_FLAT_SUCCESS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER;
import static org.springframework.http.HttpStatus.OK;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.service.entity_graph.EntityGraphService;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityGraphFlatDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler.ErrorResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityGraphFlatDtoOutMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/// REST controller for entity relationship graph operations.
///
/// Provides endpoints to retrieve flat (nodes and edges) relationship graphs
/// starting from a specified entity, suitable for frontend visualization tools
/// such as React Flow, Vis.js, and Cytoscape.
@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
@Tag(name = "Entity Graph", description = "Entity relationship graph operations")
public class EntityGraphController {

    private final EntityGraphService entityGraphService;

    /// Retrieves the entity relationship graph as a flat nodes-and-edges structure.
    ///
    /// Returns all entities as nodes and all directed relations as edges. Nodes are
    /// deduplicated; edges encode directionality. Suitable for React Flow, Vis.js,
    /// Cytoscape, and similar frontend graph visualization libraries.
    ///
    /// @param templateIdentifier the template identifier of the root entity
    /// @param entityIdentifier the business identifier of the root entity
    /// @param depth the maximum traversal depth (default 1, clamped between 1 and 10)
    /// @param includeData when true, each node includes a data object with entity property values
    /// @return flat DTO containing nodes and edges arrays
    @GetMapping("/{templateIdentifier}/{entityIdentifier}/graph")
    @ResponseStatus(OK)
    @Operation(
            summary = ENDPOINT_GET_ENTITY_GRAPH_FLAT_SUMMARY,
            description = ENDPOINT_GET_ENTITY_GRAPH_FLAT_DESCRIPTION,
            responses = {
                    @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_GRAPH_FLAT_SUCCESS,
                            content = @Content(schema = @Schema(implementation = EntityGraphFlatDtoOut.class))),
                    @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public EntityGraphFlatDtoOut getEntityGraph(
            @PathVariable @NotBlank String templateIdentifier,
            @PathVariable @NotBlank String entityIdentifier,
            @Parameter(description = PARAM_DEPTH_DESCRIPTION)
            @RequestParam(defaultValue = "1") int depth,
            @Parameter(description = PARAM_INCLUDE_DATA_DESCRIPTION)
            @RequestParam(defaultValue = "false") boolean includeData) {

        EntityGraphNode graphNode = entityGraphService.getEntityGraph(
                templateIdentifier, entityIdentifier, depth, includeData);

        return EntityGraphFlatDtoOutMapper.toFlatDto(graphNode);
    }
}
