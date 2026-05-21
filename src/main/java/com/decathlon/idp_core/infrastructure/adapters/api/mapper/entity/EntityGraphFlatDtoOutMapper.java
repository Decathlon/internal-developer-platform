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

    /// Maps a domain graph node tree to a flat [EntityGraphFlatDtoOut].
    ///
    /// @param root           the root [EntityGraphNode] returned by the domain service
    /// @param relationFilter when non-empty, only edges whose type is in this set are emitted,
    ///                       and nodes not referenced by any remaining edge are pruned from the
    ///                       result (except the root, which is always included);
    ///                       an empty set means no filter — all edge types and nodes are emitted
    /// @return flat DTO with deduplicated nodes and directed edges
    public static EntityGraphFlatDtoOut toFlatDto(EntityGraphNode root, Set<String> relationFilter) {
        if (root == null) {
            return new EntityGraphFlatDtoOut(List.of(), List.of());
        }

        // Use a SequencedSet to deduplicate nodes while preserving insertion order
        SequencedSet<EntityGraphNodeFlatDtoOut> nodes = new LinkedHashSet<>();
        List<EntityGraphEdgeDtoOut> edges = new ArrayList<>();
        // Tracks visited node IDs to prevent infinite loops in cyclic graphs
        Set<String> visitedNodeIds = new HashSet<>();
        // Tracks emitted edge signatures (source|target|label) to avoid duplicate edges
        // when the same relation is encountered from both sides during traversal
        Set<String> emittedEdgeSignatures = new HashSet<>();
        var edgeCounter = new AtomicInteger(0);

        traverse(root, nodes, edges, visitedNodeIds, emittedEdgeSignatures, edgeCounter, relationFilter);

        // When a relation filter is active, prune nodes that are not connected to any
        // remaining edge. The root is always kept. Without this step, nodes reachable via
        // non-filtered edges (e.g. C via "depends-on" when filtering "monitors") would
        // appear in the node list despite having no visible edges.
        List<EntityGraphNodeFlatDtoOut> finalNodes;
        if (relationFilter.isEmpty()) {
            finalNodes = List.copyOf(nodes);
        } else {
            // Collect all node IDs referenced by the filtered edges only.
            // The root receives no special treatment: if it has no matching edges
            // it is pruned just like any other disconnected node.
            Set<String> referencedNodeIds = new HashSet<>();
            for (var edge : edges) {
                referencedNodeIds.add(edge.source());
                referencedNodeIds.add(edge.target());
            }
            finalNodes = nodes.stream()
                    .filter(n -> referencedNodeIds.contains(n.id()))
                    .toList();
        }

        return new EntityGraphFlatDtoOut(finalNodes, List.copyOf(edges));
    }

    private static void traverse(
            EntityGraphNode node,
            SequencedSet<EntityGraphNodeFlatDtoOut> nodes,
            List<EntityGraphEdgeDtoOut> edges,
            Set<String> visitedNodeIds,
            Set<String> emittedEdgeSignatures,
            AtomicInteger edgeCounter,
            Set<String> relationFilter) {

        var nodeId = nodeId(node.templateIdentifier(), node.identifier());

        // Skip this node if already visited to prevent infinite loops in cyclic graphs
        if (!visitedNodeIds.add(nodeId)) {
            return;
        }

        nodes.add(new EntityGraphNodeFlatDtoOut(
                nodeId, node.name(), node.templateIdentifier(), node.identifier(),
                toDataMap(node)));

        // Traverse outbound relations: emit edge from currentNode → target only when the
        // relation type matches the filter (or no filter is active). Nodes are always
        // traversed so that deeper nodes remain reachable regardless of edge visibility.
        for (EntityGraphRelation relation : node.relations()) {
            for (EntityGraphNode target : relation.targets()) {
                var targetId = nodeId(target.templateIdentifier(), target.identifier());
                if (relationFilter.isEmpty() || relationFilter.contains(relation.name())) {
                    addEdge(edges, emittedEdgeSignatures, edgeCounter, nodeId, targetId, relation.name());
                }
                traverse(target, nodes, edges, visitedNodeIds, emittedEdgeSignatures, edgeCounter, relationFilter);
            }
        }

        // Traverse inbound relations: emit edge from source → currentNode.
        // This is essential when the root entity has no outbound relations and is only
        // reachable as a target. Without this, traversal would stop at the root with no edges.
        for (EntityGraphRelation relation : node.relationsAsTarget()) {
            for (EntityGraphNode source : relation.targets()) {
                var sourceId = nodeId(source.templateIdentifier(), source.identifier());
                if (relationFilter.isEmpty() || relationFilter.contains(relation.name())) {
                    addEdge(edges, emittedEdgeSignatures, edgeCounter, sourceId, nodeId, relation.name());
                }
                traverse(source, nodes, edges, visitedNodeIds, emittedEdgeSignatures, edgeCounter, relationFilter);
            }
        }
    }

    /// Adds a directed edge only if it has not been emitted before, preventing duplicates
    /// that arise when the same relation is encountered from both the source and the target
    /// during depth-first traversal.
    private static void addEdge(
            List<EntityGraphEdgeDtoOut> edges,
            Set<String> emittedEdgeSignatures,
            AtomicInteger edgeCounter,
            String sourceId,
            String targetId,
            String label) {

        var signature = sourceId + "|" + targetId + "|" + label;
        if (emittedEdgeSignatures.add(signature)) {
            edges.add(new EntityGraphEdgeDtoOut(
                    "e" + edgeCounter.incrementAndGet(), sourceId, targetId, label));
        }
    }

    /// Builds the unique node identifier from the entity's composite key.
    /// Format: "templateIdentifier:identifier" — mirrors EntityCompositeKey.toString().
    private static String nodeId(String templateIdentifier, String identifier) {
        return templateIdentifier + ":" + identifier;
    }

    /// Converts a node's property list to a name→value map for the `data` field.
    /// Returns an empty map when there are no properties; the DTO's @JsonInclude(NON_EMPTY)
    /// annotation ensures an empty map is omitted from the JSON output.
    private static Map<String, Object> toDataMap(EntityGraphNode node) {
        return node.properties().stream()
                .collect(Collectors.toMap(p -> p.name(), p -> p.value()));
    }
}
