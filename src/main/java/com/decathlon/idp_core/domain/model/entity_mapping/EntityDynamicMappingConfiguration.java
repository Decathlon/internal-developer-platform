package com.decathlon.idp_core.domain.model.entity_mapping;

public record EntityDynamicMappingConfiguration(
    String template,
    String filter,
    String functions,
    EntityDynamicMapping entityMapping
) {}

