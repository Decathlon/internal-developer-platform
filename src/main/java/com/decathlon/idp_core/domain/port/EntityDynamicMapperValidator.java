package com.decathlon.idp_core.domain.port;

import com.decathlon.idp_core.domain.exception.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMappingConfiguration;


public interface EntityDynamicMapperValidator {

    /**
     * Validates the provided configuration against the DSL syntax and platform rules.
     * * @param config The configuration to validate.
     * @throws EntityDynamicMappingConfigurationException if the DSL is malformed or missing required fields.
     */
    void validate(EntityDynamicMappingConfiguration config);
    
}
