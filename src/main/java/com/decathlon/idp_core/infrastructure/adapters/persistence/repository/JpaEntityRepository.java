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
  List<EntityJpaEntity> findAllByIdentifierInWithRelations(@Param("ids") Collection<UUID> ids);

  /// Fetch properties for entities that were already loaded. This is called after
  /// findAllByIdentifierInWithRelations to complete the entity graph.
  @Query("SELECT DISTINCT e FROM EntityJpaEntity e LEFT JOIN FETCH e.properties WHERE e.id IN :identifiers")
  List<EntityJpaEntity> findAllByIdentifierInWithProperties(
      @Param("identifiers") Collection<UUID> ids);

  @Query(value = """
      WITH RECURSIVE entity_graph(id, depth) AS (
          -- 1. ANCHOR MEMBER: Start with the specific root entity UUID
          SELECT CAST(:entityId AS UUID), 0

          UNION -- Frontier propagation: automatically eliminates path duplicates at each step

          -- 2. RECURSIVE MEMBER: Scan indexed schema tables via direct binary matches
          SELECT neighbor.id, eg.depth + 1
          FROM entity_graph eg
          CROSS JOIN LATERAL (
              -- Track A: Outbound direction (this entity -> targets)
              SELECT rte.target_entity_uuid AS id
              FROM idp_core.entity_relations er
              JOIN idp_core.relation_target_entities rte ON rte.relation_id = er.relation_id
              WHERE er.entity_id = eg.id
                AND rte.target_entity_uuid IS NOT NULL

              UNION ALL

              -- Track B: Inbound direction (sources -> this entity as target)
              SELECT er.entity_id AS id
              FROM idp_core.relation_target_entities rte
              JOIN idp_core.entity_relations er ON er.relation_id = rte.relation_id
              WHERE rte.target_entity_uuid = eg.id
          ) neighbor
          -- Keeps the depth bounded entirely at the database layer
          WHERE eg.depth < :depth
      )
      -- 3. LEAN RETURN: Extract only the unique raw UUIDs discovered in the network skeleton
      SELECT DISTINCT id FROM entity_graph;
                                    """, nativeQuery = true)
  List<UUID> findEntityUuidsInGraph(@Param("entityId") UUID entityId, @Param("depth") int depth);

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
}
