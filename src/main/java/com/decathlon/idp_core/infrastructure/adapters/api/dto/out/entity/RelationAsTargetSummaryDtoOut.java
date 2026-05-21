package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/// Output DTO representing an incoming relationship where the entity is the target.
@JsonNaming(SnakeCaseStrategy.class)
public record RelationAsTargetSummaryDtoOut(
        String targetEntityIdentifier,
        String relationName,
        String sourceEntityIdentifier,
        String sourceEntityName
) {}
