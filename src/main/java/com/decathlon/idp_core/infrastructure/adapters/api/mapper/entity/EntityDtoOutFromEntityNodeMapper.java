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
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntitySummaryDto;

@Component
public class EntityDtoOutFromEntityNodeMapper {

  private record TraversalState(String rootIdentifier, // Stored to easily identify the starting
      // anchor
      Map<String, Set<EntitySummaryDto>> relationsMap, Set<String> visitedNodeIds,
      Set<String> emittedEdgeSignatures) {
  }

  /// Converts an [EntityGraphNode] to an [EntityDtoOut] with flattened relations.
  ///
  /// **Flattening strategy:** All relations from the entire graph tree (root +
  /// all descendants) are collected into a single flat `relations` map at the
  /// root
  /// level. No nested relation structures are created.
  ///
  /// **Cycle prevention:** Uses a visited set to avoid infinite recursion on
  /// circular references.
  ///
  /// @param root the root entity graph node to convert
  /// @return the DTO with flat unified relations map containing all graph
  /// relations
  public static EntityDtoOut toDto(EntityGraphNode root) {
    if (root == null) {
      return null;
    }

    // Build properties map
    Map<String, Object> properties = root.properties().stream()
        .collect(Collectors.toMap(p -> p.name(), p -> p.value(), (v1, v2) -> v1));

    // Traverse graph and collect all relations into a flat map
    var state = new TraversalState(root.identifier(), new HashMap<>(), new HashSet<>(),
        new HashSet<>());

    traverse(root, state);

    // Convert Map<String, Set<EntitySummaryDto>> to Map<String,
    // List<EntitySummaryDto>>
    Map<String, List<EntitySummaryDto>> finalizedRelations = state.relationsMap().entrySet()
        .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));

    // Construct immutable record
    return new EntityDtoOut(root.identifier(), root.name(), root.templateIdentifier(), properties,
        finalizedRelations);
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
              .add(new EntitySummaryDto(dependencyNode.identifier(), dependencyNode.name(),
                  dependencyNode.templateIdentifier()));
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
              .add(new EntitySummaryDto(dependencyNode.identifier(), dependencyNode.name(),
                  dependencyNode.templateIdentifier()));
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
