package com.decathlon.idp_core.infrastructure.adapters.persistence.specification;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
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

/// Builds a JPA [Specification] for [EntityJpaEntity] from a [SearchFilterNode] tree.
///
/// **Query strategy:**
/// - [SearchFilterNode.Group] nodes are translated recursively: AND → Specification::and,
///   OR → Specification::or.
/// - [SearchFilterNode.Criterion] nodes are translated based on the field prefix:
///   - `template` → direct predicate on templateIdentifier
///   - `identifier` / `name` → direct predicates on the entity root
///   - `property.{name}` → INNER JOIN on the `properties` collection
///   - `relation.{name}` / `relation.{name}.identifier|name` → JOIN on
///     `relations` with optional sub-query for target entity properties
///   - `relations_as_target.{name}.identifier|name` → correlated sub-query
///     that finds entities targeted by qualifying reverse relations
///
/// **Security:** LIKE-based operators ([SearchOperator#CONTAINS], [SearchOperator#NOT_CONTAINS],
/// [SearchOperator#STARTS_WITH], [SearchOperator#ENDS_WITH]) escape SQL wildcards (`%` and
/// `_`) in user-supplied values to prevent unintended pattern matching.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntitySearchSpecification {

    private static final char LIKE_ESCAPE_CHAR = '\\';
    private static final String TEMPLATE_IDENTIFIER = "templateIdentifier";
    private static final String IDENTIFIER = "identifier";
    private static final String NAME = "name";
    private static final String RELATION = "relation";
    private static final String RELATIONS = "relations";
    private static final String TARGET_ENTITY_IDENTIFIERS = "targetEntityIdentifiers";
    private static final String PROPERTY_PREFIX = "property.";
    private static final String RELATION_PREFIX = "relation.";
    private static final String RELATIONS_AS_TARGET_PREFIX = "relations_as_target.";

    /// Builds a [Specification] from the root [SearchFilterNode].
    ///
    /// @param filter the root of the search filter tree
    /// @return a composed [Specification] matching the filter tree
    public static Specification<EntityJpaEntity> of(SearchFilterNode filter) {
        return build(filter);
    }

    /// Builds a global free-text search [Specification] that matches entities whose
    /// `identifier`, `name`, `templateIdentifier`, or any property value contains the given string (case-insensitive).
    ///
    /// The four conditions are combined with OR so that a match on any field is sufficient.
    /// The "any property" branch uses a correlated EXISTS subquery to avoid row multiplication.
    ///
    /// @param query the search string; must be non-null and non-blank
    /// @return a [Specification] implementing the global text search
    public static Specification<EntityJpaEntity> globalTextSearch(String query) {
        String escaped = escapeLikeWildcards(query.toLowerCase());
        String pattern = "%" + escaped + "%";

        Specification<EntityJpaEntity> byIdentifier =
                (root, q, cb) -> cb.like(cb.lower(root.get(IDENTIFIER)), pattern, LIKE_ESCAPE_CHAR);

        Specification<EntityJpaEntity> byName =
                (root, q, cb) -> cb.like(cb.lower(root.get(NAME)), pattern, LIKE_ESCAPE_CHAR);

        Specification<EntityJpaEntity> byTemplate =
                (root, q, cb) -> cb.like(cb.lower(root.get(TEMPLATE_IDENTIFIER)), pattern, LIKE_ESCAPE_CHAR);

        Specification<EntityJpaEntity> byAnyProperty = (root, queryCtx, cb) -> {
            // Correlated EXISTS: does this entity have at least one property whose value matches?
            var sub = queryCtx.subquery(Integer.class);
            var subRoot = sub.from(EntityJpaEntity.class);
            var propJoin = subRoot.join("properties");
            sub.select(cb.literal(1))
                    .where(
                            cb.equal(subRoot.get("id"), root.get("id")),
                            cb.like(cb.lower(propJoin.get("value").as(String.class)), pattern, LIKE_ESCAPE_CHAR)
                    );
            return cb.exists(sub);
        };

        return byIdentifier.or(byName).or(byTemplate).or(byAnyProperty);
    }

    private static Specification<EntityJpaEntity> build(SearchFilterNode node) {
        return switch (node) {
            case SearchFilterNode.Group g -> buildGroup(g);
            case SearchFilterNode.Criterion c -> buildCriterion(c);
        };
    }

    private static Specification<EntityJpaEntity> buildGroup(SearchFilterNode.Group group) {
        var nodes = group.nodes();
        if (nodes.isEmpty()) {
            return (root, query, cb) -> cb.conjunction(); // empty group matches all
        }

        List<Specification<EntityJpaEntity>> specs = nodes.stream().map(EntitySearchSpecification::build).toList();

        return switch (group.connector()) {
            case AND -> specs.stream().reduce(Specification::and).orElseThrow();
            case OR -> specs.stream().reduce(Specification::or).orElseThrow();
        };
    }

    // --- Field-based criterion dispatch ---

    private static Specification<EntityJpaEntity> buildCriterion(SearchFilterNode.Criterion c) {
        var field = c.field();
        if ("template".equals(field)) {
            return (root, query, cb) -> buildPredicate(cb, root.get(TEMPLATE_IDENTIFIER), c.operation(), c.value());
        }
        if (IDENTIFIER.equals(field)) {
            return (root, query, cb) -> buildPredicate(cb, root.get(IDENTIFIER), c.operation(), c.value());
        }
        if (NAME.equals(field)) {
            return (root, query, cb) -> buildPredicate(cb, root.get(NAME), c.operation(), c.value());
        }
        if (field.startsWith(PROPERTY_PREFIX)) {
            return propertySpec(c, field.substring(PROPERTY_PREFIX.length()));
        }
        if (field.startsWith(RELATIONS_AS_TARGET_PREFIX)) {
            return relationsAsTargetSpec(c, field.substring(RELATIONS_AS_TARGET_PREFIX.length()));
        }
        if (RELATION.equals(field)) {
            return relationNameSpec(c);
        }
        if (field.startsWith(RELATION_PREFIX)) {
            return relationSpec(c, field.substring(RELATION_PREFIX.length()));
        }
        throw new IllegalArgumentException("Unknown search field: " + field);
    }

    // --- Property spec ---

    private static Specification<EntityJpaEntity> propertySpec(SearchFilterNode.Criterion c, String propertyName) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<EntityJpaEntity, PropertyJpaEntity> propJoin = root.join("properties");
            return cb.and(
                    cb.equal(propJoin.get(NAME), propertyName),
                    buildPredicate(cb, propJoin.get("value"), c.operation(), c.value())
            );
        };
    }

    // --- Relation specs ---

    private static Specification<EntityJpaEntity> relationNameSpec(SearchFilterNode.Criterion c) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<EntityJpaEntity, RelationJpaEntity> relJoin = root.join(RELATIONS);
            return buildPredicate(cb, relJoin.get(NAME), c.operation(), c.value());
        };
    }

    private static Specification<EntityJpaEntity> relationSpec(SearchFilterNode.Criterion c, String relationPart) {
        int dotIndex = relationPart.indexOf('.');
        if (dotIndex > 0) {
            // relation.{name}.{identifier|name} → filter by target entity property with a subquery
            String relationName = relationPart.substring(0, dotIndex);
            String property = relationPart.substring(dotIndex + 1);
            return relationPropertySpec(c, relationName, property);
        }
        // relation.{name} → filter by target entity identifier
        return relationEntitySpec(c, relationPart);
    }

    private static Specification<EntityJpaEntity> relationEntitySpec(SearchFilterNode.Criterion c, String relationName) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<EntityJpaEntity, RelationJpaEntity> relJoin = root.join(RELATIONS);
            Join<RelationJpaEntity, String> targetJoin = relJoin.join(TARGET_ENTITY_IDENTIFIERS);
            return cb.and(
                    cb.equal(relJoin.get(NAME), relationName),
                    buildPredicate(cb, targetJoin, c.operation(), c.value())
            );
        };
    }

    private static Specification<EntityJpaEntity> relationPropertySpec(
            SearchFilterNode.Criterion c, String relationName, String property) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<EntityJpaEntity, RelationJpaEntity> relJoin = root.join(RELATIONS);
            Join<RelationJpaEntity, String> targetIdJoin = relJoin.join(TARGET_ENTITY_IDENTIFIERS);

            // Subquery: find target entity identifiers whose identifier/name matches
            var subquery = query.subquery(String.class);
            var subRoot = subquery.from(EntityJpaEntity.class);
            subquery.select(subRoot.get(IDENTIFIER))
                    .where(buildPredicate(cb, subRoot.get(property), c.operation(), c.value()));

            return cb.and(
                    cb.equal(relJoin.get(NAME), relationName),
                    cb.in(targetIdJoin).value(subquery)
            );
        };
    }

    // --- Relations-as-target specs ---

    private static Specification<EntityJpaEntity> relationsAsTargetSpec(
            SearchFilterNode.Criterion c, String relPart) {
        int dotIndex = relPart.indexOf('.');
        if (dotIndex <= 0) {
            throw new IllegalArgumentException(
                    "Invalid field 'relations_as_target." + relPart
                            + "': expected form relations_as_target.{relationName}.{identifier|name}");
        }
        String relationName = relPart.substring(0, dotIndex);
        String property = relPart.substring(dotIndex + 1); // identifier or name

        return (root, query, cb) -> {
            // Subquery: collect target identifiers from relations named <relationName>
            // whose source entity's <property> matches the criterion.
            Subquery<String> subquery = query.subquery(String.class);
            Root<EntityJpaEntity> sourceRoot = subquery.from(EntityJpaEntity.class);
            Join<EntityJpaEntity, RelationJpaEntity> relJoin = sourceRoot.join(RELATIONS);
            Join<RelationJpaEntity, String> targetJoin = relJoin.join(TARGET_ENTITY_IDENTIFIERS);
            subquery.select(targetJoin)
                    .where(
                            cb.equal(relJoin.get(NAME), relationName),
                            buildPredicate(cb, sourceRoot.get(property), c.operation(), c.value())
                    );
            return cb.in(root.get(IDENTIFIER)).value(subquery);
        };
    }

    // --- Predicate builder ---

    private static Predicate buildPredicate(
            CriteriaBuilder cb,
            Expression<?> field,
            SearchOperator operator,
            String value) {
        if (isNumericOperator(operator)) {
            return buildNumericPredicate(cb, field, operator, new BigDecimal(value));
        }
        Expression<String> stringField = field.as(String.class);
        return switch (operator) {
            case EQ -> cb.equal(cb.lower(stringField), value.toLowerCase());
            case NEQ -> cb.notEqual(cb.lower(stringField), value.toLowerCase());
            case CONTAINS -> {
                String escaped = escapeLikeWildcards(value.toLowerCase());
                yield cb.like(cb.lower(stringField), "%" + escaped + "%", LIKE_ESCAPE_CHAR);
            }
            case NOT_CONTAINS -> {
                String escaped = escapeLikeWildcards(value.toLowerCase());
                yield cb.notLike(cb.lower(stringField), "%" + escaped + "%", LIKE_ESCAPE_CHAR);
            }
            case STARTS_WITH -> {
                String escaped = escapeLikeWildcards(value.toLowerCase());
                yield cb.like(cb.lower(stringField), escaped + "%", LIKE_ESCAPE_CHAR);
            }
            case ENDS_WITH -> {
                String escaped = escapeLikeWildcards(value.toLowerCase());
                yield cb.like(cb.lower(stringField), "%" + escaped, LIKE_ESCAPE_CHAR);
            }
            default -> throw new IllegalStateException("Unhandled operator: " + operator);
        };
    }

    private static boolean isNumericOperator(SearchOperator operator) {
        return switch (operator) {
            case GT, GTE, LT, LTE -> true;
            default -> false;
        };
    }

    private static Predicate buildNumericPredicate(
            CriteriaBuilder cb,
            Expression<?> field,
            SearchOperator operator,
            BigDecimal numericValue) {
        // Use HibernateCriteriaBuilder.cast() to generate an explicit SQL CAST(field AS NUMERIC).
        // The property value column is VARCHAR; without an explicit cast PostgreSQL would reject
        // the comparison with a numeric literal.
        Expression<BigDecimal> numericField =
                ((HibernateCriteriaBuilder) cb).cast(
                        (org.hibernate.query.criteria.JpaExpression<?>) field, BigDecimal.class);
        return switch (operator) {
            case GT -> cb.greaterThan(numericField, numericValue);
            case GTE -> cb.greaterThanOrEqualTo(numericField, numericValue);
            case LT -> cb.lessThan(numericField, numericValue);
            case LTE -> cb.lessThanOrEqualTo(numericField, numericValue);
            default -> throw new IllegalStateException("Not a numeric operator: " + operator);
        };
    }

    /// Escapes SQL LIKE wildcards (`%` and `_`) in the given value so they are
    /// treated as literal characters rather than pattern metacharacters.
    static String escapeLikeWildcards(String value) {
        return value
                .replace(String.valueOf(LIKE_ESCAPE_CHAR), LIKE_ESCAPE_CHAR + String.valueOf(LIKE_ESCAPE_CHAR))
                .replace("%", LIKE_ESCAPE_CHAR + "%")
                .replace("_", LIKE_ESCAPE_CHAR + "_");
    }

}
