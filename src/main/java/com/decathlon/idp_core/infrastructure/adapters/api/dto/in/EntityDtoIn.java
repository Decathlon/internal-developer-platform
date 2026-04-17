package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityDtoIn {
    private String name;
    private String identifier;
    private Map<String, Object> properties;
    private List<RelationDtoIn> relations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor

    @Builder
    @JsonNaming(SnakeCaseStrategy.class)
    public static class RelationDtoIn {
        private String name;
        private List<String> targetEntityIdentifiers;
    }
}
