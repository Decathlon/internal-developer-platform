package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityDynamicMappingPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityDynamicMappingRepository;

import lombok.RequiredArgsConstructor;

/// Persistence adapter for [EntityDynamicMapping] read operations.
@Component
@RequiredArgsConstructor
public class EntityDynamicMappingAdaptor implements EntityDynamicMappingPort {
  private final JpaEntityDynamicMappingRepository jpaEntityDynamicMappingRepository;
  private final EntityDynamicMappingPersistenceMapper entityDynamicMappingPersistenceMapper;

  @Override
  public List<EntityDynamicMapping> findByTemplateIdentifier(String identifier) {
    return jpaEntityDynamicMappingRepository.findByTemplateIdentifier(identifier).stream()
        .map(entityDynamicMappingPersistenceMapper::toDomain).toList();
  }

  @Override
  public Boolean existsByTemplateIdentifier(String templateIdentifier) {
    return jpaEntityDynamicMappingRepository.existsByTemplateIdentifier(templateIdentifier);
  }
}
