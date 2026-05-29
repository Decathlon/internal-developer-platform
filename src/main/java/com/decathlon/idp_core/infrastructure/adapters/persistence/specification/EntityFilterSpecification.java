package com.decathlon.idp_core.infrastructure.adapters.persistence.specification;

import java.util.stream.Stream;

import org.springframework.data.jpa.domain.Specification;

import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.FilterCriterion;
import com.decathlon.idp_core.domain.model.enums.FilterOperator;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/// Builds a JPA [Specification] for [EntityJpaEntity] from an [EntityFilter].
///
/// **Query strategy:**
/// - Attribute criteria use direct predicates on the entity root.
/// - Property criteria use an INNER JOIN on the `properties` collection.
/// - Relation name criteria filter entities that have a relation with a specific name.
/// - Relation entity criteria use an INNER JOIN on the `relations` collection and
///   then on the `targetEntityIdentifiers` element collection.
/// - Relation property criteria use an INNER JOIN on the `relations` collection and
///   filter on the specified property (e.g., `name`, `identifier`).
/// - Relations as target name criteria find entities where they are targets of relations
///   with a specific name (requires joining relations and checking targetEntityIdentifiers).
/// - Join-based criteria call `query.distinct(true)` to prevent duplicate rows from
/// - All criteria are combined with AND logic via [Specification#allOf].
///
/// **Security:** The CONTAINS operator escapes SQL LIKE wildcards (`%`, `_`) in
/// user-supplied values to prevent unintended pattern matching.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntityFilterSpecification {

    private static final String NAME = "name";
    private static final String IDENTIFIER = "identifier";
    private static final String RELATIONS = "relations";
    private static final String TARGET_ENTITY_IDENTIFIERS = "targetEntityIdentifiers";

    /// Builds a [Specification] that matches entities belonging to the given template identifier
    /// and satisfying all criteria in the given filter.
    ///
    /// @param templateIdentifier the template to scope the query to
    /// @param filter             the filter to apply; may be empty (no additional predicates)
    /// @return a composed [Specification] combining template scope and all filter criteria
    public static Specification<EntityJpaEntity> of(String templateIdentifier, EntityFilter filter) {
        var criteriaSpecs = filter.criteria().stream()
                .map(EntityFilterSpecification::fromCriterion);

        return Stream.concat(
                Stream.of(hasTemplateIdentifier(templateIdentifier)),
                criteriaSpecs
        ).reduce(Specification::and).orElse(hasTemplateIdentifier(templateIdentifier));
    }

    private static Specification<EntityJpaEntity> hasTemplateIdentifier(String templateIdentifier) {
        return (root, query, cb) -> cb.equal(root.get("templateIdentifier"), templateIdentifier);
    }

    private static Specification<EntityJpaEntity> fromCriterion(FilterCriterion criterion) {
        return switch (criterion.keyType()) {
            case ATTRIBUTE -> attributeSpec(criterion);
            case PROPERTY -> propertySpec(criterion);
            case RELATION_NAME -> relationNameSpec(criterion);
            case RELATION_ENTITY -> relationEntitySpec(criterion);
            case RELATION_PROPERTY -> relationPropertySpec(criterion);
            case RELATIONS_AS_TARGET_NAME -> relationsAsTargetNameSpec(criterion);
            case RELATIONS_AS_TARGET_PROPERTY -> relationsAsTargetPropertySpec(criterion);
        };
    }

    private static Specification<EntityJpaEntity> attributeSpec(FilterCriterion criterion) {
        return (root, query, cb) ->
                buildPredicate(cb, root.get(criterion.key()), criterion.operator(), criterion.value());
    }

    private static Specification<EntityJpaEntity> propertySpec(FilterCriterion criterion) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<EntityJpaEntity, PropertyJpaEntity> propJoin = root.join("properties");
            return cb.and(
                    cb.equal(propJoin.get(NAME), criterion.key()),
                    buildPredicate(cb, propJoin.get("value"), criterion.operator(), criterion.value())
            );
        };
    }

    private static Specification<EntityJpaEntity> relationEntitySpec(FilterCriterion criterion) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<EntityJpaEntity, RelationJpaEntity> relJoin = root.join(RELATIONS);
            Join<RelationJpaEntity, String> targetJoin = relJoin.join(TARGET_ENTITY_IDENTIFIERS);
            return cb.and(
                    cb.equal(relJoin.get(NAME), criterion.key()),
                    buildPredicate(cb, targetJoin, criterion.operator(), criterion.value())
            );
        };
    }

    private static Specification<EntityJpaEntity> relationPropertySpec(FilterCriterion criterion) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<EntityJpaEntity, RelationJpaEntity> relJoin = root.join(RELATIONS);

            String compositeKey = criterion.key();
            int dotIndex = compositeKey.indexOf('.');
            if (dotIndex < 0) {
                throw new IllegalArgumentException("Invalid composite key format: " + compositeKey);
            }
            String relationName = compositeKey.substring(0, dotIndex);
            String propertyName = compositeKey.substring(dotIndex + 1);

            // Check if the property is a target entity property (identifier, name)
            if (IDENTIFIER.equals(propertyName) || NAME.equals(propertyName)) {
                // Join to target entity identifiers first
                Join<RelationJpaEntity, String> targetIdJoin = relJoin.join(TARGET_ENTITY_IDENTIFIERS);
                // Create a subquery to find the actual target entities and filter by their properties
                var subquery = query.subquery(String.class);
                var subRoot = subquery.from(EntityJpaEntity.class);
                subquery.select(subRoot.get(IDENTIFIER))
                        .where(buildPredicate(cb, subRoot.get(propertyName), criterion.operator(), criterion.value()));

                return cb.and(
                        cb.equal(relJoin.get(NAME), relationName),
                        cb.in(targetIdJoin).value(subquery)
                );
            } else {
                // Direct relation property (shouldn't happen normally as RelationJpaEntity has limited properties)
                return cb.and(
                        cb.equal(relJoin.get(NAME), relationName),
                        buildPredicate(cb, relJoin.get(propertyName), criterion.operator(), criterion.value())
                );
            }
        };
    }

    private static Predicate buildPredicate(
            CriteriaBuilder cb,
            Expression<?> field,
            FilterOperator operator,
            String value) {
        return switch (operator) {
            case EQUALS -> JpaPredicateBuilder.buildPredicate(cb, field, SearchOperator.EQ, value);
            case CONTAINS -> JpaPredicateBuilder.buildPredicate(cb, field, SearchOperator.CONTAINS, value);
            // LESS_THAN / GREATER_THAN keep lexicographic string comparison (System A semantics).
            case LESS_THAN -> cb.lessThan(field.as(String.class), value);
            case GREATER_THAN -> cb.greaterThan(field.as(String.class), value);
        };
    }

    private static Specification<EntityJpaEntity> relationNameSpec(FilterCriterion criterion) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<EntityJpaEntity, RelationJpaEntity> relJoin = root.join(RELATIONS);
            return buildPredicate(cb, relJoin.get(NAME), criterion.operator(), criterion.value());
        };
    }

    private static Specification<EntityJpaEntity> relationsAsTargetNameSpec(FilterCriterion criterion) {
        return (root, query, cb) -> {
            // Find entities whose identifier appears as a target in any relation whose name matches.
            // Uses a correlated subquery to avoid joining through the entity's own outgoing relations.
            Subquery<String> subquery = query.subquery(String.class);
            Root<RelationJpaEntity> relRoot = subquery.from(RelationJpaEntity.class);
            Join<RelationJpaEntity, String> targetJoin = relRoot.join(TARGET_ENTITY_IDENTIFIERS);
            subquery.select(targetJoin)
                    .where(buildPredicate(cb, relRoot.get(NAME), criterion.operator(), criterion.value()));
            return cb.in(root.get(IDENTIFIER)).value(subquery);
        };
    }

    /// Finds entities whose `identifier` appears as a `targetEntityIdentifier` in any
    /// relation whose **source entity** property matches the criterion.
    ///
    /// Example: `relations_as_target.api-link.name:microservice` returns entities that
    /// are targeted by a `api-link` relation originating from an entity whose name
    /// contains "microservice".
    private static Specification<EntityJpaEntity> relationsAsTargetPropertySpec(FilterCriterion criterion) {
        return (root, query, cb) -> {
            String compositeKey = criterion.key();
            int dotIndex = compositeKey.indexOf('.');
            if (dotIndex < 0) {
                throw new IllegalArgumentException("Invalid composite key format: " + compositeKey);
            }
            String relationName = compositeKey.substring(0, dotIndex);
            String propertyName = compositeKey.substring(dotIndex + 1); // "identifier" or "name"

            // Subquery: collect all target identifiers from relations named <relationName>
            // that originate from source entities whose <propertyName> matches.
            Subquery<String> subquery = query.subquery(String.class);
            Root<EntityJpaEntity> sourceRoot = subquery.from(EntityJpaEntity.class);
            Join<EntityJpaEntity, RelationJpaEntity> relJoin = sourceRoot.join(RELATIONS);
            Join<RelationJpaEntity, String> targetJoin = relJoin.join(TARGET_ENTITY_IDENTIFIERS);
            subquery.select(targetJoin)
                    .where(
                            cb.equal(relJoin.get(NAME), relationName),
                            buildPredicate(cb, sourceRoot.get(propertyName), criterion.operator(), criterion.value())
                    );
            return cb.in(root.get(IDENTIFIER)).value(subquery);
        };
    }

}
