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

@Slf4j
@Component
@RequiredArgsConstructor
public class EntityDtoOutFromEntityNodeMapper {

  private final EntityTemplateService entityTemplateService;

  /// Resolves and maps an `EntityGraphNode` tree to its flat output presentation.
  ///
  /// @param root the starting core node of the relational tree
  /// hierarchy
  /// @param entityTemplateIdentifier the operational template key for type
  /// matching
  /// @param maxDepth the strict maximum boundary layer limit
  /// allowed for relationship collection
  /// @return a fully populated, flat [EntityDtoOut] transfer structure
  public EntityDtoOut toDto(EntityGraphNode root, String entityTemplateIdentifier, int maxDepth) {
    if (root == null) {
      return null;
    }

    EntityTemplate entityTemplate = entityTemplateService
        .getEntityTemplateByIdentifier(entityTemplateIdentifier);

    return toDto(root, entityTemplate, maxDepth);
  }

  /// Internal orchestrator handling property conversion and deep
  /// structural flattening.
  private EntityDtoOut toDto(EntityGraphNode root, EntityTemplate entityTemplate, int maxDepth) {
    if (root == null) {
      return null;
    }

    Map<String, Object> properties = mapPropertiesFromGraphNode(root, entityTemplate);

    var state = new TraversalState(root.identifier(), new HashMap<>(), new HashMap<>(),
        new HashSet<>());

    // Initiate sequence tracking from baseline layer 0
    traverse(root, 0, maxDepth, state);

    Map<String, List<EntitySummaryDto>> finalizedRelations = state.relationsMap().entrySet()
        .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));

    return new EntityDtoOut(root.identifier(), root.name(), root.templateIdentifier(), properties,
        finalizedRelations);
  }

  /// Overloaded fallback extracting the primary node index from a page container.
  public EntityDtoOut toDto(Page<EntityGraphNode> page, EntityTemplate entityTemplate,
      int maxDepth) {
    if (page == null || page.getContent().isEmpty()) {
      return null;
    }

    return toDto(page.getContent().get(0), entityTemplate, maxDepth);
  }

  /// Converts a paginated list of domain tree elements into an aligned page of
  /// flattened output objects.
  ///
  /// @param graphNodesPage raw paginated collections returned by internal domain
  /// services
  /// @param entityTemplate the parent metadata template definition
  /// @param maxDepth the maximum layer boundary permitted for extraction
  /// @return a paginated series of flat [EntityDtoOut] summaries
  public Page<EntityDtoOut> toPageDto(Page<EntityGraphNode> graphNodesPage,
      String entityTemplateIdentifier, int maxDepth) {
    if (graphNodesPage == null) {
      return Page.empty();
    }

    EntityTemplate entityTemplate = entityTemplateService
        .getEntityTemplateByIdentifier(entityTemplateIdentifier);

    return graphNodesPage.map(node -> this.toDto(node, entityTemplate, maxDepth));
  }

  /// Deep recursive traversal driver evaluating node safety matrix steps. This
  /// method preserves low cognitive complexity by offloading nested structural
  /// loop evaluation down to dedicated directional processing helpers.
  private static void traverse(EntityGraphNode node, int currentDepth, int maxDepth,
      TraversalState state) {
    String nodeId = nodeId(node.templateIdentifier(), node.identifier());

    // DEPTH-AWARE MATRIX CHECK: Blocks paths only if seen previously at an
    // equal/closer depth level.
    // This ensures shared nodes discovered at lower depths are allowed to expose
    // their branches.
    Integer minDepthSeen = state.visitedNodeDepths().get(nodeId);
    if (minDepthSeen != null && currentDepth >= minDepthSeen) {
      return;
    }
    state.visitedNodeDepths().put(nodeId, currentDepth);

    // Map outbound and inbound geometries via isolated sub-methods
    processOutboundRelations(node, nodeId, currentDepth, maxDepth, state);
    processInboundRelations(node, nodeId, currentDepth, maxDepth, state);
  }

  /// Evaluates outbound dependency linkages originating from the targeted node
  /// scope.
  private static void processOutboundRelations(EntityGraphNode node, String nodeId,
      int currentDepth, int maxDepth, TraversalState state) {
    boolean shouldCollectRelations = currentDepth < maxDepth;

    for (EntityGraphRelation relation : node.relations()) {
      for (EntityGraphNode target : relation.targets()) {
        String targetId = nodeId(target.templateIdentifier(), target.identifier());
        String signature = nodeId + "|" + targetId + "|" + relation.name();

        // Guard: Do not add the root entity as a dependency of itself
        boolean isNotRoot = !target.identifier().equalsIgnoreCase(state.rootIdentifier());

        if (shouldCollectRelations && isNotRoot && state.emittedEdgeSignatures().add(signature)) {
          state.relationsMap().computeIfAbsent(relation.name(), k -> new LinkedHashSet<>())
              .add(new EntitySummaryDto(target.identifier(), target.name(),
                  target.templateIdentifier()));
        }

        if (currentDepth < maxDepth) {
          traverse(target, currentDepth + 1, maxDepth, state);
        }
      }
    }
  }

  /// Evaluates inbound backlink entries pointing towards the targeted node scope.
  private static void processInboundRelations(EntityGraphNode node, String nodeId, int currentDepth,
      int maxDepth, TraversalState state) {
    boolean shouldCollectRelations = currentDepth < maxDepth;

    for (EntityGraphRelation relation : node.relationsAsTarget()) {
      for (EntityGraphNode source : relation.targets()) {
        String sourceId = nodeId(source.templateIdentifier(), source.identifier());
        String signature = sourceId + "|" + nodeId + "|" + relation.name();

        // Guard: Do not add the root entity as a dependency of itself
        boolean isNotRoot = !source.identifier().equalsIgnoreCase(state.rootIdentifier());

        if (shouldCollectRelations && isNotRoot && state.emittedEdgeSignatures().add(signature)) {
          state.relationsMap().computeIfAbsent(relation.name(), k -> new LinkedHashSet<>())
              .add(new EntitySummaryDto(source.identifier(), source.name(),
                  source.templateIdentifier()));
        }

        if (currentDepth < maxDepth) {
          traverse(source, currentDepth + 1, maxDepth, state);
        }
      }
    }
  }

  private static String nodeId(String templateIdentifier, String identifier) {
    return templateIdentifier + ":" + identifier;
  }

  /// Safely maps raw domain properties to typed presentation properties.
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

  /// State container capturing cross-layer tracking matrices during execution.
  private record TraversalState(String rootIdentifier,
      Map<String, Set<EntitySummaryDto>> relationsMap, Map<String, Integer> visitedNodeDepths,
      Set<String> emittedEdgeSignatures) {
  }
}
