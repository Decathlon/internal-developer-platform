package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

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
    public Optional<Entity> findByTemplateIdentifierAndIdentifier(String templateIdentifier, String identifier) {
        return jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier, identifier)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Entity> findByTemplateIdentifierAndName(String templateIdentifier, String entityName) {
        return jpaEntityRepository.findByTemplateIdentifierAndName(templateIdentifier, entityName)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Entity> findByTemplateIdentifier(String templateIdentifier, Pageable pageable) {
        return jpaEntityRepository.findByTemplateIdentifier(templateIdentifier, pageable).map(mapper::toDomain);
    }

    @Override
    public List<EntitySummary> findByIdentifierIn(List<String> identifiers) {
        return jpaEntityRepository.findByIdentifierIn(identifiers);
    }

    @Override
    public List<EntitySummary> findByRelationIdIn(List<UUID> relationIds) {
        return jpaEntityRepository.findByRelationIdIn(relationIds);
    }
}
