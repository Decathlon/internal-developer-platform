package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntitySummaryDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// Adapter mapper for converting a recursive [EntityGraphNode] domain tree into the flat,
/// unified [EntityDtoOut] representation expected by API clients.
///
/// **Design:**
/// - Traverses both `relations` (outbound) and `relationsAsTarget` (inbound) depth-first,
///   flattening the tree into a single relations map at the root level.
/// - Outbound edges are collected as `source → target` during outbound traversal.
/// - Inbound edges are collected as `source → root` during inbound traversal.
/// - Both traversals originate independently from the root entity at depth 0.
/// - A `Set` of emitted edge signatures (`source|target|label`) deduplicates edges that
///   would otherwise be emitted twice when both sides of a relation are traversed.
/// - Depth tracking matrices (`visitedOutboundDepths`, `visitedInboundDepths`) prevent
///   infinite loops and redundant re-traversal in cyclic graphs by recording the minimum
///   depth at which each node was first reached during each directional pass.
/// - Directional isolation is maintained: outbound traversal strictly ignores `relationsAsTarget`,
///   and inbound traversal strictly ignores `relations`, preventing cross-branch entity leakage
///   at shared convergence nodes.
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityDtoOutFromEntityNodeMapper {

  private final EntityTemplateService entityTemplateService;

  /// Resolves an [EntityGraphNode] tree and converts it into a flat
  /// [EntityDtoOut] representation.
  ///
  /// @param root the starting root node of the entity graph
  /// @param entityTemplateIdentifier the template key used for property type
  /// definitions
  /// @param maxDepth the strict maximum depth layer allowed for relationship
  /// collection
  /// @return a fully mapped, flat [EntityDtoOut] object
  public EntityDtoOut toDto(EntityGraphNode root, String entityTemplateIdentifier, int maxDepth) {
    if (root == null) {
      return null;
    }

    EntityTemplate entityTemplate = entityTemplateService
        .getEntityTemplateByIdentifier(entityTemplateIdentifier);

    return toDto(root, entityTemplate, maxDepth);
  }

  /// Internal orchestrator handling property conversions and initiating
  /// directional graph traversals.
  private EntityDtoOut toDto(EntityGraphNode root, EntityTemplate entityTemplate, int maxDepth) {
    if (root == null) {
      return null;
    }

    Map<String, Object> properties = mapPropertiesFromGraphNode(root, entityTemplate);

    var state = new TraversalState(root.identifier(), new HashMap<>(), new HashMap<>(),
        new HashMap<>(), new HashSet<>());

    // Initiate directional traversals independently starting from root at depth 0
    traverseOutbound(root, 0, maxDepth, state);
    traverseInbound(root, 0, maxDepth, state);

    Map<String, List<EntitySummaryDto>> finalizedRelations = state.relationsMap().entrySet()
        .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));

    return new EntityDtoOut(root.identifier(), root.name(), root.templateIdentifier(), properties,
        finalizedRelations);
  }

  /// Overloaded fallback extracting the primary root node from a paginated
  /// container.
  public EntityDtoOut toDto(Page<EntityGraphNode> page, EntityTemplate entityTemplate,
      int maxDepth) {
    if (page == null || page.getContent().isEmpty()) {
      return null;
    }

    return toDto(page.getContent().get(0), entityTemplate, maxDepth);
  }

  /// Converts a paginated series of domain [EntityGraphNode] trees into a page of
  /// flattened
  /// [EntityDtoOut] objects.
  ///
  /// @param graphNodesPage paginated graph nodes returned by the domain service
  /// @param entityTemplateIdentifier the template identifier for property type
  /// definitions
  /// @param maxDepth the maximum depth threshold allowed for flattening relations
  /// @return paginated DTOs with flattened unified relations maps
  public Page<EntityDtoOut> toPageDto(Page<EntityGraphNode> graphNodesPage,
      String entityTemplateIdentifier, int maxDepth) {
    if (graphNodesPage == null) {
      return Page.empty();
    }

    EntityTemplate entityTemplate = entityTemplateService
        .getEntityTemplateByIdentifier(entityTemplateIdentifier);

    return graphNodesPage.map(node -> this.toDto(node, entityTemplate, maxDepth));
  }

  /// Recursively walks downstream outbound relations (`source → target`).
  ///
  /// **Directional Isolation:** Strictly ignores `relationsAsTarget()` to prevent
  /// upstream or sibling
  /// entity leakage when visiting shared convergence nodes.
  ///
  /// @param node current graph node being evaluated
  /// @param currentDepth active depth level in the execution stack
  /// @param maxDepth maximum allowable depth boundary
  /// @param state shared accumulator state tracking visited depths and emitted
  /// edge signatures
  private static void traverseOutbound(EntityGraphNode node, int currentDepth, int maxDepth,
      TraversalState state) {
    String nodeId = nodeId(node.templateIdentifier(), node.identifier());

    Integer minDepth = state.visitedOutboundDepths().get(nodeId);
    if (minDepth != null && currentDepth >= minDepth) {
      return;
    }
    state.visitedOutboundDepths().put(nodeId, currentDepth);

    boolean shouldCollectRelations = currentDepth < maxDepth;

    for (EntityGraphRelation relation : node.relations()) {
      for (EntityGraphNode target : relation.targets()) {
        String targetId = nodeId(target.templateIdentifier(), target.identifier());
        String signature = nodeId + "|" + targetId + "|" + relation.name();

        boolean isNotRoot = !target.identifier().equalsIgnoreCase(state.rootIdentifier());

        if (shouldCollectRelations && isNotRoot && state.emittedEdgeSignatures().add(signature)) {
          state.relationsMap().computeIfAbsent(relation.name(), k -> new LinkedHashSet<>())
              .add(new EntitySummaryDto(target.identifier(), target.name(),
                  target.templateIdentifier()));
        }

        if (currentDepth < maxDepth) {
          traverseOutbound(target, currentDepth + 1, maxDepth, state);
        }
      }
    }
  }

  /// Recursively walks upstream inbound relations (`target ← source`).
  ///
  /// **Directional Isolation:** Strictly ignores direct `relations()` to prevent
  /// downstream entity
  /// leakage when visiting shared convergence nodes.
  ///
  /// @param node current graph node being evaluated
  /// @param currentDepth active depth level in the execution stack
  /// @param maxDepth maximum allowable depth boundary
  /// @param state shared accumulator state tracking visited depths and emitted
  /// edge signatures
  private static void traverseInbound(EntityGraphNode node, int currentDepth, int maxDepth,
      TraversalState state) {
    String nodeId = nodeId(node.templateIdentifier(), node.identifier());

    Integer minDepth = state.visitedInboundDepths().get(nodeId);
    if (minDepth != null && currentDepth >= minDepth) {
      return;
    }
    state.visitedInboundDepths().put(nodeId, currentDepth);

    boolean shouldCollectRelations = currentDepth < maxDepth;

    for (EntityGraphRelation relation : node.relationsAsTarget()) {
      for (EntityGraphNode source : relation.targets()) {
        String sourceId = nodeId(source.templateIdentifier(), source.identifier());
        String signature = sourceId + "|" + nodeId + "|" + relation.name();

        boolean isNotRoot = !source.identifier().equalsIgnoreCase(state.rootIdentifier());

        if (shouldCollectRelations && isNotRoot && state.emittedEdgeSignatures().add(signature)) {
          state.relationsMap().computeIfAbsent(relation.name(), k -> new LinkedHashSet<>())
              .add(new EntitySummaryDto(source.identifier(), source.name(),
                  source.templateIdentifier()));
        }

        if (currentDepth < maxDepth) {
          traverseInbound(source, currentDepth + 1, maxDepth, state);
        }
      }
    }
  }

  /// Builds the unique node identifier string format
  /// (`templateIdentifier:identifier`).
  ///
  /// This composite ID is used for deduplication and lookups within traversal
  /// accumulators.
  private static String nodeId(String templateIdentifier, String identifier) {
    return templateIdentifier + ":" + identifier;
  }

  /// Maps properties from a graph node using the provided template definition for
  /// type conversion.
  ///
  /// Uses explicit filtering and null-safety checks to handle missing property
  /// definitions
  /// and maintain consistent property representation across the API.
  ///
  /// @param graphNode the node containing properties
  /// @param template entity template with property definitions
  /// @return map of property names to converted values
  private Map<String, Object> mapPropertiesFromGraphNode(EntityGraphNode graphNode,
      EntityTemplate template) {
    if (graphNode.properties() == null || graphNode.properties().isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, PropertyDefinition> definitions = template.propertiesDefinitions().stream()
        .filter(Objects::nonNull).collect(Collectors.toMap(def -> def.name(), Function.identity()));

    return graphNode.properties().stream().filter(prop -> prop.value() != null)
        .collect(Collectors.toMap(Property::name,
            prop -> PropertyValueConverter.convert(prop, definitions.get(prop.name()))));
  }

  /// Groups mutable traversal accumulators to keep the traversal method
  /// signatures readable
  /// and manage state efficiently across directional passes.
  private record TraversalState(String rootIdentifier,
      Map<String, Set<EntitySummaryDto>> relationsMap, Map<String, Integer> visitedOutboundDepths,
      Map<String, Integer> visitedInboundDepths, Set<String> emittedEdgeSignatures) {
  }
}
