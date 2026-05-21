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
public interface JpaEntityRepository extends JpaRepository<EntityJpaEntity, UUID>, JpaSpecificationExecutor<EntityJpaEntity> {

    @Query("SELECT e.identifier AS identifier, e.name AS name, e.templateIdentifier AS templateIdentifier FROM EntityJpaEntity e WHERE e.identifier IN :identifiers")
    List<EntitySummary> findByIdentifierIn(List<String> identifiers);

    @Query("SELECT e.identifier AS identifier, e.name AS name, e.templateIdentifier AS templateIdentifier FROM EntityJpaEntity e JOIN e.relations r WHERE r.id IN :relationIds")
    List<EntitySummary> findByRelationIdIn(List<UUID> relationIds);

    Optional<EntityJpaEntity> findByTemplateIdentifierAndIdentifier(String templateIdentifier, String identifier);

    Optional<EntityJpaEntity> findByIdentifier(String identifier);

    Optional<EntityJpaEntity> findByTemplateIdentifierAndName(String templateIdentifier, String name);

    Page<EntityJpaEntity> findByTemplateIdentifier(String templateIdentifier, Pageable pageable);

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

    /// Batch fetch entities by identifiers with eager loading of relations and properties.
    /// Uses two separate queries to avoid Hibernate's MultipleBagFetchException.
    /// First fetches entities with relations, then fetches properties separately.
    @Query("SELECT DISTINCT e FROM EntityJpaEntity e LEFT JOIN FETCH e.relations WHERE e.identifier IN :identifiers")
    List<EntityJpaEntity> findAllByIdentifierInWithRelations(@Param("identifiers") Collection<String> identifiers);

    /// Fetch properties for entities that were already loaded.
    /// This is called after findAllByIdentifierInWithRelations to complete the entity graph.
    @Query("SELECT DISTINCT e FROM EntityJpaEntity e LEFT JOIN FETCH e.properties WHERE e.identifier IN :identifiers")
    List<EntityJpaEntity> findAllByIdentifierInWithProperties(@Param("identifiers") Collection<String> identifiers);

    @Query(value = """
            WITH RECURSIVE
            -- Traverse outbound relations (this entity -> targets)
            outbound_graph(identifier, template_identifier, depth) AS (
                SELECT e.identifier, e.template_identifier, 0
                FROM entity e
                WHERE e.identifier = :entityIdentifier
                  AND e.template_identifier = :templateIdentifier

                UNION ALL

                SELECT e2.identifier, e2.template_identifier, og.depth + 1
                FROM outbound_graph og
                JOIN entity e ON e.identifier = og.identifier AND e.template_identifier = og.template_identifier
                JOIN entity_relations er ON er.entity_id = e.id
                JOIN relation r ON r.id = er.relation_id
                JOIN relation_target_entities rte ON rte.relation_id = r.id
                JOIN entity e2 ON e2.identifier = rte.target_entity_identifier
                WHERE og.depth < :depth
            ),
            -- Traverse inbound relations (sources -> this entity as target)
            inbound_graph(identifier, template_identifier, depth) AS (
                SELECT e.identifier, e.template_identifier, 0
                FROM entity e
                WHERE e.identifier = :entityIdentifier
                  AND e.template_identifier = :templateIdentifier

                UNION ALL

                SELECT e2.identifier, e2.template_identifier, ig.depth + 1
                FROM inbound_graph ig
                JOIN entity e ON e.identifier = ig.identifier AND e.template_identifier = ig.template_identifier
                JOIN relation_target_entities rte ON rte.target_entity_identifier = e.identifier
                JOIN relation r ON r.id = rte.relation_id
                JOIN entity_relations er ON er.relation_id = r.id
                JOIN entity e2 ON e2.id = er.entity_id
                WHERE ig.depth < :depth
            )
            SELECT DISTINCT identifier, template_identifier FROM outbound_graph
            UNION
            SELECT DISTINCT identifier, template_identifier FROM inbound_graph
            """, nativeQuery = true)
    List<Object[]> findEntityGraphIdentifiers(
        @Param("templateIdentifier") String templateIdentifier,
        @Param("entityIdentifier") String entityIdentifier,
        @Param("depth") int depth);

    /// Variant of [findEntityGraphIdentifiers] that restricts traversal to the given relation names.
    /// When the list is empty, all relation names are followed (no filter).
    /// The filter is applied inside both the outbound and inbound recursive CTE steps so that only
    /// entities reachable through the specified relations are returned, keeping the result set lean.
    @Query(value = """
            WITH RECURSIVE
            outbound_graph(identifier, template_identifier, depth) AS (
                SELECT e.identifier, e.template_identifier, 0
                FROM entity e
                WHERE e.identifier = :entityIdentifier
                  AND e.template_identifier = :templateIdentifier

                UNION ALL

                SELECT e2.identifier, e2.template_identifier, og.depth + 1
                FROM outbound_graph og
                JOIN entity e ON e.identifier = og.identifier AND e.template_identifier = og.template_identifier
                JOIN entity_relations er ON er.entity_id = e.id
                JOIN relation r ON r.id = er.relation_id
                JOIN relation_target_entities rte ON rte.relation_id = r.id
                JOIN entity e2 ON e2.identifier = rte.target_entity_identifier
                WHERE og.depth < :depth
                  AND r.name IN :relationNames
            ),
            inbound_graph(identifier, template_identifier, depth) AS (
                SELECT e.identifier, e.template_identifier, 0
                FROM entity e
                WHERE e.identifier = :entityIdentifier
                  AND e.template_identifier = :templateIdentifier

                UNION ALL

                SELECT e2.identifier, e2.template_identifier, ig.depth + 1
                FROM inbound_graph ig
                JOIN entity e ON e.identifier = ig.identifier AND e.template_identifier = ig.template_identifier
                JOIN relation_target_entities rte ON rte.target_entity_identifier = e.identifier
                JOIN relation r ON r.id = rte.relation_id
                JOIN entity_relations er ON er.relation_id = r.id
                JOIN entity e2 ON e2.id = er.entity_id
                WHERE ig.depth < :depth
                  AND r.name IN :relationNames
            )
            SELECT DISTINCT identifier, template_identifier FROM outbound_graph
            UNION
            SELECT DISTINCT identifier, template_identifier FROM inbound_graph
            """, nativeQuery = true)
    List<Object[]> findEntityGraphIdentifiersFilteredByRelations(
        @Param("templateIdentifier") String templateIdentifier,
        @Param("entityIdentifier") String entityIdentifier,
        @Param("depth") int depth,
        @Param("relationNames") Collection<String> relationNames);
}
