package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;

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
    public List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityIdentifiers(
            List<String> targetEntityIdentifiers) {
        return jpaRelationRepository.findRelationsSummariesByTargetEntityIdentifiers(targetEntityIdentifiers);
    }
}
