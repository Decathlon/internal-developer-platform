package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_RELATIONS_AS_TARGET_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_RELATIONS_DESCRIPTION;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

/// Output DTO representing a node in the entity relationship graph.
///
/// Contains summary information about the entity and its resolved outbound relations
/// grouped by relation name, and incoming relations where this entity is the target.
@JsonNaming(SnakeCaseStrategy.class)
public record EntityGraphNodeDtoOut(

        String identifier,
        String name,

        @Schema(description = ENTITY_GRAPH_RELATIONS_DESCRIPTION)
        Map<String, List<EntityGraphNodeDtoOut>> relations,

        @Schema(description = ENTITY_GRAPH_RELATIONS_AS_TARGET_DESCRIPTION)
        Map<String, List<EntityGraphNodeDtoOut>> relationsAsTarget
) {}
