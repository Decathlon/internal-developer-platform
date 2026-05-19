package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_EDGE_ID_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_EDGE_SOURCE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_EDGE_TARGET_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_EDGE_TYPE_DESCRIPTION;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

/// Output DTO representing a directed relation edge in the flat entity graph.
///
/// Encodes a single directional connection between two entity nodes, identified
/// by their composite-key-derived node IDs.
@JsonNaming(SnakeCaseStrategy.class)
public record EntityGraphEdgeDtoOut(

        @Schema(description = ENTITY_GRAPH_FLAT_EDGE_ID_DESCRIPTION)
        String id,

        @Schema(description = ENTITY_GRAPH_FLAT_EDGE_SOURCE_DESCRIPTION)
        String source,

        @Schema(description = ENTITY_GRAPH_FLAT_EDGE_TARGET_DESCRIPTION)
        String target,

        @Schema(description = ENTITY_GRAPH_FLAT_EDGE_TYPE_DESCRIPTION)
        String type
) {}
