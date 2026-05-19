package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_NODE_ID_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_NODE_IDENTIFIER_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_NODE_LABEL_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_FLAT_NODE_TEMPLATE_DESCRIPTION;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

/// Output DTO representing a single node in the flat entity graph.
///
/// Used by frontend visualization tools (React Flow, Vis.js, Cytoscape) that expect
/// entities and their relationships as separate, non-nested collections.
@JsonNaming(SnakeCaseStrategy.class)
public record EntityGraphNodeFlatDtoOut(

        @Schema(description = ENTITY_GRAPH_FLAT_NODE_ID_DESCRIPTION)
        String id,

        @Schema(description = ENTITY_GRAPH_FLAT_NODE_LABEL_DESCRIPTION)
        String label,

        @Schema(description = ENTITY_GRAPH_FLAT_NODE_TEMPLATE_DESCRIPTION)
        String templateIdentifier,

        @Schema(description = ENTITY_GRAPH_FLAT_NODE_IDENTIFIER_DESCRIPTION)
        String identifier
) {}
