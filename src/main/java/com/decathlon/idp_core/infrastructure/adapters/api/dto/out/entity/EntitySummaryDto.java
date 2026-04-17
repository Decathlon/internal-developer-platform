package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class EntitySummaryDto {
    private String identifier;
    private String name;
}
