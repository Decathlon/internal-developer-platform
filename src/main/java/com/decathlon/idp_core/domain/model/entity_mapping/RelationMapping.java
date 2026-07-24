package com.decathlon.idp_core.domain.model.entity_mapping;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record RelationMapping(
    /// The relation name as defined on the entity template.
    @NotBlank String name,
    /// The JSLT expressions used to extract the target entity identifier(s).
    @NotEmpty List<@NotBlank String> expressions) {
}
