package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.domain.service.entity_graph.EntityGraphService;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_graph.EntityGraphEdgeDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_graph.EntityGraphFlatDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_graph.EntityGraphNodeFlatDtoOut;

/// Mapper for converting a recursive [EntityGraphNode] domain tree into the flat
/// nodes-and-edges representation expected by frontend visualization libraries
///
/// **Design:**
/// - Traverses both `relations` (outbound) and `relationsAsTarget` (inbound) depth-first,
///   deduplicating nodes by their composite node ID (templateIdentifier:identifier).
/// - Outbound edges are emitted as `source → target`.
/// - Inbound edges (relationsAsTarget) are emitted as `source → currentNode`, preserving
///   the original direction of the relation. This is critical when the root entity has no
///   outbound relations and is only reachable as a relation target.
/// - A `SequencedSet` of visited node IDs prevents infinite loops in cyclic graphs.
/// - A `Set` of edge signatures (`source|target|label`) deduplicates edges that would
///   otherwise be emitted twice when both sides of a relation are traversed.
/// - Filtering (relation names, property names) is a domain concern handled upstream by
///   [EntityGraphService]; this mapper only flattens the tree it receives.
public final class EntityGraphFlatDtoOutMapper {

  private EntityGraphFlatDtoOutMapper() {
    // Utility class — not instantiable
  }

  /// Groups mutable traversal accumulators to stay within the method-parameter
  /// limit
  /// and keep the traversal signature readable.
  private record TraversalState(SequencedSet<EntityGraphNodeFlatDtoOut> nodes,
      List<EntityGraphEdgeDtoOut> edges, Set<String> visitedNodeIds,
      Set<String> emittedEdgeSignatures, AtomicInteger edgeCounter) {
  }

  /// Maps a domain graph node tree to a flat [EntityGraphFlatDtoOut].
  ///
  /// The domain graph passed here is already filtered by the service layer;
  /// this method only performs structural flattening.
  ///
  /// @param root the root [EntityGraphNode] returned by the domain service
  /// @return flat DTO with deduplicated nodes and directed edges
  public static EntityGraphFlatDtoOut toFlatDto(EntityGraphNode root) {
    if (root == null) {
      return new EntityGraphFlatDtoOut(List.of(), List.of());
    }

    var state = new TraversalState(new LinkedHashSet<>(), // nodes — insertion-ordered, deduplicated
        new ArrayList<>(), // edges
        new HashSet<>(), // visitedNodeIds — prevents infinite loops in cyclic graphs
        new HashSet<>(), // emittedEdgeSignatures — prevents duplicate edges
        new AtomicInteger(0)); // edgeCounter

    traverse(root, state);

    return new EntityGraphFlatDtoOut(List.copyOf(state.nodes()), List.copyOf(state.edges()));
  }

  private static void traverse(EntityGraphNode node, TraversalState state) {

    var nodeId = nodeId(node.templateIdentifier(), node.identifier());

    // Skip this node if already visited to prevent infinite loops in cyclic graphs
    if (!state.visitedNodeIds().add(nodeId)) {
      return;
    }

    state.nodes().add(new EntityGraphNodeFlatDtoOut(nodeId, node.name(), node.templateIdentifier(),
        node.identifier(), toDataMap(node)));

    // Traverse outbound relations: emit edge from currentNode → target.
    for (EntityGraphRelation relation : node.relations()) {
      for (EntityGraphNode target : relation.targets()) {
        var targetId = nodeId(target.templateIdentifier(), target.identifier());
        addEdge(state, nodeId, targetId, relation.name());
        traverse(target, state);
      }
    }

    // Traverse inbound relations: emit edge from source → currentNode.
    // This is essential when the root entity has no outbound relations and is only
    // reachable as a target. Without this, traversal would stop at the root with no
    // edges.
    for (EntityGraphRelation relation : node.relationsAsTarget()) {
      for (EntityGraphNode source : relation.targets()) {
        var sourceId = nodeId(source.templateIdentifier(), source.identifier());
        addEdge(state, sourceId, nodeId, relation.name());
        traverse(source, state);
      }
    }
  }

  /// Adds a directed edge only if it has not been emitted before, preventing
  /// duplicates
  /// that arise when the same relation is encountered from both the source and
  /// the target
  /// during depth-first traversal.
  private static void addEdge(TraversalState state, String sourceId, String targetId,
      String label) {

    var signature = sourceId + "|" + targetId + "|" + label;
    if (state.emittedEdgeSignatures().add(signature)) {
      state.edges().add(new EntityGraphEdgeDtoOut("e" + state.edgeCounter().incrementAndGet(),
          sourceId, targetId, label));
    }
  }

  /// Builds the unique node identifier from the entity's composite key.
  /// Format: "templateIdentifier:identifier" — mirrors
  private static String nodeId(String templateIdentifier, String identifier) {
    return templateIdentifier + ":" + identifier;
  }

  /// Converts a node's property list to a name→value map for the `data` field.
  ///
  /// The domain service has already applied any property filter; this method
  /// simply converts whatever properties the node carries into the map format
  /// expected by the DTO.
  ///
  /// Returns an empty map when there are no properties; the DTO's
  /// @JsonInclude(NON_EMPTY) annotation ensures an empty map is omitted from the
  /// JSON output.
  private static Map<String, Object> toDataMap(EntityGraphNode node) {
    return node.properties().stream().collect(Collectors.toMap(p -> p.name(), p -> p.value()));
  }
}
