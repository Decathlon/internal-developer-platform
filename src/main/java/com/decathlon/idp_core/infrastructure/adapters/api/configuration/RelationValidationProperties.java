package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import com.decathlon.idp_core.domain.model.enums.RelationTargetValidationMode;

/// Relation target validation settings bound from `app.relations`.
@ConfigurationProperties(prefix = "app.relations")
public record RelationValidationProperties(
    @DefaultValue("STRICT") RelationTargetValidationMode targetValidationMode) {
}
