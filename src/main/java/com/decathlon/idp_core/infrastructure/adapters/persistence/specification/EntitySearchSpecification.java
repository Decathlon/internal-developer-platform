package com.decathlon.idp_core.infrastructure.adapters.persistence.specification;

import static com.decathlon.idp_core.infrastructure.adapters.persistence.specification.JpaPredicateBuilder.buildPredicate;
import static com.decathlon.idp_core.infrastructure.adapters.persistence.specification.JpaPredicateBuilder.escapeLikeWildcards;

import java.util.List;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

import com.decathlon.idp_core.domain.model.search.SearchFilterNode;
import com.decathlon.idp_core.domain.model.search.SearchOperator;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationTargetJpaEntity;

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
///   - `property.{name}` → correlated EXISTS subquery on the `properties` collection
///   - `relation.{name}` / `relation.{name}.identifier|name` → correlated EXISTS subquery
///     on `relations` with optional nested IN subquery for target entity properties
///   - `relations_as_target.{name}.identifier|name` → correlated IN subquery
///     that finds entities targeted by qualifying reverse relations
///
/// **Performance:** All collection-based filters use EXISTS subqueries instead of JOINs.
/// This eliminates row multiplication (an entity with N properties and M relations would
/// otherwise produce N×M rows requiring DISTINCT), making pagination and count queries
/// significantly cheaper.
///
/// **Security:** LIKE-based operators ([SearchOperator#CONTAINS], [SearchOperator#NOT_CONTAINS],
/// [SearchOperator#STARTS_WITH], [SearchOperator#ENDS_WITH]) use PostgreSQL `ILIKE` for
/// case-insensitive matching, allowing GIN trigram indexes to be leveraged.
/// SQL wildcards (`%` and `_`) in user-supplied values are escaped to prevent unintended
/// pattern matching. EQ and NEQ use `LOWER()` with functional btree indexes.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EntitySearchSpecification {

  private static final String TEMPLATE_IDENTIFIER = "templateIdentifier";
  private static final String IDENTIFIER = "identifier";
  private static final String TEMPLATE = "template";
  private static final String NAME = "name";
  private static final String RELATION = "relation";
  private static final String RELATIONS = "relations";
  private static final String RELATIONS_AS_TARGET = "relations_as_target";
  private static final String TARGET_ENTITIES = "targetEntities";
  private static final String TARGET_ENTITY_IDENTIFIER = "targetEntityIdentifier";
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
  /// `identifier`, `name`, `templateIdentifier`, or any property value contains
  /// the given string (case-insensitive).
  ///
  /// The four conditions are combined with OR so that a match on any field is
  /// sufficient.
  /// The "any property" branch uses a correlated EXISTS subquery to avoid row
  /// multiplication.
  /// All comparisons use `ILIKE` so that GIN trigram indexes (V3_5) can be
  /// leveraged.
  ///
  /// @param query the search string; must be non-null and non-blank
  /// @return a [Specification] implementing the global text search
  public static Specification<EntityJpaEntity> globalTextSearch(String query) {
    // No toLowerCase() needed — ILIKE is inherently case-insensitive.
    String escaped = escapeLikeWildcards(query);
    String pattern = "%" + escaped + "%";

    Specification<EntityJpaEntity> byIdentifier = (root, q, cb) -> ((HibernateCriteriaBuilder) cb)
        .ilike(root.get(IDENTIFIER), pattern, JpaPredicateBuilder.LIKE_ESCAPE_CHAR);

    Specification<EntityJpaEntity> byName = (root, q, cb) -> ((HibernateCriteriaBuilder) cb)
        .ilike(root.get(NAME), pattern, JpaPredicateBuilder.LIKE_ESCAPE_CHAR);

    Specification<EntityJpaEntity> byTemplate = (root, q, cb) -> ((HibernateCriteriaBuilder) cb)
        .ilike(root.get(TEMPLATE_IDENTIFIER), pattern, JpaPredicateBuilder.LIKE_ESCAPE_CHAR);

    Specification<EntityJpaEntity> byAnyProperty = (root, queryCtx, cb) -> {
      // Correlated EXISTS: does this entity have at least one property whose value
      // matches?
      var sub = queryCtx.subquery(Integer.class);
      var subRoot = sub.from(EntityJpaEntity.class);
      var propJoin = subRoot.join("properties");
      sub.select(cb.literal(1)).where(cb.equal(subRoot.get("id"), root.get("id")),
          ((HibernateCriteriaBuilder) cb).ilike(propJoin.get("value").as(String.class), pattern,
              JpaPredicateBuilder.LIKE_ESCAPE_CHAR));
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

    List<Specification<EntityJpaEntity>> specs = nodes.stream()
        .map(EntitySearchSpecification::build).toList();

    return switch (group.connector()) {
      case AND -> specs.stream().reduce(Specification::and).orElseThrow();
      case OR -> specs.stream().reduce(Specification::or).orElseThrow();
    };
  }

  // --- Field-based criterion dispatch ---

  private static Specification<EntityJpaEntity> buildCriterion(SearchFilterNode.Criterion c) {
    var field = c.field();
    return switch (field) {
      case TEMPLATE -> (root, query, cb) -> buildPredicate(cb, root.get(TEMPLATE_IDENTIFIER), c.operation(), c.value());
      case String f when f.equals(IDENTIFIER) -> (root, query, cb) -> buildPredicate(cb, root.get(IDENTIFIER), c.operation(), c.value());
      case String f when f.equals(NAME) -> (root, query, cb) -> buildPredicate(cb, root.get(NAME), c.operation(), c.value());
      case String f when f.startsWith(PROPERTY_PREFIX) -> propertySpec(c, f.substring(PROPERTY_PREFIX.length()));
      case String f when f.startsWith(RELATIONS_AS_TARGET_PREFIX) -> relationsAsTargetSpec(c, f.substring(RELATIONS_AS_TARGET_PREFIX.length()));
      case String f when f.equals(RELATIONS_AS_TARGET) -> relationsAsTargetNameSpec(c);
      case String f when f.equals(RELATION) -> relationNameSpec(c);
      case String f when f.startsWith(RELATION_PREFIX) -> relationSpec(c, f.substring(RELATION_PREFIX.length()));
      default -> throw new IllegalArgumentException("Unknown search field: " + field);
    };
  }

  // --- Property spec ---

  private static Specification<EntityJpaEntity> propertySpec(SearchFilterNode.Criterion c,
      String propertyName) {
    return (root, query, cb) -> {
      // Correlated EXISTS: does this entity have a property with the given name and
      // value?
      // Using EXISTS instead of JOIN avoids row multiplication and removes the need
      // for DISTINCT.
      var sub = query.subquery(Integer.class);
      var subRoot = sub.from(EntityJpaEntity.class);
      var propJoin = subRoot.join("properties");
      sub.select(cb.literal(1)).where(cb.equal(subRoot.get("id"), root.get("id")),
          cb.equal(propJoin.get(NAME), propertyName),
          buildPredicate(cb, propJoin.get("value"), c.operation(), c.value()));
      return cb.exists(sub);
    };
  }

  // --- Relation specs ---

  private static Specification<EntityJpaEntity> relationNameSpec(SearchFilterNode.Criterion c) {
    return (root, query, cb) -> {
      // Correlated EXISTS: does this entity have at least one relation whose name
      // matches?
      var sub = query.subquery(Integer.class);
      var subRoot = sub.from(EntityJpaEntity.class);
      var relJoin = subRoot.join(RELATIONS);
      sub.select(cb.literal(1)).where(cb.equal(subRoot.get("id"), root.get("id")),
          buildPredicate(cb, relJoin.get(NAME), c.operation(), c.value()));
      return cb.exists(sub);
    };
  }

  private static Specification<EntityJpaEntity> relationSpec(SearchFilterNode.Criterion c,
      String relationPart) {
    int dotIndex = relationPart.indexOf('.');
    if (dotIndex > 0) {
      // relation.{name}.{identifier|name} → filter by target entity property with a
      // subquery
      String relationName = relationPart.substring(0, dotIndex);
      String property = relationPart.substring(dotIndex + 1);
      return relationPropertySpec(c, relationName, property);
    }
    // relation.{name} → filter by target entity identifier
    return relationEntitySpec(c, relationPart);
  }

  private static Specification<EntityJpaEntity> relationEntitySpec(SearchFilterNode.Criterion c,
      String relationName) {
    return (root, query, cb) -> {
      // Correlated EXISTS: does this entity have a relation named <relationName>
      // whose target entity identifier matches the criterion?
      var sub = query.subquery(Integer.class);
      var subRoot = sub.from(EntityJpaEntity.class);
      var relJoin = subRoot.join(RELATIONS);
      Join<RelationJpaEntity, RelationTargetJpaEntity> targetJoin = relJoin.join(TARGET_ENTITIES);
      sub.select(cb.literal(1)).where(cb.equal(subRoot.get("id"), root.get("id")),
          cb.equal(relJoin.get(NAME), relationName),
          buildPredicate(cb, targetJoin.get(TARGET_ENTITY_IDENTIFIER), c.operation(), c.value()));
      return cb.exists(sub);
    };
  }

  private static Specification<EntityJpaEntity> relationPropertySpec(SearchFilterNode.Criterion c,
      String relationName, String property) {
    return (root, query, cb) -> {
      // Correlated EXISTS: does this entity have a relation named <relationName>
      // whose target identifier appears in the set of entity identifiers
      // whose <property> matches the criterion?
      var sub = query.subquery(Integer.class);
      var subRoot = sub.from(EntityJpaEntity.class);
      var relJoin = subRoot.join(RELATIONS);
      Join<RelationJpaEntity, RelationTargetJpaEntity> targetJoin = relJoin.join(TARGET_ENTITIES);

      // Inner scalar subquery: entity identifiers whose identifier/name satisfies the
      // criterion.
      var innerSubquery = query.subquery(String.class);
      var innerRoot = innerSubquery.from(EntityJpaEntity.class);
      innerSubquery.select(innerRoot.get(IDENTIFIER))
          .where(buildPredicate(cb, innerRoot.get(property), c.operation(), c.value()));

      sub.select(cb.literal(1)).where(cb.equal(subRoot.get("id"), root.get("id")),
          cb.equal(relJoin.get(NAME), relationName),
          cb.in(targetJoin.get(TARGET_ENTITY_IDENTIFIER)).value(innerSubquery));
      return cb.exists(sub);
    };
  }

  // --- Relations-as-target specs ---

  private static Specification<EntityJpaEntity> relationsAsTargetNameSpec(
      SearchFilterNode.Criterion c) {
    return (root, query, cb) -> {
      // Subquery: collect all target entity identifiers from relations whose name
      // matches.
      // For NOT_CONTAINS / NEQ (negative operators): use NOT IN with the positive
      // equivalent
      // predicate so that the result means "not targeted by any matching reverse
      // relation",
      // which is the natural set-membership interpretation of "does not contain".
      SearchOperator effectiveOp = switch (c.operation()) {
        case NOT_CONTAINS -> SearchOperator.CONTAINS;
        case NEQ -> SearchOperator.EQ;
        default -> c.operation();
      };

      Subquery<String> subquery = query.subquery(String.class);
      Root<EntityJpaEntity> sourceRoot = subquery.from(EntityJpaEntity.class);
      Join<EntityJpaEntity, RelationJpaEntity> relJoin = sourceRoot.join(RELATIONS);
      Join<RelationJpaEntity, RelationTargetJpaEntity> targetJoin = relJoin.join(TARGET_ENTITIES);
      subquery.select(targetJoin.get(TARGET_ENTITY_IDENTIFIER))
          .where(buildPredicate(cb, relJoin.get(NAME), effectiveOp, c.value()));

      boolean isNegated = c.operation() == SearchOperator.NOT_CONTAINS
          || c.operation() == SearchOperator.NEQ;
      var membership = cb.in(root.get(IDENTIFIER)).value(subquery);
      return isNegated ? cb.not(membership) : membership;
    };
  }

  private static Specification<EntityJpaEntity> relationsAsTargetSpec(SearchFilterNode.Criterion c,
      String relPart) {
    int dotIndex = relPart.indexOf('.');
    if (dotIndex <= 0) {
      throw new IllegalArgumentException("Invalid field 'relations_as_target." + relPart
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
      Join<RelationJpaEntity, RelationTargetJpaEntity> targetJoin = relJoin.join(TARGET_ENTITIES);
      subquery.select(targetJoin.get(TARGET_ENTITY_IDENTIFIER)).where(
          cb.equal(relJoin.get(NAME), relationName),
          buildPredicate(cb, sourceRoot.get(property), c.operation(), c.value()));
      return cb.in(root.get(IDENTIFIER)).value(subquery);
    };
  }

}
