package com.decathlon.idp_core.domain.service.entity_template;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.RelationNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.TargetTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

import lombok.RequiredArgsConstructor;

/// Domain service for relation definition validations and business rules.
///
/// **Business purpose:** Enforces invariants for relation definitions within
/// entity templates, including uniqueness constraints and referential integrity.
///
/// **Key responsibilities:**
/// - Validate relation name uniqueness within an entity template
/// - Validate that all target templates referenced by relations exist
@Service
@RequiredArgsConstructor
public class RelationDefinitionValidationService {

    private final EntityTemplateRepositoryPort entityTemplateRepositoryPort;

    /// Validates that all relation names are unique within a template.
    ///
    /// @param relations the list of relation definitions to validate
    /// @throws RelationNameAlreadyExistsException if duplicate relation names
    ///                                            are found
    public void validateUniqueRelationNames(List<RelationDefinition> relations) {
        if (relations == null || relations.isEmpty()) {
            return;
        }
        Set<String> names = new HashSet<>();
        relations.stream()
                .map(RelationDefinition::name)
                .filter(Objects::nonNull)
                .filter(name -> !names.add(name))
                .findFirst()
                .ifPresent(name -> {
                    throw new RelationNameAlreadyExistsException(name);
                });
    }

    /// Validates that all target templates exist for the given relations.
    ///
    /// **Contract:** Ensures referential integrity by verifying that every
    /// target template referenced by a relation exists in the system.
    ///
    /// @param relations the list of relation definitions to validate
    /// @throws TargetTemplateNotFoundException if any referenced target template
    ///                                         doesn't exist
    public void validateTargetTemplatesExist(List<RelationDefinition> relations) {
        if (relations == null || relations.isEmpty()) {
            return;
        }

        for (RelationDefinition relation : relations) {
            if (relation.targetTemplateIdentifier() != null &&
                    !entityTemplateRepositoryPort.existsByIdentifier(relation.targetTemplateIdentifier())) {
                throw new TargetTemplateNotFoundException(relation.targetTemplateIdentifier());
            }
        }
    }

}
