package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;

@Repository
public interface JpaEntityRepository
    extends
      JpaRepository<EntityJpaEntity, UUID>,
      JpaSpecificationExecutor<EntityJpaEntity> {

  @Query("SELECT e.identifier AS identifier, e.name AS name, e.templateIdentifier AS templateIdentifier FROM EntityJpaEntity e WHERE e.identifier IN :identifiers")
  List<EntitySummary> findByIdentifierIn(List<String> identifiers);

  /// Finds entity summaries by composite keys (templateIdentifier + identifier).
  ///
  /// **Purpose:** Batch lookup of entity summaries using composite keys to ensure
  /// uniqueness across templates. This prevents IllegalStateException when
  /// building summary maps or resolving relation targets.
  ///
  /// **Design:** Uses a native SQL query with tuple IN clause to match composite
  /// key pairs. Returns only the essential fields (identifier, name,
  /// templateIdentifier)
  /// for summary DTOs.
  ///
  /// **Why composite keys?** Entity identifiers are unique within a template, not
  /// globally. Using both templateIdentifier and identifier ensures we fetch the
  /// correct entity without conflicts.
  ///
  /// **Note:** Uses native SQL because JPQL doesn't support tuple IN clauses.
  /// The unnest function creates a table from the parameter arrays, and we join
  /// on both columns to match composite keys.
  ///
  /// @param templateIdentifiers list of template identifiers (parallel to
  /// identifiers)
  /// @param identifiers list of entity identifiers (parallel to
  /// templateIdentifiers)
  /// @return list of entity summaries matching the composite keys
  @Query(value = """
      SELECT e.identifier, e.name, e.template_identifier AS templateIdentifier
      FROM idp_core.entity e
      JOIN unnest(:templateIdentifiers, :identifiers) AS k(template_identifier, identifier)
        ON e.template_identifier = k.template_identifier AND e.identifier = k.identifier
      """, nativeQuery = true)
  List<EntitySummary> findByCompositeKeys(
      @Param("templateIdentifiers") String[] templateIdentifiers,
      @Param("identifiers") String[] identifiers);

  @Query("SELECT e.identifier AS identifier, e.name AS name, e.templateIdentifier AS templateIdentifier FROM EntityJpaEntity e JOIN e.relations r WHERE r.id IN :relationIds")
  List<EntitySummary> findByRelationIdIn(List<UUID> relationIds);

  Optional<EntityJpaEntity> findByTemplateIdentifierAndIdentifier(String templateIdentifier,
      String identifier);

  Optional<EntityJpaEntity> findByTemplateIdentifierAndName(String templateIdentifier, String name);

  Page<EntityJpaEntity> findByTemplateIdentifier(String templateIdentifier, Pageable pageable);

  /// Batch fetch entities by identifiers with eager loading of relations and
  /// properties. Uses two separate queries to avoid Hibernate's
  /// MultipleBagFetchException. First fetches entities with relations, then
  /// fetches properties separately.
  @Query("""
      SELECT DISTINCT e
      FROM EntityJpaEntity e
      LEFT JOIN FETCH e.relations r
      WHERE e.id IN :ids
      """)
  List<EntityJpaEntity> findAllByIdinWithRelations(@Param("ids") Collection<UUID> ids);

  /// Fetch properties for entities that were already loaded. This is called after
  /// findAllByIdInWithRelations to complete the entity graph.
  @Query("SELECT DISTINCT e FROM EntityJpaEntity e LEFT JOIN FETCH e.properties WHERE e.id IN :ids")
  List<EntityJpaEntity> findAllByIdInWithProperties(@Param("ids") Collection<UUID> ids);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      DELETE FROM PropertyJpaEntity p
      WHERE p IN (
        SELECT p2 FROM EntityJpaEntity e JOIN e.properties p2
        WHERE e.templateIdentifier = :templateIdentifier
        AND p2.name IN :propertyNames
      )
      """)
  void deletePropertiesByTemplateIdentifierAndPropertyName(
      @Param("templateIdentifier") String templateIdentifier,
      @Param("propertyNames") Collection<String> propertyNames);

  /// Deletes all relations of specific types associated with a template.
  ///
  /// **Purpose:** Removes relations by name for all entities of a given template.
  /// This is useful during template updates when certain relation types need to
  /// be
  /// cleaned up before reloading.
  ///
  /// **Design:** Uses a nested query to identify relations first, then removes
  /// them.
  /// The `@Modifying` annotation automatically clears the persistence context and
  /// flushes changes to the database.
  ///
  /// @param templateIdentifier the template identifier to filter entities
  /// @param relationNames collection of relation names to delete
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      DELETE FROM RelationJpaEntity r
      WHERE r IN (
        SELECT r2 FROM EntityJpaEntity e JOIN e.relations r2
        WHERE e.templateIdentifier = :templateIdentifier
        AND r2.name IN :relationNames
      )
      """)
  void deleteRelationsByTemplateIdentifierAndRelationName(
      @Param("templateIdentifier") String templateIdentifier,
      @Param("relationNames") Collection<String> relationNames);

  /// Deletes an entity and all its associated relations by template and
  /// identifier.
  ///
  /// **Purpose:** Removes a single entity record from the database along with all
  /// relations it participates in (both as source and target via cascading
  /// delete).
  ///
  /// **Design:** Uses the composite key (templateIdentifier, identifier) to
  /// uniquely
  /// identify the entity, matching the domain model's identity semantics.
  ///
  /// @param templateIdentifier the template identifier of the entity
  /// @param entityIdentifier the identifier of the entity within its template
  void deleteByTemplateIdentifierAndIdentifier(
      @Param("templateIdentifier") String templateIdentifier,
      @Param("entityIdentifier") String entityIdentifier);

  /// Finds all entities with a specific template identifier and identifiers.
  ///
  /// **Purpose:** Batch lookup of multiple entities within a template using their
  /// identifiers. Used during graph traversal to fetch entities in bulk.
  ///
  /// **Design:** Leverages the composite key (templateIdentifier, identifier) for
  /// efficient filtering. Reduces N+1 queries by loading multiple entities in one
  /// call.
  ///
  /// @param templateIdentifier the template identifier to filter by
  /// @param identifiers collection of entity identifiers to retrieve
  /// @return list of matching entities, or empty list if none found
  List<EntityJpaEntity> findAllByTemplateIdentifierAndIdentifierIn(String templateIdentifier,
      List<String> identifiers);

  /// Finds all entities that have relations pointing to a target entity.
  ///
  /// **Purpose:** Discovers inbound relationships—entities that reference the
  /// given
  /// target identifier. Essential for bidirectional graph traversal to find
  /// dependents.
  ///
  /// **Design:** Uses a native SQL query for complex join logic across the
  /// relation
  /// hierarchy (entity → entity_relations → relation_target_entities → entity
  /// target).
  /// This provides better control over the join strategy than JPQL for this
  /// specific
  /// multi-level traversal.
  ///
  /// **Performance:** Avoids N+1 by using a single join query. The DISTINCT
  /// clause
  /// eliminates duplicates when multiple relations point to the same target.
  ///
  /// @param targetIdentifier the identifier of the target entity to find
  /// referrers for
  /// @return list of entities that have relations to the target, or empty list if
  /// none
  @Query(value = """
      SELECT DISTINCT e.*
      FROM idp_core.entity e
      JOIN idp_core.entity_relations er ON er.entity_id = e.id
      JOIN idp_core.relation_target_entities rte ON rte.relation_id = er.relation_id
      JOIN idp_core.entity target ON target.id = rte.target_entity_uuid
      WHERE target.identifier = :targetIdentifier
      """, nativeQuery = true)
  List<EntityJpaEntity> findEntitiesRelated(@Param("targetIdentifier") String targetIdentifier);

  /// Discovers all entity UUIDs reachable from root entities within a depth
  /// limit.
  ///
  /// **Purpose:** Executes a recursive Common Table Expression (CTE) to find all
  /// entities connected to the root(s) via relationships, respecting depth and
  /// traversal mode constraints. Returns only UUIDs for efficient bulk loading.
  ///
  /// **Algorithm:** Breadth-First Search using recursive SQL CTE with three
  /// phases:
  ///
  /// 1. **Anchor Member**: Initializes state tokens (UUID, depth, flow direction)
  /// for
  /// root entities. Flow direction matches the traversal mode:
  /// - `OUTBOUND_ONLY`: Only OUTBOUND flow
  /// - `DIRECT_LINEAGE`: Both OUTBOUND and INBOUND (separate tokens per root)
  /// - `BIDIRECTIONAL`: Only ANY flow (follows both directions)
  ///
  /// 2. **Recursive Member**: Propagates state tokens through the graph:
  /// - Matches flow direction (e.g., OUTBOUND token only follows outbound edges)
  /// - Stops when depth limit is reached
  /// - Respects flow semantics to prevent invalid path combinations
  ///
  /// 3. **Final Select**: Returns distinct UUIDs of all discovered nodes
  ///
  /// **Why state tokens?** The flow direction (OUTBOUND/INBOUND/ANY) is stored in
  /// the recursive state to enforce traversal rules. For example, in
  /// DIRECT_LINEAGE:
  /// - One OUTBOUND token flows downstream from root
  /// - One INBOUND token flows upstream from root
  /// - They never cross over (outbound token cannot follow inbound edge)
  ///
  /// **Performance:** Returns only UUIDs (no entity hydration). Bulk entity
  /// loading
  /// happens in a separate query to avoid N+1 problems. The depth limit prevents
  /// unbounded traversal in cyclic graphs.
  ///
  /// @param rootIds collection of root entity UUIDs (single or multiple)
  /// @param depth maximum traversal depth (must be >= 1, typically clamped by
  /// domain)
  /// @param mode traversal mode as string ('OUTBOUND_ONLY', 'DIRECT_LINEAGE',
  /// 'BIDIRECTIONAL')
  /// @return list of all discovered entity UUIDs in the reachable subgraph, or
  /// empty
  /// list if no entities are reachable
  @Query(value = """
      WITH RECURSIVE entity_graph(id, depth, flow) AS (
          -- 1. ANCHOR MEMBER: Initialize state tokens for multiple root entities
          SELECT e.id, 0, 'OUTBOUND' AS flow
          FROM idp_core.entity e
          WHERE e.id IN :rootIds AND :mode IN ('DIRECT_LINEAGE', 'OUTBOUND_ONLY')

          UNION

          SELECT e.id, 0, 'INBOUND' AS flow
          FROM idp_core.entity e
          WHERE e.id IN :rootIds AND :mode = 'DIRECT_LINEAGE'

          UNION

          SELECT e.id, 0, 'ANY' AS flow
          FROM idp_core.entity e
          WHERE e.id IN :rootIds AND :mode = 'BIDIRECTIONAL'

          UNION

          -- 2. RECURSIVE MEMBER: Propagate isolated pathways down the graph footprint
          SELECT combined.id, eg.depth + 1, eg.flow
          FROM entity_graph eg
          JOIN (
              -- Outbound Paths
              SELECT er.entity_id AS source_id, rte.target_entity_uuid AS id, 'OUTBOUND' AS flow_match
              FROM idp_core.entity_relations er
              JOIN idp_core.relation_target_entities rte ON rte.relation_id = er.relation_id
              WHERE rte.target_entity_uuid IS NOT NULL

              UNION ALL

              SELECT er.entity_id AS source_id, rte.target_entity_uuid AS id, 'ANY' AS flow_match
              FROM idp_core.entity_relations er
              JOIN idp_core.relation_target_entities rte ON rte.relation_id = er.relation_id
              WHERE rte.target_entity_uuid IS NOT NULL

              UNION ALL

              -- Inbound Paths
              SELECT rte.target_entity_uuid AS source_id, er.entity_id AS id, 'INBOUND' AS flow_match
              FROM idp_core.relation_target_entities rte
              JOIN idp_core.entity_relations er ON er.relation_id = rte.relation_id

              UNION ALL

              SELECT rte.target_entity_uuid AS source_id, er.entity_id AS id, 'ANY' AS flow_match
              FROM idp_core.relation_target_entities rte
              JOIN idp_core.entity_relations er ON er.relation_id = rte.relation_id
          ) combined ON combined.source_id = eg.id AND combined.flow_match = eg.flow
          WHERE eg.depth < :depth
      )
      -- 3. Return the clean deduplicated set of structural skeleton UUIDs
      SELECT DISTINCT id FROM entity_graph;
      """, nativeQuery = true)
  List<UUID> findEntityIdsInGraph(@Param("rootIds") Collection<UUID> rootIds,
      @Param("depth") int depth, @Param("mode") String mode);
}
