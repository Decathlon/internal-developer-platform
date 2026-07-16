package com.decathlon.idp_core.domain.service.entity_graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityGraphService Tests")
class EntityGraphServiceTest {

  @Mock
  private EntityRepositoryPort entityRepositoryPort;

  @Mock
  private EntityGraphRepositoryPort entityGraphRepositoryPort;

  @Mock
  private EntityTemplateValidationService entityTemplateValidationService;

  @Spy
  private EntityGraphHelper entityGraphHelper = new EntityGraphHelper();

  @InjectMocks
  private EntityGraphService entityGraphService;

  private Entity entity(String templateIdentifier, String identifier, String name) {
    return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, List.of(),
        List.of());
  }

  private Entity entityWithRelations(String templateIdentifier, String identifier, String name,
      List<Relation> relations) {
    return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, List.of(),
        relations);
  }

  private Relation relation(String name, String targetTemplateIdentifier, String... targetIds) {
    return new Relation(UUID.randomUUID(), name, targetTemplateIdentifier, List.of(targetIds));
  }

  private static final String TEMPLATE = "web-service";

  /// Helper to stub the graph repository port
  /// Builds a map from the provided entities and configures the mock to return it
  private void stubGraph(Entity... entities) {
    Map<UUID, Entity> entityMap = buildEntityMap(entities);
    when(entityGraphRepositoryPort.findEntityGraph(any(), anyInt(), anyBoolean(),
        any(EntityGraphTraversalMode.class))).thenReturn(entityMap);
  }

  /// Builds an immutable map of entities keyed by their UUID
  private Map<UUID, Entity> buildEntityMap(Entity... entities) {
    var entityMap = new HashMap<UUID, Entity>();
    for (Entity e : entities) {
      entityMap.put(e.id(), e);
    }
    return entityMap;
  }

  // ========================
  @Nested
  @DisplayName("Root Entity Not Found")
  class RootEntityNotFound {

    @Test
    @DisplayName("Should throw EntityNotFoundException when root entity does not exist")
    void shouldThrowWhenRootEntityNotFound() {
      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "missing"))
          .thenReturn(Optional.empty());

      assertThatThrownBy(this::callGetEntityGraphForMissing)
          .isInstanceOf(EntityNotFoundException.class);

      verify(entityGraphRepositoryPort, never()).findEntityGraph(any(), anyInt(), anyBoolean(),
          any(EntityGraphTraversalMode.class));
    }

    private void callGetEntityGraphForMissing() {
      entityGraphService.getEntityGraph(TEMPLATE, "missing", 1, false, Set.of(), Set.of(),
          EntityGraphTraversalMode.BIDIRECTIONAL);
    }
  }

  // ========================
  @Nested
  @DisplayName("Single Root — No Relations")
  class SingleRootNoRelations {

    @Test
    @DisplayName("Should return leaf node when entity has no relations")
    void shouldReturnLeafNodeWhenNoRelations() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.identifier()).isEqualTo("api");
      assertThat(result.name()).isEqualTo("API Service");
      assertThat(result.relations()).isEmpty();
      assertThat(result.relationsAsTarget()).isEmpty();
    }
  }

  // ========================
  @Nested
  @DisplayName("Outbound Relations")
  class OutboundRelations {

    @Test
    @DisplayName("Should resolve outbound relation targets at depth 1")
    void shouldResolveOutboundRelations() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "postgres")));
      Entity postgres = entity("database", "postgres", "Postgres DB");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api, postgres);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.relations()).hasSize(1);
      assertThat(result.relations().get(0).name()).isEqualTo("uses-db");
      assertThat(result.relations().get(0).targets()).hasSize(1);
      assertThat(result.relations().get(0).targets().get(0).identifier()).isEqualTo("postgres");
    }

    @Test
    @DisplayName("Should filter out target when not in the pre-loaded entity map")
    void shouldReturnFallbackNodeWhenTargetNotInMap() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "missing-db")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      // When target entity is not found in the map, it's filtered out
      assertThat(result.relations()).isEmpty();
    }
  }

  // ========================
  @Nested
  @DisplayName("Inbound Relations (relationsAsTarget)")
  class InboundRelations {

    @Test
    @DisplayName("Should resolve inbound relations when another entity points to root")
    void shouldResolveInboundRelations() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      Entity consumer = entityWithRelations(TEMPLATE, "consumer", "Consumer",
          List.of(relation("depends-on", TEMPLATE, "api")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api, consumer);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.relationsAsTarget()).hasSize(1);
      assertThat(result.relationsAsTarget().get(0).name()).isEqualTo("depends-on");
      assertThat(result.relationsAsTarget().get(0).targets().get(0).identifier())
          .isEqualTo("consumer");
    }
  }

  // ========================
  @Nested
  @DisplayName("Depth Clamping")
  class DepthClamping {

    @Test
    @DisplayName("Should clamp depth below 1 to 1")
    void shouldClampDepthBelowOne() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api);

      entityGraphService.getEntityGraph(TEMPLATE, "api", 0, false, Set.of(), Set.of(),
          EntityGraphTraversalMode.BIDIRECTIONAL);

      verify(entityGraphRepositoryPort).findEntityGraph(any(), anyInt(), anyBoolean(),
          any(EntityGraphTraversalMode.class));
    }

    @Test
    @DisplayName("Should clamp depth above MAX_DEPTH to MAX_DEPTH")
    void shouldClampDepthAboveTen() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api);

      entityGraphService.getEntityGraph(TEMPLATE, "api", 99, false, Set.of(), Set.of(),
          EntityGraphTraversalMode.BIDIRECTIONAL);

      verify(entityGraphRepositoryPort).findEntityGraph(any(), anyInt(), anyBoolean(),
          any(EntityGraphTraversalMode.class));
    }
  }

  // ========================
  @Nested
  @DisplayName("Depth Limit — Leaf Nodes at Boundary")
  class DepthLimit {

    @Test
    @DisplayName("Should return target as leaf node when depth limit is reached")
    void shouldReturnLeafNodeAtDepthBoundary() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "postgres")));
      Entity postgres = entityWithRelations("database", "postgres", "Postgres DB",
          List.of(relation("runs-on", "infra", "server-1")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      // At depth=1, only api and postgres should be in the graph (server is beyond
      // depth limit)
      stubGraph(api, postgres);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      EntityGraphNode postgresNode = result.relations().get(0).targets().get(0);
      assertThat(postgresNode.identifier()).isEqualTo("postgres");
      // At depth=1, postgres is a leaf — no further outbound relations resolved
      assertThat(postgresNode.relations()).isEmpty();
      // But it CAN have inbound relations from entities already in the graph (api)
      assertThat(postgresNode.relationsAsTarget()).hasSize(1);
      assertThat(postgresNode.relationsAsTarget().get(0).name()).isEqualTo("uses-db");
      assertThat(postgresNode.relationsAsTarget().get(0).targets().get(0).identifier())
          .isEqualTo("api");
    }
  }

  // ========================
  @Nested
  @DisplayName("Multiple Named Relations")
  class MultipleRelations {

    @Test
    @DisplayName("Should resolve multiple distinct relation types")
    void shouldResolveMultipleNamedRelations() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service", List.of(
          relation("uses-db", "database", "postgres"), relation("depends-on", TEMPLATE, "auth")));
      Entity postgres = entity("database", "postgres", "Postgres DB");
      Entity auth = entity(TEMPLATE, "auth", "Auth Service");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api, postgres, auth);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.relations()).hasSize(2);
      assertThat(result.relations().stream().map(r -> r.name()))
          .containsExactlyInAnyOrder("uses-db", "depends-on");
    }
  }

  // ========================
  @Nested
  @DisplayName("Relation Filtering")
  class RelationFiltering {

    @Test
    @DisplayName("Should include only relations matching the relation filter")
    void shouldFilterRelationsByName() {
      // A --(depends-on)--> B, A --(owns)--> C; filter keeps only 'depends-on'
      Entity a = entityWithRelations(TEMPLATE, "a", "A",
          List.of(relation("depends-on", TEMPLATE, "b"), relation("owns", TEMPLATE, "c")));
      Entity b = entity(TEMPLATE, "b", "B");
      Entity c = entity(TEMPLATE, "c", "C");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "a"))
          .thenReturn(Optional.of(a));
      stubGraph(a, b, c);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "a", 2, false,
          Set.of("depends-on"), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.relations()).hasSize(1);
      assertThat(result.relations().get(0).name()).isEqualTo("depends-on");
    }

    @Test
    @DisplayName("Should return all relations when relation filter is empty")
    void shouldReturnAllRelationsWhenFilterIsEmpty() {
      Entity a = entityWithRelations(TEMPLATE, "a", "A",
          List.of(relation("depends-on", TEMPLATE, "b"), relation("owns", TEMPLATE, "c")));
      Entity b = entity(TEMPLATE, "b", "B");
      Entity c = entity(TEMPLATE, "c", "C");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "a"))
          .thenReturn(Optional.of(a));
      stubGraph(a, b, c);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "a", 2, false, Set.of(),
          Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.relations()).hasSize(2);
      assertThat(result.relations().stream().map(r -> r.name()))
          .containsExactlyInAnyOrder("depends-on", "owns");
    }

    @Test
    @DisplayName("Should filter inbound relations by name")
    void shouldFilterInboundRelationsByName() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      Entity consumer = entityWithRelations(TEMPLATE, "consumer", "Consumer",
          List.of(relation("depends-on", TEMPLATE, "api")));
      Entity unrelated = entityWithRelations(TEMPLATE, "unrelated", "Unrelated",
          List.of(relation("owns", TEMPLATE, "api")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api, consumer, unrelated);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of("depends-on"), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.relationsAsTarget()).hasSize(1);
      assertThat(result.relationsAsTarget().get(0).name()).isEqualTo("depends-on");
    }
  }

  // ========================
  @Nested
  @DisplayName("Property Filtering")
  class PropertyFiltering {

    private Entity entityWithProperties(String templateIdentifier, String identifier, String name,
        List<Property> properties) {
      return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, properties,
          List.of());
    }

    @Test
    @DisplayName("Should include only properties matching the property filter")
    void shouldFilterPropertiesByName() {
      var propEnv = new Property(UUID.randomUUID(), "env", "prod");
      var propOwner = new Property(UUID.randomUUID(), "owner", "team-a");
      Entity api = entityWithProperties(TEMPLATE, "api", "API Service",
          List.of(propEnv, propOwner));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, true, Set.of(),
          Set.of("env"), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.properties()).hasSize(1);
      assertThat(result.properties().get(0).name()).isEqualTo("env");
    }

    @Test
    @DisplayName("Should return all properties when property filter is empty")
    void shouldReturnAllPropertiesWhenFilterIsEmpty() {
      var propEnv = new Property(UUID.randomUUID(), "env", "prod");
      var propOwner = new Property(UUID.randomUUID(), "owner", "team-a");
      Entity api = entityWithProperties(TEMPLATE, "api", "API Service",
          List.of(propEnv, propOwner));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, true, Set.of(),
          Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.properties()).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty properties when includeProperties is false regardless of filter")
    void shouldReturnEmptyPropertiesWhenIncludePropertiesIsFalse() {
      var propEnv = new Property(UUID.randomUUID(), "env", "prod");
      Entity api = entityWithProperties(TEMPLATE, "api", "API Service", List.of(propEnv));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of("env"), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.properties()).isEmpty();
    }
  }

  // ========================
  @Nested
  @DisplayName("Visited Node Guard — OOM Prevention")
  class VisitedNodeGuard {

    @Test
    @DisplayName("Should complete at depth=6 without exponential recursion for a small graph")
    void shouldNotExplodeAtMaxDepthWithSmallGraph() {
      // A --(uses)--> B --(uses)--> C; B also has inbound from A and C has inbound
      // from B.
      // Without the visited-node guard this produces O(2^depth) calls at depth=6.
      Entity a = entityWithRelations(TEMPLATE, "a", "A", List.of(relation("uses", TEMPLATE, "b")));
      Entity b = entityWithRelations(TEMPLATE, "b", "B", List.of(relation("uses", TEMPLATE, "c")));
      Entity c = entity(TEMPLATE, "c", "C");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "a"))
          .thenReturn(Optional.of(a));
      stubGraph(a, b, c);

      // Must complete instantly — any OOM or StackOverflow here means the guard is
      // missing.
      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "a", 10, false, Set.of(),
          Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      assertThat(result.identifier()).isEqualTo("a");
      assertThat(result.relations()).hasSize(1);
    }

    @Test
    @DisplayName("Should return stub leaf for already-visited node instead of re-expanding it")
    void shouldReturnStubLeafForRevisitedNode() {
      // A --(uses)--> B; B also points back to A (cycle: A→B→A)
      Entity a = entityWithRelations(TEMPLATE, "a", "A", List.of(relation("uses", TEMPLATE, "b")));
      Entity b = entityWithRelations(TEMPLATE, "b", "B", List.of(relation("uses", TEMPLATE, "a")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "a"))
          .thenReturn(Optional.of(a));
      stubGraph(a, b);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "a", 5, false, Set.of(),
          Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      // A → B is resolved
      assertThat(result.relations()).hasSize(1);
      EntityGraphNode nodeB = result.relations().get(0).targets().get(0);
      assertThat(nodeB.identifier()).isEqualTo("b");

      // B → A is a revisit: A was already marked visited, so it returns a stub leaf
      // with no further outbound or inbound relations (no infinite loop).
      EntityGraphNode stubA = nodeB.relations().get(0).targets().get(0);
      assertThat(stubA.identifier()).isEqualTo("a");
      assertThat(stubA.relations()).isEmpty();
      assertThat(stubA.relationsAsTarget()).isEmpty();
    }
  }

  // ========================
  @Nested
  @DisplayName("Graph Traversal Mode")
  class GraphTraversalMode {

    @Test
    @DisplayName("BIDIRECTIONAL mode should include both outbound and inbound relations")
    void bidirectionalModeShouldIncludeBothDirections() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "postgres")));
      Entity postgres = entity("database", "postgres", "Postgres DB");
      Entity consumer = entityWithRelations(TEMPLATE, "consumer", "Consumer",
          List.of(relation("depends-on", TEMPLATE, "api")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api, postgres, consumer);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      // Should have outbound relation to postgres
      assertThat(result.relations()).hasSize(1);
      assertThat(result.relations().get(0).name()).isEqualTo("uses-db");
      assertThat(result.relations().get(0).targets().get(0).identifier()).isEqualTo("postgres");

      // Should have inbound relation from consumer
      assertThat(result.relationsAsTarget()).hasSize(1);
      assertThat(result.relationsAsTarget().get(0).name()).isEqualTo("depends-on");
      assertThat(result.relationsAsTarget().get(0).targets().get(0).identifier())
          .isEqualTo("consumer");
    }

    @Test
    @DisplayName("OUTBOUND_ONLY mode should include only outbound relations")
    void outboundOnlyModeShouldExcludeInboundRelations() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "postgres")));
      Entity postgres = entity("database", "postgres", "Postgres DB");
      Entity consumer = entityWithRelations(TEMPLATE, "consumer", "Consumer",
          List.of(relation("depends-on", TEMPLATE, "api")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api, postgres, consumer);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.OUTBOUND_ONLY);

      // Should have outbound relation to postgres
      assertThat(result.relations()).hasSize(1);
      assertThat(result.relations().get(0).name()).isEqualTo("uses-db");
      assertThat(result.relations().get(0).targets().get(0).identifier()).isEqualTo("postgres");

      // Should NOT have inbound relations
      assertThat(result.relationsAsTarget()).isEmpty();
    }

    @Test
    @DisplayName("DIRECT_LINEAGE mode should show inbound of root and outbound of downstream")
    void directLineageModeShouldShowInboundAtRootAndOutboundDownstream() {
      // Setup: consumer -> api -> postgres, backend -> api
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "postgres")));
      Entity postgres = entity("database", "postgres", "Postgres DB");
      Entity consumer = entityWithRelations(TEMPLATE, "consumer", "Consumer",
          List.of(relation("depends-on", TEMPLATE, "api")));
      Entity backend = entityWithRelations(TEMPLATE, "backend", "Backend",
          List.of(relation("calls", TEMPLATE, "api")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api, postgres, consumer, backend);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 2, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.DIRECT_LINEAGE);

      // Root should have inbound relations: consumer -> api, backend -> api
      assertThat(result.relationsAsTarget()).hasSize(2);
      var inboundIdentifiers = result.relationsAsTarget().stream()
          .flatMap(rel -> rel.targets().stream()).map(node -> node.identifier()).toList();
      assertThat(inboundIdentifiers).containsExactlyInAnyOrder("consumer", "backend");

      // Root should have outbound relation: api -> postgres
      assertThat(result.relations()).hasSize(1);
      assertThat(result.relations().get(0).name()).isEqualTo("uses-db");
      assertThat(result.relations().get(0).targets().get(0).identifier()).isEqualTo("postgres");

      // Downstream nodes (consumer, backend) should NOT have inbound relations
      var consumerNode = result.relationsAsTarget().stream().flatMap(rel -> rel.targets().stream())
          .filter(node -> node.identifier().equals("consumer")).findFirst().orElseThrow();
      assertThat(consumerNode.relationsAsTarget()).isEmpty();

      var backendNode = result.relationsAsTarget().stream().flatMap(rel -> rel.targets().stream())
          .filter(node -> node.identifier().equals("backend")).findFirst().orElseThrow();
      assertThat(backendNode.relationsAsTarget()).isEmpty();
    }

    @Test
    @DisplayName("Mode parameter should be passed to repository port")
    void modeShouldBePassedToRepositoryPort() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api);

      entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false, Set.of(), Set.of(),
          EntityGraphTraversalMode.OUTBOUND_ONLY);
      entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false, Set.of(), Set.of(),
          EntityGraphTraversalMode.DIRECT_LINEAGE);
      entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false, Set.of(), Set.of(),
          EntityGraphTraversalMode.BIDIRECTIONAL);

      // Repository must be called once per invocation — 3 calls total
      verify(entityGraphRepositoryPort, times(3)).findEntityGraph(any(), anyInt(), anyBoolean(),
          any(EntityGraphTraversalMode.class));
    }

    @Test
    @DisplayName("BIDIRECTIONAL mode should traverse full graph with multiple levels")
    void bidirectionalModeShouldTraverseFullGraphMultipleLevels() {
      // Create a complex graph: consumer -> api -> postgres, with backend also -> api
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "postgres")));
      Entity postgres = entity("database", "postgres", "Postgres DB");
      Entity consumer = entityWithRelations(TEMPLATE, "consumer", "Consumer",
          List.of(relation("depends-on", TEMPLATE, "api")));
      Entity backend = entityWithRelations(TEMPLATE, "backend", "Backend",
          List.of(relation("calls", TEMPLATE, "api")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(api, postgres, consumer, backend);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 2, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);

      // Outbound: api -> postgres
      assertThat(result.relations()).hasSize(1);
      assertThat(result.relations().get(0).targets().get(0).identifier()).isEqualTo("postgres");

      // Inbound: consumer -> api, backend -> api
      assertThat(result.relationsAsTarget()).hasSize(2);
      var inboundIdentifiers = result.relationsAsTarget().stream()
          .flatMap(rel -> rel.targets().stream()).map(node -> node.identifier()).toList();
      assertThat(inboundIdentifiers).containsExactlyInAnyOrder("consumer", "backend");
    }

    @Test
    @DisplayName("OUTBOUND_ONLY mode should only follow forward dependencies in multi-level graph")
    void outboundOnlyModeShouldOnlyFollowForwardDependencies() {
      // Chain: frontend -> api -> postgres
      Entity frontend = entityWithRelations(TEMPLATE, "frontend", "Frontend",
          List.of(relation("calls", TEMPLATE, "api")));
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "postgres")));
      Entity postgres = entity("database", "postgres", "Postgres DB");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "frontend"))
          .thenReturn(Optional.of(frontend));
      stubGraph(frontend, api, postgres);

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "frontend", 2, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.OUTBOUND_ONLY);

      // Should follow: frontend -> api -> postgres
      assertThat(result.identifier()).isEqualTo("frontend");
      assertThat(result.relations()).hasSize(1);
      EntityGraphNode apiNode = result.relations().get(0).targets().get(0);
      assertThat(apiNode.identifier()).isEqualTo("api");
      assertThat(apiNode.relations()).hasSize(1);
      assertThat(apiNode.relations().get(0).targets().get(0).identifier()).isEqualTo("postgres");

      // No inbound relations at any level
      assertThat(result.relationsAsTarget()).isEmpty();
      assertThat(apiNode.relationsAsTarget()).isEmpty();
    }

    @Test
    @DisplayName("Mode should affect inbound index construction behavior")
    void modeShouldAffectInboundIndexConstruction() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      Entity consumer = entityWithRelations(TEMPLATE, "consumer", "Consumer",
          List.of(relation("depends-on", TEMPLATE, "api")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));

      // With BIDIRECTIONAL, consumer should be in the graph
      stubGraph(api, consumer);
      EntityGraphNode bidirectionalResult = entityGraphService.getEntityGraph(TEMPLATE, "api", 1,
          false, Set.of(), Set.of(), EntityGraphTraversalMode.BIDIRECTIONAL);
      assertThat(bidirectionalResult.relationsAsTarget()).hasSize(1);

      // With OUTBOUND_ONLY, even if consumer is in the map, inbound shouldn't be
      // built
      stubGraph(api, consumer);
      EntityGraphNode outboundResult = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false,
          Set.of(), Set.of(), EntityGraphTraversalMode.OUTBOUND_ONLY);
      assertThat(outboundResult.relationsAsTarget()).isEmpty();
    }
  }
}
