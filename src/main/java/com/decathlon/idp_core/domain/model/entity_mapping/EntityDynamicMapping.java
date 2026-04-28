package com.decathlon.idp_core.domain.model.entity_mapping;

import java.util.Map;

public record EntityDynamicMapping(
    String identifier,
    String templateIdentifier,
    String name,
    Map<String, String> properties,
    Map<String, String> relations
) {}