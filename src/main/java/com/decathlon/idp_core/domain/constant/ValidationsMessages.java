package com.decathlon.idp_core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationsMessages {

    // Entity Template validation messages
    public static final String TEMPLATE_ALREADY_EXISTS = "An Entity Template already exists with the same identifier";
    public static final String TEMPLATE_IDENTIFIER_MANDATORY = "Entity Template identifier is mandatory and cannot be blank";
    public static final String PROPERTY_DEFINITIONS_MANDATORY = "Entity Template property definitions are mandatory and cannot be empty";

    // Property Definition validation messages
    public static final String PROPERTY_NAME_MANDATORY = "Property name is mandatory and cannot be blank";
    public static final String PROPERTY_DESCRIPTION_MANDATORY = "Property description is mandatory and cannot be blank";
    public static final String PROPERTY_TYPE_MANDATORY = "Property type is mandatory";

    // Relation Definition validation messages
    public static final String RELATION_NAME_MANDATORY = "Relation name is mandatory and cannot be blank";
    public static final String RELATION_TARGET_IDENTIFIER_MANDATORY = "Target entity identifier is mandatory and cannot be blank";
    public static final String RELATION_NAME_MANDATORY_SIMPLE = "Relation name is mandatory";
    public static final String RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE = "Relation target identifier is mandatory";
}
