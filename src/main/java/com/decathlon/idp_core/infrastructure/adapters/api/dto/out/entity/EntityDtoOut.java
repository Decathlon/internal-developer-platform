package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(SnakeCaseStrategy.class)
public class EntityDtoOut {

    private String templateIdentifier;
    private String name;
    private String identifier;
    private Map<String, Object> properties;
    private Map<String, List<EntitySummaryDto>> relations;
    private Map<String, List<EntitySummaryDto>> relationsAsTarget;

}
