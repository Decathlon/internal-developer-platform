package com.decathlon.idp_core.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;
import com.decathlon.idp_core.domain.repository.RelationRepository;

import lombok.AllArgsConstructor;

/**
 * Service for managing relations between entities, including queries for relation ownerships by target entity identifiers.
 * <p>
 * This service provides methods to retrieve summaries of relations where a given entity (or entities) is the target.
 * </p>
 */
@Service
@AllArgsConstructor
public class RelationService {

    private final RelationRepository relationRepository;

    /**
     * Finds all relation ownerships where the specified entity identifiers are the target.
     *
     * @param targetEntityIdentifiers List of entity identifiers to search for as relation targets.
     * @return List of {@link RelationAsTargetSummary} representing relations where the given entities are targets.
     */
    public List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityIdentifiers(List<String> targetEntityIdentifiers) {
        return relationRepository.findRelationsSummariesByTargetEntityIdentifiers(targetEntityIdentifiers);
    }

}
