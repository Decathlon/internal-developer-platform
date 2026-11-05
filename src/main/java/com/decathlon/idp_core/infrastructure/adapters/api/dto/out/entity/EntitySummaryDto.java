package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_RELATION_ENTITY_PROPERTIES;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

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

    String templateIdentifier,

    @JsonInclude(Include.NON_NULL) @Schema(description = FIELD_RELATION_ENTITY_PROPERTIES) Map<String, Object> properties) {

  public EntitySummaryDto(String identifier, String name, String templateIdentifier) {
    this(identifier, name, templateIdentifier, null);
  }

  public EntitySummaryDto {
    properties = properties == null ? null : Map.copyOf(properties);
  }
}
