package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDepDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntitySummaryDto;

@Component
public final class EntityDepDtoOutMapper {

  private EntityDepDtoOutMapper() {
    // Utility class — not instantiable
  }

  private record TraversalState(String rootIdentifier, // Stored to easily identify the starting
                                                       // anchor
      Map<String, Set<EntitySummaryDto>> relationsMap, Set<String> visitedNodeIds,
      Set<String> emittedEdgeSignatures) {
  }

  public static EntityDepDtoOut toDto(EntityGraphNode root) {
    if (root == null) {
      return null;
    }

    EntityDepDtoOut dto = EntityDepDtoOut.builder().identifier(root.identifier()).name(root.name())
        .templateIdentifier(root.templateIdentifier()).build();
    dto.setIdentifier(root.identifier());
    dto.setName(root.name());
    dto.setTemplateIdentifier(root.templateIdentifier());

    dto.setProperties(root.properties().stream()
        .collect(Collectors.toMap(p -> p.name(), p -> p.value(), (v1, v2) -> v1)));

    var state = new TraversalState(root.identifier(), // Capture the root anchor ID
        new HashMap<>(), new HashSet<>(), new HashSet<>());

    traverse(root, state);

    // Convert Map<String, Set<EntitySummaryDto>> to Map<String,
    // List<EntitySummaryDto>>
    Map<String, List<EntitySummaryDto>> finalizedRelations = state.relationsMap().entrySet()
        .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));

    dto.setRelations(finalizedRelations);
    return dto;
  }

  private static void traverse(EntityGraphNode node, TraversalState state) {
    var nodeId = nodeId(node.templateIdentifier(), node.identifier());

    if (!state.visitedNodeIds().add(nodeId)) {
      return;
    }

    // 1. Outbound Relations: Current Node is ALWAYS Source, Target is ALWAYS Target
    for (EntityGraphRelation relation : node.relations()) {
      for (EntityGraphNode target : relation.targets()) {
        var targetId = nodeId(target.templateIdentifier(), target.identifier());
        var signature = nodeId + "|" + targetId + "|" + relation.name();

        if (state.emittedEdgeSignatures().add(signature)) {
          // Identify the dependency node based on relation geometry
          EntityGraphNode dependencyNode = identifyDependency(state.rootIdentifier(), node, target);

          state.relationsMap().computeIfAbsent(relation.name(), k -> new LinkedHashSet<>())
              .add(new EntitySummaryDto(dependencyNode.identifier(), dependencyNode.name()));
        }
        traverse(target, state);
      }
    }

    // 2. Inbound Relations: Source is ALWAYS Source, Current Node is ALWAYS Target
    for (EntityGraphRelation relation : node.relationsAsTarget()) {
      for (EntityGraphNode source : relation.targets()) {
        var sourceId = nodeId(source.templateIdentifier(), source.identifier());
        var signature = sourceId + "|" + nodeId + "|" + relation.name();

        if (state.emittedEdgeSignatures().add(signature)) {
          // Identify the dependency node based on relation geometry
          EntityGraphNode dependencyNode = identifyDependency(state.rootIdentifier(), source, node);

          state.relationsMap().computeIfAbsent(relation.name(), k -> new LinkedHashSet<>())
              .add(new EntitySummaryDto(dependencyNode.identifier(), dependencyNode.name()));
        }
        traverse(source, state);
      }
    }
  }

  private static EntityGraphNode identifyDependency(String rootIdentifier, EntityGraphNode source,
      EntityGraphNode target) {
    // If the root node is the source, we want to look at what it points to (the
    // target)
    if (source.identifier().equals(rootIdentifier)) {
      return target;
    }
    // If the root node is the target, we want to look at what points to it (the
    // source)
    if (target.identifier().equals(rootIdentifier)) {
      return source;
    }
    // Deep structural default: if we are out in the graph branches, map the target
    // entity
    return target;
  }

  private static String nodeId(String templateIdentifier, String identifier) {
    return templateIdentifier + ":" + identifier;
  }
}
