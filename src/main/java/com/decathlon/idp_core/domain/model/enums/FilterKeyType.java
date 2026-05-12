package com.decathlon.idp_core.domain.model.enums;

/// Target fields of a filter criterion in the entity query DSL.
///
/// **Business semantics:**
/// - [ATTRIBUTE] targets a direct entity attribute such as `identifier` or `name`
/// - [PROPERTY] targets the value of a named property (e.g. `property.language`)
/// - [RELATION_NAME] filters by the relation type name (e.g. `relation=api-link`)
/// - [RELATION_ENTITY] targets the target entity identifiers of a named relation
///   (e.g. `relation.database=my-db`)
/// - [RELATION_TEMPLATE] targets the target template identifier of a named relation
///   (e.g. `relation.database.template=api-service`)
/// - [RELATION_PROPERTY] targets a property (`identifier` or `name`) of the target entity
///   in a relation. Key format: `relationName.propertyName`
///   (e.g. `relation.api-link.identifier=microservice-1`)
/// - [RELATIONS_AS_TARGET_NAME] filters by relation type name where this entity is the target
///   (e.g. `relations_as_target=api-link`)
/// - [RELATIONS_AS_TARGET_PROPERTY] targets a property (`identifier` or `name`) of the
///   *source* entity in a reverse relation. Key format: `relationName.propertyName`
///   (e.g. `relations_as_target.api-link.name:microservice`)
public enum FilterKeyType {
    ATTRIBUTE,
    PROPERTY,
    RELATION_NAME,
    RELATION_ENTITY,
    RELATION_TEMPLATE,
    RELATION_PROPERTY,
    RELATIONS_AS_TARGET_NAME,
    RELATIONS_AS_TARGET_PROPERTY
}
