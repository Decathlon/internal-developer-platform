package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;
import com.decathlon.idp_core.infrastructure.adapters.persistence.specification.EntitySpecification;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostgresEntityAdapter implements EntityRepositoryPort {

  private final JpaEntityRepository jpaEntityRepository;
  private final EntityPersistenceMapper mapper;

  @Override
  public Entity save(Entity entity) {
    return mapper.toDomain(jpaEntityRepository.save(mapper.toJpa(entity)));
  }

  @Override
  public Optional<Entity> findById(UUID id) {
    return jpaEntityRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Entity> findByTemplateIdentifierAndIdentifier(String templateIdentifier,
      String identifier) {
    return jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier, identifier)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<Entity> findByTemplateIdentifierAndName(String templateIdentifier,
      String entityName) {
    return jpaEntityRepository.findByTemplateIdentifierAndName(templateIdentifier, entityName)
        .map(mapper::toDomain);
  }

  @Override
  public Page<Entity> findByTemplateIdentifier(String templateIdentifier, Pageable pageable) {
    var pageableEntity = jpaEntityRepository.findByTemplateIdentifier(templateIdentifier, pageable);
    return pageableEntity.map(mapper::toDomain);
  }

  @Override
  public Page<Entity> findByTemplateIdentifierWithFilter(String templateIdentifier,
      EntityFilter filter, Pageable pageable) {
    Specification<EntityJpaEntity> spec = EntitySpecification.of(templateIdentifier, filter);
    return jpaEntityRepository.findAll(spec, pageable).map(mapper::toDomain);
  }

  @Override
  public List<EntitySummary> findByIdentifierIn(List<String> identifiers) {
    return jpaEntityRepository.findByIdentifierIn(identifiers);
  }

  @Override
  public List<EntitySummary> findByRelationIdIn(List<UUID> relationIds) {
    return jpaEntityRepository.findByRelationIdIn(relationIds);
  }

  @Override
  public void deletePropertiesByTemplateIdentifierAndPropertyName(String templateIdentifier,
      Collection<String> propertyNames) {
    jpaEntityRepository.deletePropertiesByTemplateIdentifierAndPropertyName(templateIdentifier,
        propertyNames);
  }

  @Override
  public void deleteRelationsByTemplateIdentifierAndRelationName(String templateIdentifier,
      Collection<String> relationNames) {
    jpaEntityRepository.deleteRelationsByTemplateIdentifierAndRelationName(templateIdentifier,
        relationNames);
  }

  @Override
  public List<Entity> findEntitiesRelated(String targetIdentifier) {
    return jpaEntityRepository.findEntitiesRelated(targetIdentifier).stream().map(mapper::toDomain)
        .toList();
  }

  @Override
  public void deleteByTemplateIdentifierAndIdentifier(final String templateIdentifier,
      final String entityIdentifier) {
    jpaEntityRepository.deleteByTemplateIdentifierAndIdentifier(templateIdentifier,
        entityIdentifier);
  }
}
