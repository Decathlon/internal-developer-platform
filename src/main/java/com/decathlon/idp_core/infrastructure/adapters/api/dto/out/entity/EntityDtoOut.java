package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Unified Entity Data Transfer Object for API responses.
 *
 * Combines outbound relations (where this entity is the source) and inbound
 * relations (where this entity is the target) into a single relations map,
 * keyed by relation name (e.g., "component-supported_by-support_group").
 *
 * This eliminates the need for separate outbound/inbound relation sections and
 * allows frontend to display distant relations without specifying direction.
 *
 * **Breaking change:** The `relations_as_target` field has been removed.
 * All relations (both directions) are now merged into the `relations` map.
 */
@JsonNaming(SnakeCaseStrategy.class)
public record EntityDtoOut(
    String identifier,

    String name,

    String templateIdentifier,

    Map<String, Object> properties,

    Map<String, List<EntitySummaryDto>> relations) {
}
