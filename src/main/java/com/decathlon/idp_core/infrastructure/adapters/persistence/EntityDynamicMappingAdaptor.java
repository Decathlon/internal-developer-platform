package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityDynamicMappingPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_dynamic_mapping.EntityDynamicMappingJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityDynamicMappingRepository;

import lombok.RequiredArgsConstructor;

/// Persistence adapter for [EntityDynamicMapping] read and write operations.
@Component
@RequiredArgsConstructor
public class EntityDynamicMappingAdaptor implements EntityDynamicMappingPort {
  private final JpaEntityDynamicMappingRepository jpaEntityDynamicMappingRepository;
  private final EntityDynamicMappingPersistenceMapper entityDynamicMappingPersistenceMapper;
  private final EntityTemplateRepositoryPort entityTemplateRepositoryPort;

  @Override
  public List<EntityDynamicMapping> findByEntityTemplateIdentifier(String identifier) {
    return jpaEntityDynamicMappingRepository.findByTemplateIdentifier(identifier).stream()
        .map(entityDynamicMappingPersistenceMapper::toDomain).toList();
  }

  @Override
  public List<EntityDynamicMapping> findByEntityTemplateId(UUID templateId) {
    return jpaEntityDynamicMappingRepository.findByTemplateId(templateId).stream()
        .map(entityDynamicMappingPersistenceMapper::toDomain).toList();
  }

  @Override
  public Boolean existsByEntityTemplateIdentifier(String templateIdentifier) {
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
    // The domain model references the entityTemplateIdentifier by its business
    // identifier, but the
    // foreign key persisted in `entity_dynamic_mapping.template_id` is the
    // entityTemplateIdentifier
    // UUID. Resolve the identifier to the entityTemplateIdentifier id before saving
    // (fail-fast).
    UUID templateId = entityTemplateRepositoryPort
        .findByIdentifier(entityDynamicMapping.entityTemplateIdentifier()).map(EntityTemplate::id)
        .orElseThrow(() -> new EntityTemplateNotFoundException("identifier",
            entityDynamicMapping.entityTemplateIdentifier()));

    EntityDynamicMappingJpaEntity entityToPersist = entityDynamicMappingPersistenceMapper
        .toJpa(entityDynamicMapping);
    entityToPersist.setEntityTemplateId(templateId);
    EntityDynamicMappingJpaEntity persistedEntity = jpaEntityDynamicMappingRepository
        .save(entityToPersist);

    return new EntityDynamicMapping(persistedEntity.getId(), entityDynamicMapping.identifier(),
        entityDynamicMapping.entityTemplateIdentifier(), entityDynamicMapping.filter(),
        entityDynamicMapping.name(), entityDynamicMapping.description(),
        entityDynamicMapping.entityIdentifier(), entityDynamicMapping.entityName(),
        entityDynamicMapping.properties(), entityDynamicMapping.relations());
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
