package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_mapping.EntityDynamicMappingJpaEntity;

/// JPA repository for EntityDynamicMapping persistence.
///
/// Manages the `entity_dynamic_mapping` table which stores mapping configurations
/// (JSLT filters, property/relation mappings) used by webhook template mappings.
@Repository
public interface JpaEntityDynamicMappingRepository
    extends
      JpaRepository<EntityDynamicMappingJpaEntity, UUID> {
  List<EntityDynamicMappingJpaEntity> findByTemplateIdentifier(String templateIdentifier);
  Boolean existsByTemplateIdentifier(String templateIdentifier);
}
