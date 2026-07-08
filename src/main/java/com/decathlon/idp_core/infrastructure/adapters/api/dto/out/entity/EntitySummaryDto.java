package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Represents a summary of an entity for use in collections and relations.
 *
 * Immutable record containing essential entity information: identifier, name,
 * and template identifier for proper entity classification. Used in both entity
 * listings and unified relation structures.
 */
@JsonNaming(SnakeCaseStrategy.class)
public record EntitySummaryDto(String identifier,

    String name,

    String templateIdentifier) {
}
