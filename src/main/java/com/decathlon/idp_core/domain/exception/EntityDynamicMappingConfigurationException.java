package com.decathlon.idp_core.domain.exception;

public class EntityDynamicMappingConfigurationException extends RuntimeException {

    public EntityDynamicMappingConfigurationException(String message) {
        super(message);
    }

    public EntityDynamicMappingConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
