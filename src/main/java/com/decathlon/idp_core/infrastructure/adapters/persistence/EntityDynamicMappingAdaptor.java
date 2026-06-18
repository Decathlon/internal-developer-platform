package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityDynamicMappingPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_mapping.EntityDynamicMappingJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityDynamicMappingRepository;

import lombok.RequiredArgsConstructor;

/// Persistence adapter for [EntityDynamicMapping] read and write operations.
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

  @Override
  public boolean existsByIdentifier(String identifier) {
    return jpaEntityDynamicMappingRepository.existsByIdentifier(identifier);
  }

  @Override
  public Optional<EntityDynamicMapping> findByIdentifier(String identifier) {
    return jpaEntityDynamicMappingRepository.findByIdentifier(identifier)
        .map(entityDynamicMappingPersistenceMapper::toDomain);
  }

  @Override
  public EntityDynamicMapping save(EntityDynamicMapping entityDynamicMapping) {
    EntityDynamicMappingJpaEntity entityToPersist = entityDynamicMappingPersistenceMapper
        .toJpa(entityDynamicMapping);
    EntityDynamicMappingJpaEntity persistedEntity = jpaEntityDynamicMappingRepository
        .save(entityToPersist);
    return entityDynamicMappingPersistenceMapper.toDomain(persistedEntity);
  }

  @Override
  public Page<EntityDynamicMapping> findAll(Pageable pageable) {
    return jpaEntityDynamicMappingRepository.findAll(pageable)
        .map(entityDynamicMappingPersistenceMapper::toDomain);
  }

  @Override
  public void deleteByIdentifier(String identifier) {
    jpaEntityDynamicMappingRepository.deleteByIdentifier(identifier);
  }
}
