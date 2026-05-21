package com.decathlon.idp_core.domain.exception.entity_mapping;

public class EntityDynamicMappingConfigurationException extends RuntimeException {

    public EntityDynamicMappingConfigurationException(String message) {
        super(message);
    }

    public EntityDynamicMappingConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
