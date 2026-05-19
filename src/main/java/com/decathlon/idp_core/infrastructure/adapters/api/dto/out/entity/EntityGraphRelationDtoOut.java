package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_RELATION_NAME_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_RELATION_TARGET_TEMPLATE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENTITY_GRAPH_RELATION_TARGETS_DESCRIPTION;

/// Output DTO representing a single named relation in the entity graph.
@JsonNaming(SnakeCaseStrategy.class)
public record EntityGraphRelationDtoOut(

        @Schema(description = ENTITY_GRAPH_RELATION_NAME_DESCRIPTION)
        String name,

        @Schema(description = ENTITY_GRAPH_RELATION_TARGET_TEMPLATE_DESCRIPTION)
        String targetTemplateIdentifier,

        @Schema(description = ENTITY_GRAPH_RELATION_TARGETS_DESCRIPTION)
        List<EntityGraphNodeDtoOut> targets
) {}
