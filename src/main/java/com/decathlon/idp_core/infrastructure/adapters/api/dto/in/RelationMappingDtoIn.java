package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

/// Relation mapping entry for inbound webhook entity projections.
///
/// Represents one relation name bound to one or more JSLT expressions that will
/// be evaluated at runtime to extract the target entity identifiers from the event payload.
///
/// The field name `target_entity_identifiers` mirrors the structure of
/// [com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDtoInCommonFields.RelationDtoIn]
/// to make the mapping definition feel natural compared to a real entity creation.
public record RelationMappingDtoIn(@NotBlank String name,
    @NotEmpty @JsonProperty("target_entity_identifiers") List<@NotBlank String> targetEntityIdentifiers) {
}
