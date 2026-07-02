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
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.LineageIdProjection;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;

@Repository
public interface JpaEntityRepository
    extends
      JpaRepository<EntityJpaEntity, UUID>,
      JpaSpecificationExecutor<EntityJpaEntity> {

  @Query("SELECT e.identifier AS identifier, e.name AS name, e.templateIdentifier AS templateIdentifier FROM EntityJpaEntity e WHERE e.identifier IN :identifiers")
  List<EntitySummary> findByIdentifierIn(List<String> identifiers);

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

  @Query(value = """
      WITH RECURSIVE entity_graph(id, depth, flow) AS (
          -- 1. ANCHOR MEMBER: Initialize state tokens for a single root entity
          SELECT e.id, 0, 'OUTBOUND' AS flow
          FROM idp_core.entity e
          WHERE e.id = :rootId AND :mode IN ('DIRECT_LINEAGE', 'OUTBOUND_ONLY')

          UNION

          SELECT e.id, 0, 'INBOUND' AS flow
          FROM idp_core.entity e
          WHERE e.id = :rootId AND :mode = 'DIRECT_LINEAGE'

          UNION

          SELECT e.id, 0, 'ANY' AS flow
          FROM idp_core.entity e
          WHERE e.id = :rootId AND :mode = 'BIDIRECTIONAL'

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
  List<UUID> findEntityIdsInGraph(@Param("rootId") UUID rootId, @Param("depth") int depth,
      @Param("mode") String mode);

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

  List<EntityJpaEntity> findAllByTemplateIdentifierAndIdentifierIn(String templateIdentifier,
      List<String> identifiers);

  // Find all entities that have relations pointing to the given target
  // identifier. Uses a native query for better control over the join strategy.
  @Query(value = """
      SELECT DISTINCT e.*
      FROM idp_core.entity e
      JOIN idp_core.entity_relations er ON er.entity_id = e.id
      JOIN idp_core.relation_target_entities rte ON rte.relation_id = er.relation_id
      JOIN idp_core.entity target ON target.id = rte.target_entity_uuid
      WHERE target.identifier = :targetIdentifier
      """, nativeQuery = true)
  List<EntityJpaEntity> findEntitiesRelated(@Param("targetIdentifier") String targetIdentifier);

  void deleteByTemplateIdentifierAndIdentifier(
      @Param("templateIdentifier") String templateIdentifier,
      @Param("entityIdentifier") String entityIdentifier);

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
  List<UUID> findEntityGraphIdentifiersBatch(@Param("rootIds") Collection<UUID> rootIds,
      @Param("depth") int depth, @Param("mode") String mode);

  @Query(value = """
              WITH RECURSIVE agnostic_discovery(id, depth) AS (
          -- ANCHOR: Start directly using the resolved target UUIDs
          SELECT e.id, 0
          FROM idp_core.entity e
          WHERE e.id IN (:rootIds)

          UNION

          -- RECURSIVE STEP: Trace connections across the edge tables
          SELECT combined.id, ad.depth + 1
          FROM agnostic_discovery ad
          JOIN (
              SELECT rte.target_entity_uuid AS anchor_id, er.entity_id AS id
              FROM idp_core.relation_target_entities rte
              JOIN idp_core.entity_relations er ON er.relation_id = rte.relation_id

              UNION ALL

              SELECT er.entity_id AS anchor_id, rte.target_entity_uuid AS id
              FROM idp_core.entity_relations er
              JOIN idp_core.relation_target_entities rte ON rte.relation_id = er.relation_id
              WHERE rte.target_entity_uuid IS NOT NULL
          ) combined ON combined.anchor_id = ad.id
          WHERE ad.depth < :depth
      )
      -- Filter down to the starting template (e.g., 'component') and paginate the roots
      SELECT DISTINCT e.id
      FROM agnostic_discovery ad
      JOIN idp_core.entity e ON e.id = ad.id
      WHERE e.template_identifier = :startTemplate
      LIMIT :size OFFSET :offset
          """, nativeQuery = true)
  List<UUID> findEntityGraphIdsByTemplate(@Param("rootIds") Collection<UUID> rootIds,
      @Param("depth") int depth, @Param("startTemplate") String startTemplate,
      @Param("size") int size, @Param("offset") int offset);

  @Query(value = """
      WITH RECURSIVE
              -- 1. ANCHOR: Map incoming root IDs to their parallel validation groups
              input_criteria(id, group_id) AS (
                  SELECT t.root_id, t.grp_id
                  FROM UNNEST(CAST(:rootIds AS uuid[]), CAST(:groupIds AS text[])) AS t(root_id, grp_id)
              ),

              -- 2. RECURSIVE PATH SEARCH: Carry group tokens AND enforce direct lineage flows
              agnostic_discovery(id, group_id, flow, depth) AS (
                  -- Initialize every path starting from the filtered targets as an INBOUND flow
                  SELECT ic.id, ic.group_id, 'INBOUND' AS flow, 0
                  FROM input_criteria ic

                  UNION

                  -- Propagate paths only where the edge direction matches the current flow lane
                  SELECT combined.id, ad.group_id, ad.flow, ad.depth + 1
                  FROM agnostic_discovery ad
                  JOIN (
                      -- Inbound Paths: Moving backward from Target to Source
                      SELECT rte.target_entity_uuid AS anchor_id, er.entity_id AS id, 'INBOUND' AS flow_match
                      FROM idp_core.relation_target_entities rte
                      JOIN idp_core.entity_relations er ON er.relation_id = rte.relation_id

                      UNION ALL

                      -- Outbound Paths: Moving forward from Source to Target
                      SELECT er.entity_id AS anchor_id, rte.target_entity_uuid AS id, 'OUTBOUND' AS flow_match
                      FROM idp_core.entity_relations er
                      JOIN idp_core.relation_target_entities rte ON rte.relation_id = er.relation_id
                      WHERE rte.target_entity_uuid IS NOT NULL
                  ) combined ON combined.anchor_id = ad.id AND combined.flow_match = ad.flow
                  WHERE ad.depth < :depth
              ),

              -- 3. INTERSECTION CORE: Group by entity and assert all distinct tokens were collected
              matched_candidates AS (
                  SELECT ad.id
                  FROM agnostic_discovery ad
                  JOIN idp_core.entity e ON e.id = ad.id
                  WHERE e.template_identifier = :startTemplate
                  GROUP BY ad.id
                  HAVING COUNT(DISTINCT ad.group_id) = :expectedGroupCount
              )

              -- 4. PAGINATED OUTPUT: Return the clean sorted chunk slice
              SELECT id FROM matched_candidates
              ORDER BY id ASC
              LIMIT :size OFFSET :offset
            """, nativeQuery = true)
  List<UUID> findEntityGraphIdsAgnosticIntersect(@Param("rootIds") UUID[] rootIds,
      @Param("groupIds") String[] groupIds, @Param("expectedGroupCount") long expectedGroupCount,
      @Param("depth") int depth, @Param("startTemplate") String startTemplate,
      @Param("size") int size, @Param("offset") int offset);



}
