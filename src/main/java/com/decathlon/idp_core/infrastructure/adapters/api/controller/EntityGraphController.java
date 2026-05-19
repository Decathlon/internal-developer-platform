package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_GRAPH_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_GRAPH_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.NOT_FOUND_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.OK_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.PARAM_DEPTH_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_GRAPH_SUCCESS;
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
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityGraphNodeDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler.ErrorResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityGraphDtoOutMapper;

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
/// Provides endpoints to retrieve hierarchical relationship graphs starting from
/// a specified entity, enabling visualization of entity dependencies and connections.
@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
@Tag(name = "Entity Graph", description = "Entity relationship graph operations")
public class EntityGraphController {

    private final EntityGraphService entityGraphService;

    /// Retrieves the entity relationship graph starting from the specified entity.
    ///
    /// Resolves outbound relations recursively up to the requested depth,
    /// returning a tree structure with entity summary information at each node.
    ///
    /// @param templateIdentifier the template identifier of the root entity
    /// @param entityIdentifier the business identifier of the root entity
    /// @param depth the maximum traversal depth (default 1, clamped between 1 and 10)
    /// @return the root graph node with resolved relations
    @GetMapping("/{templateIdentifier}/{entityIdentifier}/graph")
    @ResponseStatus(OK)
    @Operation(
            summary = ENDPOINT_GET_ENTITY_GRAPH_SUMMARY,
            description = ENDPOINT_GET_ENTITY_GRAPH_DESCRIPTION,
            responses = {
                    @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_GRAPH_SUCCESS,
                            content = @Content(schema = @Schema(implementation = EntityGraphNodeDtoOut.class))),
                    @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER,
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public EntityGraphNodeDtoOut getEntityGraph(
            @PathVariable @NotBlank String templateIdentifier,
            @PathVariable @NotBlank String entityIdentifier,
            @Parameter(description = PARAM_DEPTH_DESCRIPTION)
            @RequestParam(defaultValue = "1") int depth) {

        EntityGraphNode graphNode = entityGraphService.getEntityGraph(
                templateIdentifier, entityIdentifier, depth);

        return EntityGraphDtoOutMapper.toDto(graphNode);
    }
}
