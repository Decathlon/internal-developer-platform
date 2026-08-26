package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;
import com.decathlon.idp_core.domain.port.RelationRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaRelationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostgresRelationAdapter implements RelationRepositoryPort {

  private final JpaRelationRepository jpaRelationRepository;

  @Override
  public List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityUuids(
      List<UUID> targetEntityUuids) {

    if (targetEntityUuids == null || targetEntityUuids.isEmpty()) {
      return List.of();
    }

    return jpaRelationRepository.findRelationsSummariesByTargetEntityUuids(targetEntityUuids);
  }
}
