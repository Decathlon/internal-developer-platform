package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityCompositeKey;
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

import lombok.RequiredArgsConstructor;

/// Persistence adapter dedicated to entity relationship graph traversal.
///
/// Separated from [PostgresEntityAdapter] because graph queries use a distinct
/// recursive CTE strategy that has no overlap with standard CRUD operations,
/// following the Interface Segregation Principle.
///
/// **Query strategy:**
/// 1. One recursive CTE query to collect all (identifier, template_identifier) pairs in the graph.
/// 2. One batch query to load entities with their relations (avoids N+1).
/// 3. One batch query to load properties separately (avoids MultipleBagFetchException).
@Component
@RequiredArgsConstructor
public class PostgresEntityGraphAdapter implements EntityGraphRepositoryPort {

    private final JpaEntityRepository jpaEntityRepository;
    private final EntityPersistenceMapper mapper;

    @Override
    public Map<EntityCompositeKey, Entity> findEntityGraph(
            String templateIdentifier,
            String entityIdentifier,
            int depth) {
        // Step 1: collect all (identifier, template_identifier) pairs via recursive CTE
        List<Object[]> graphPairs = jpaEntityRepository.findEntityGraphIdentifiers(
                templateIdentifier, entityIdentifier, depth);

        if (graphPairs.isEmpty()) {
            return Map.of();
        }

        // Step 2: extract unique identifiers for batch loading
        List<String> identifiers = graphPairs.stream()
                .map(pair -> (String) pair[0])
                .distinct()
                .toList();

        // Step 3: batch-load entities with relations, then properties in separate queries
        // to avoid Hibernate's MultipleBagFetchException
        List<EntityJpaEntity> jpaEntities =
                jpaEntityRepository.findAllByIdentifierInWithRelations(identifiers);
        jpaEntityRepository.findAllByIdentifierInWithProperties(identifiers);

        // Step 4: map to domain and key by composite key for O(1) lookup
        return jpaEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toMap(
                        e -> new EntityCompositeKey(e.templateIdentifier(), e.identifier()),
                        Function.identity()
                ));
    }
}
