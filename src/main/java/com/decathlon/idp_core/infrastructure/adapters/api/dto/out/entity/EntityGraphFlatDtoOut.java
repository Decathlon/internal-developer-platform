package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_EDGES_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_NODES_DESCRIPTION;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

/// Top-level response DTO for the flat entity graph representation.
///
/// Separates entities from their connections into two parallel collections,
/// following the de-facto standard expected by frontend visualization libraries
/// such as React Flow, Vis.js, and Cytoscape. This format avoids nesting and
/// any risk of infinite loops caused by circular relations.
@JsonNaming(SnakeCaseStrategy.class)
public record EntityGraphFlatDtoOut(

        @Schema(description = ENTITY_GRAPH_FLAT_NODES_DESCRIPTION)
        List<EntityGraphNodeFlatDtoOut> nodes,

        @Schema(description = ENTITY_GRAPH_FLAT_EDGES_DESCRIPTION)
        List<EntityGraphEdgeDtoOut> edges
) {
    /// Defensive copies prevent external mutation of the returned collections.
    public EntityGraphFlatDtoOut {
        nodes = nodes != null ? List.copyOf(nodes) : List.of();
        edges = edges != null ? List.copyOf(edges) : List.of();
    }
}
