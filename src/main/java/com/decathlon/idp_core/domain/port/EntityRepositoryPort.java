package com.decathlon.idp_core.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityCompositeKey;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.search.PaginatedResult;
import com.decathlon.idp_core.domain.model.search.PaginationCriteria;
import com.decathlon.idp_core.domain.model.search.SearchFilterNode;

/// Driven port defining the contract for [Entity] persistence operations.
///
/// **Contract expectations for implementations:**
/// - `save()` must persist the entity and return the saved version with any generated IDs
/// - `findById()` must return empty Optional for non-existent entities
/// - `findByTemplateIdentifierAndIdentifier()` enforces business uniqueness constraints
/// - `findByTemplateIdentifier()` must support pagination for large entity sets
/// - `findByTemplateIdentifierWithFilter()` must apply all filter criteria with AND logic
/// - `findByIdentifierIn()` optimizes bulk entity lookups for relationship resolution
/// - `findByRelationIdIn()` enables reverse relationship navigation
/// - `deletePropertiesByTemplateIdentifierAndPropertyName()` must remove all property instances matching the given names for entities of the specified template
/// - `deleteRelationsByTemplateIdentifierAndRelationName()` must remove all relation instances matching the given names for entities of the specified template
/// - `search()` searches for entities across all templates using a nested filter tree and optional free-text query.
///
/// **Transaction behavior:** Implementations should handle transaction boundaries
/// appropriately for the underlying persistence technology.
public interface EntityRepositoryPort {

  Entity save(Entity entity);

  Optional<Entity> findById(UUID id);

  Optional<Entity> findByTemplateIdentifierAndIdentifier(String templateIdentifier,
      String identifier);

  Optional<Entity> findByTemplateIdentifierAndName(String templateIdentifier, String entityName);

  Page<Entity> findByTemplateIdentifier(String templateIdentifier, Pageable pageable);

  Page<Entity> findByTemplateIdentifierWithFilter(String templateIdentifier, EntityFilter filter,
      Pageable pageable);

  List<EntitySummary> findByIdentifierIn(List<String> identifiers);

  /// Finds entity summaries by composite keys (templateIdentifier, identifier).
  ///
  /// **Design:** Queries entities using both templateIdentifier and identifier
  /// to respect the database's composite uniqueness constraint. This prevents
  /// fetching wrong entities when identifiers are duplicated across templates.
  ///
  /// **Performance:** Implementations should optimize this query using an IN
  /// clause
  /// with OR conditions for each composite key pair.
  ///
  /// @param compositeKeys list of (templateIdentifier, identifier) pairs
  /// @return list of matching entity summaries
  List<EntitySummary> findSummariesByCompositeKeys(List<EntityCompositeKey> compositeKeys);

  List<EntitySummary> findByRelationIdIn(List<UUID> relationIds);

  void deletePropertiesByTemplateIdentifierAndPropertyName(String templateIdentifier,
      Collection<String> propertyNames);

  void deleteRelationsByTemplateIdentifierAndRelationName(String templateIdentifier,
      Collection<String> relationNames);

  PaginatedResult<Entity> search(SearchFilterNode filter, String query,
      PaginationCriteria paginationCriteria);

  List<Entity> findEntitiesRelated(String targetIdentifier);

  void deleteByTemplateIdentifierAndIdentifier(String templateIdentifier, String entityIdentifier);

}
