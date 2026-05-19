package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityGraphNodeDtoOut;

/// Mapper for converting domain [EntityGraphNode] to its API output DTO
/// representation.
///
/// Uses Record Patterns for recursive tree mapping since MapStruct does not
/// handle recursive structures cleanly.
public final class EntityGraphDtoOutMapper {

    private EntityGraphDtoOutMapper() {
        // Utility class
    }

    /// Maps a domain graph node to its DTO representation.
    ///
    /// @param node the domain graph node
    /// @return the output DTO
    public static EntityGraphNodeDtoOut toDto(EntityGraphNode node) {
        if (node == null) {
            return null;
        }
        return new EntityGraphNodeDtoOut(
                node.identifier(), node.name(),
                mapRelations(node.relations()),
                mapRelations(node.relationsAsTarget()));
    }

    private static Map<String, List<EntityGraphNodeDtoOut>> mapRelations(List<EntityGraphRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return Map.of();
        }
        return relations.stream()
                .collect(Collectors.toMap(
                        EntityGraphRelation::name,
                        relation -> relation.targets().stream()
                                .map(EntityGraphDtoOutMapper::toDto)
                                .toList(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }
}
