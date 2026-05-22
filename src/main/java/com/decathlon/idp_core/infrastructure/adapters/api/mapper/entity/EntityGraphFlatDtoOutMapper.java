package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

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
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityGraphEdgeDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityGraphFlatDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityGraphNodeFlatDtoOut;

/// Mapper for converting a recursive [EntityGraphNode] domain tree into the flat
/// nodes-and-edges representation expected by frontend visualization libraries
/// (React Flow, Vis.js, Cytoscape).
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
public final class EntityGraphFlatDtoOutMapper {

    private EntityGraphFlatDtoOutMapper() {
        // Utility class — not instantiable
    }

    /// Groups mutable traversal accumulators to stay within the method-parameter limit
    /// and keep the traversal signature readable.
    private record TraversalState(
            SequencedSet<EntityGraphNodeFlatDtoOut> nodes,
            List<EntityGraphEdgeDtoOut> edges,
            Set<String> visitedNodeIds,
            Set<String> emittedEdgeSignatures,
            AtomicInteger edgeCounter) {
    }

    /// Maps a domain graph node tree to a flat [EntityGraphFlatDtoOut].
    ///
    /// @param root             the root [EntityGraphNode] returned by the domain service
    /// @param relationFilter   when non-empty, only edges whose type is in this set are emitted,
    ///                         and nodes not referenced by any remaining edge are pruned;
    ///                         an empty set means no filter — all edge types and nodes are emitted
    /// @param propertyFilter   when non-empty, only properties whose name is in this set appear
    ///                         in each node's `data` field;
    ///                         an empty set means no filter — all properties are included
    /// @return flat DTO with deduplicated nodes and directed edges
    public static EntityGraphFlatDtoOut toFlatDto(EntityGraphNode root, Set<String> relationFilter,
            Set<String> propertyFilter) {
        if (root == null) {
            return new EntityGraphFlatDtoOut(List.of(), List.of());
        }

        var state = new TraversalState(
                new LinkedHashSet<>(),   // nodes — insertion-ordered, deduplicated
                new ArrayList<>(),       // edges
                new HashSet<>(),         // visitedNodeIds — prevents infinite loops in cyclic graphs
                new HashSet<>(),         // emittedEdgeSignatures — prevents duplicate edges
                new AtomicInteger(0));   // edgeCounter

        traverse(root, state, relationFilter, propertyFilter);

        // When a relation filter is active, prune nodes that are not connected to any
        // remaining edge. Without this step, nodes reachable via non-filtered edges would
        // appear in the node list despite having no visible edges.
        List<EntityGraphNodeFlatDtoOut> finalNodes;
        if (relationFilter.isEmpty()) {
            finalNodes = List.copyOf(state.nodes());
        } else {
            // Collect all node IDs referenced by the filtered edges only.
            // The root receives no special treatment: if it has no matching edges
            // it is pruned just like any other disconnected node.
            Set<String> referencedNodeIds = new HashSet<>();
            for (var edge : state.edges()) {
                referencedNodeIds.add(edge.source());
                referencedNodeIds.add(edge.target());
            }
            finalNodes = state.nodes().stream()
                    .filter(n -> referencedNodeIds.contains(n.id()))
                    .toList();
        }

        return new EntityGraphFlatDtoOut(finalNodes, List.copyOf(state.edges()));
    }

    private static void traverse(
            EntityGraphNode node,
            TraversalState state,
            Set<String> relationFilter,
            Set<String> propertyFilter) {

        var nodeId = nodeId(node.templateIdentifier(), node.identifier());

        // Skip this node if already visited to prevent infinite loops in cyclic graphs
        if (!state.visitedNodeIds().add(nodeId)) {
            return;
        }

        state.nodes().add(new EntityGraphNodeFlatDtoOut(
                nodeId, node.name(), node.templateIdentifier(), node.identifier(),
                toDataMap(node, propertyFilter)));

        // Traverse outbound relations: emit edge from currentNode → target only when the
        // relation type matches the filter (or no filter is active). Nodes are always
        // traversed so that deeper nodes remain reachable regardless of edge visibility.
        for (EntityGraphRelation relation : node.relations()) {
            for (EntityGraphNode target : relation.targets()) {
                var targetId = nodeId(target.templateIdentifier(), target.identifier());
                if (relationFilter.isEmpty() || relationFilter.contains(relation.name())) {
                    addEdge(state, nodeId, targetId, relation.name());
                }
                traverse(target, state, relationFilter, propertyFilter);
            }
        }

        // Traverse inbound relations: emit edge from source → currentNode.
        // This is essential when the root entity has no outbound relations and is only
        // reachable as a target. Without this, traversal would stop at the root with no edges.
        for (EntityGraphRelation relation : node.relationsAsTarget()) {
            for (EntityGraphNode source : relation.targets()) {
                var sourceId = nodeId(source.templateIdentifier(), source.identifier());
                if (relationFilter.isEmpty() || relationFilter.contains(relation.name())) {
                    addEdge(state, sourceId, nodeId, relation.name());
                }
                traverse(source, state, relationFilter, propertyFilter);
            }
        }
    }

    /// Adds a directed edge only if it has not been emitted before, preventing duplicates
    /// that arise when the same relation is encountered from both the source and the target
    /// during depth-first traversal.
    private static void addEdge(
            TraversalState state,
            String sourceId,
            String targetId,
            String label) {

        var signature = sourceId + "|" + targetId + "|" + label;
        if (state.emittedEdgeSignatures().add(signature)) {
            state.edges().add(new EntityGraphEdgeDtoOut(
                    "e" + state.edgeCounter().incrementAndGet(), sourceId, targetId, label));
        }
    }

    /// Builds the unique node identifier from the entity's composite key.
    /// Format: "templateIdentifier:identifier" — mirrors EntityCompositeKey.toString().
    private static String nodeId(String templateIdentifier, String identifier) {
        return templateIdentifier + ":" + identifier;
    }

    /// Converts a node's property list to a name→value map for the `data` field.
    ///
    /// When [propertyFilter] is non-empty, only entries whose name is contained in the
    /// filter are included. Returns an empty map when there are no matching properties;
    /// the DTO's @JsonInclude(NON_EMPTY) annotation ensures an empty map is omitted from
    /// the JSON output.
    ///
    /// @param node           the graph node whose properties are converted
    /// @param propertyFilter when non-empty, restricts which properties appear in the map;
    ///                       an empty set means all properties are included
    private static Map<String, Object> toDataMap(EntityGraphNode node, Set<String> propertyFilter) {
        var stream = node.properties().stream();
        if (!propertyFilter.isEmpty()) {
            stream = stream.filter(p -> propertyFilter.contains(p.name()));
        }
        return stream.collect(Collectors.toMap(p -> p.name(), p -> p.value()));
    }
}
