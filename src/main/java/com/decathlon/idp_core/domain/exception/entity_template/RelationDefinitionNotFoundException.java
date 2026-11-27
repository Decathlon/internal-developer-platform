package com.decathlon.idp_core.domain.exception.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_DEFINITION_NOT_FOUND;

/// Domain exception thrown when no relation definition matches a requested name.
///
/// **Business purpose:** Enforces that relation-scoped queries reference a
/// relation name defined in at least one entity template.
public class RelationDefinitionNotFoundException extends RuntimeException {

  private final String relationName;

  /// Creates an exception for the missing relation definition name.
  ///
  /// @param relationName the relation name that could not be found
  public RelationDefinitionNotFoundException(String relationName) {
    super(String.format(RELATION_DEFINITION_NOT_FOUND, relationName));
    this.relationName = relationName;
  }

  /// Returns the relation name that was not found.
  ///
  /// @return requested relation name
  public String getRelationName() {
    return relationName;
  }
}
