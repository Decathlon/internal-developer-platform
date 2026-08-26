package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

/// Relation mapping entry dedicated to entity dynamic mapping input payloads.
///
/// This DTO intentionally accepts only the array form for
/// `target_entity_identifiers` to keep the contract explicit and predictable.
public record EntityDynamicMappingRelationDtoIn(@NotBlank String name,
    @NotEmpty @JsonProperty("target_entity_identifiers") List<@NotBlank String> targetEntityIdentifiers) {
}
