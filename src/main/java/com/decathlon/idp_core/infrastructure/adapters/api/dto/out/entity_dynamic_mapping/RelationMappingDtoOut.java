package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/// Relation mapping entry exposed in API responses.
///
/// Mirrors [com.decathlon.idp_core.infrastructure.adapters.api.dto.in.RelationMappingDtoIn]:
/// the `target_entity_identifiers` field holds the list of JSLT expressions used to extract
/// the target entity identifiers from the event payload at runtime.
public record RelationMappingDtoOut(String name,
    @JsonProperty("target_entity_identifiers") List<String> targetEntityIdentifiers) {
}
