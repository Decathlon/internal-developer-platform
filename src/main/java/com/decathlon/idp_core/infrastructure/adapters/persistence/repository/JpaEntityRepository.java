package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;

@Repository
public interface JpaEntityRepository extends JpaRepository<EntityJpaEntity, UUID> {

    @Query("SELECT e.identifier AS identifier, e.name AS name, e.templateIdentifier AS templateIdentifier FROM EntityJpaEntity e WHERE e.identifier IN :identifiers")
    List<EntitySummary> findByIdentifierIn(List<String> identifiers);

    @Query("SELECT e.identifier AS identifier, e.name AS name, e.templateIdentifier AS templateIdentifier FROM EntityJpaEntity e JOIN e.relations r WHERE r.id IN :relationIds")
    List<EntitySummary> findByRelationIdIn(List<UUID> relationIds);

    Optional<EntityJpaEntity> findByTemplateIdentifierAndIdentifier(String templateIdentifier, String identifier);

    Page<EntityJpaEntity> findByTemplateIdentifier(String templateIdentifier, Pageable pageable);
}
