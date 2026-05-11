package com.decathlon.idp_core.domain.service.entity_template;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.entity_template.RelationNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationTargetTemplateChangeException;
import com.decathlon.idp_core.domain.exception.entity_template.TargetTemplateNotFoundException;
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
/// - Enforce immutable target templates by rejecting target template changes
@Service
@RequiredArgsConstructor
public class RelationDefinitionValidationService {

    private final EntityTemplateRepositoryPort entityTemplateRepositoryPort;

    /// Validates that all relation names are unique within a template.
    ///
    /// @param relations the list of relation definitions to validate
    /// @throws RelationNameAlreadyExistsException if duplicate relation names
    ///                                            are found
    public void validateRelationNamesUniqueness(List<RelationDefinition> relations) {
        Set<String> names = new HashSet<>();
        relations.stream()
                .map(RelationDefinition::name)
                .filter(Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
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
    /// doesn't exist or is null
    public void validateTargetTemplatesExist(List<RelationDefinition> relations) {
        for (RelationDefinition relation : relations) {
            String targetIdentifier = relation.targetTemplateIdentifier();
            if (targetIdentifier == null || !entityTemplateRepositoryPort.existsByIdentifier(targetIdentifier)) {
                throw new TargetTemplateNotFoundException(targetIdentifier);
            }
        }
    }

    /// Validates that target template identifiers are not changed on existing relations.
    ///
    /// **Contract:** Enforces the invariant that relation target templates cannot be
    /// modified after initial creation. Any attempt to change a target template identifier
    /// is forbidden, as existing entity relation values would point to the wrong template type.
    ///
    /// @param existingRelations the existing relation definitions (from the persisted template)
    /// @param incomingRelations the new/updated relation definitions
    /// @throws RelationTargetTemplateChangeException if any relation target template change is attempted
    public void validateTargetTemplateChanges(List<RelationDefinition> existingRelations, List<RelationDefinition> incomingRelations) {
        if (existingRelations == null || existingRelations.isEmpty() ||
                incomingRelations == null || incomingRelations.isEmpty()) {
            return;
        }

        Map<String, RelationDefinition> incomingMap = incomingRelations.stream()
                .collect(Collectors.toMap(r -> r.name().toLowerCase(Locale.ROOT), Function.identity()));

        for (RelationDefinition existing : existingRelations) {
            RelationDefinition incoming = incomingMap.get(existing.name().toLowerCase(Locale.ROOT));
            boolean targetChanged = incoming != null &&
                    !Objects.equals(existing.targetTemplateIdentifier(), incoming.targetTemplateIdentifier());

            if (targetChanged) {
                throw new RelationTargetTemplateChangeException(
                        existing.name(),
                        existing.targetTemplateIdentifier(),
                        incoming.targetTemplateIdentifier());
            }
        }
    }

}
