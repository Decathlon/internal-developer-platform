package com.decathlon.idp_core.domain.service.entity_template;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.RelationNameAlreadyExistsException;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;

/// Domain service for relation definition validations and business rules.
///
/// **Business purpose:** Enforces invariants for relation definitions within
/// entity templates, including uniqueness constraints
///
/// **Key responsibilities:**
/// - Validate relation name uniqueness within an entity template
@Service
public class RelationDefinitionService {

    /// Validates that all relation names are unique within a template.
    ///
    /// @param relations the list of relation definitions to validate
    /// @throws RelationNameAlreadyExistsException if duplicate relation names
    ///                                            are found
    public void validateUniqueRelationNames(List<RelationDefinition> relations) {
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

}
