package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.LineageIdProjection;

public class JpaEntitySearchRepository {
  @PersistenceContext
  private EntityManager entityManager;

  @SuppressWarnings("unchecked")
  public List<LineageIdProjection> findEntityGraphWithLocalFilters(UUID[] rootIds,
      String[] groupIds, long expectedGroupCount, int depth, String startTemplate,
      String compiledLocalSqlSnippet, // Example value: "AND e.name LIKE 'payment-%' AND e.lifecycle
                                      // = 'PRODUCTION'"
      int size, int offset) {
    // Base static template with a text placeholder for your dynamic local
    // constraints
    String sqlTemplate = """
        WITH RECURSIVE
        input_criteria(id, group_id) AS (
            SELECT t.root_id, t.grp_id
            FROM UNNEST(CAST(:rootIds AS uuid[]), CAST(:groupIds AS text[])) AS t(root_id, grp_id)
        ),
        agnostic_discovery(id, group_id, flow, depth) AS (
            SELECT ic.id, ic.group_id, 'INBOUND' AS flow, 0
            FROM input_criteria ic
            UNION
            SELECT combined.id, ad.group_id, ad.flow, ad.depth + 1
            FROM agnostic_discovery ad
            JOIN (
                SELECT rte.target_entity_uuid AS anchor_id, er.entity_id AS id, 'INBOUND' AS flow_match
                FROM idp_core.relation_target_entities rte
                JOIN idp_core.entity_relations er ON er.relation_id = rte.relation_id
                UNION ALL
                SELECT er.entity_id AS anchor_id, rte.target_entity_uuid AS id, 'OUTBOUND' AS flow_match
                FROM idp_core.entity_relations er
                JOIN idp_core.relation_target_entities rte ON rte.relation_id = er.relation_id
                WHERE rte.target_entity_uuid IS NOT NULL
            ) combined ON combined.anchor_id = ad.id AND combined.flow_match = ad.flow
            WHERE ad.depth < :depth
        ),
        matched_candidates AS (
            SELECT ad.id
            FROM agnostic_discovery ad
            JOIN idp_core.entity e ON e.id = ad.id
            WHERE e.template_identifier = :startTemplate
            %s -- <--- THE DYNAMIC LOCAL SNIPPET INJECTION POINT!
            GROUP BY ad.id
            HAVING COUNT(DISTINCT ad.group_id) = :expectedGroupCount
        )
        SELECT
            id,
            COUNT(*) OVER() AS totalCount
        FROM matched_candidates
        ORDER BY id ASC
        LIMIT :size OFFSET :offset
        """;

    // Safely format the string template by inserting your compiled property snippet
    // clauses
    String finalSql = String.format(sqlTemplate,
        compiledLocalSqlSnippet != null ? compiledLocalSqlSnippet : "");

    Query query = entityManager.createNativeQuery(finalSql);

    // Bind the parameters securely
    query.setParameter("rootIds", rootIds);
    query.setParameter("groupIds", groupIds);
    query.setParameter("expectedGroupCount", expectedGroupCount);
    query.setParameter("depth", depth);
    query.setParameter("startTemplate", startTemplate);
    query.setParameter("size", size);
    query.setParameter("offset", offset);

    // Map the raw database object tuples back into your clean Java projection interface
    List<Object[]> rawResults = query.getResultList();
    return rawResults.stream().map(record -> new LineageIdProjection() {
      @Override
      public UUID getId() {
        return (UUID) record[0];
      }
      @Override
      public Long getTotalCount() {
        return ((Number) record[1]).longValue();
      }
    }).collect(Collectors.toList());
  }
}
