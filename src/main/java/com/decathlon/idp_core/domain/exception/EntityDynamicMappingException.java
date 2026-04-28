package com.decathlon.idp_core.domain.exception;

public class EntityDynamicMappingException extends RuntimeException{
        public EntityDynamicMappingException(String message, Throwable cause) {
            super(message, cause);
        }

        public EntityDynamicMappingException(String message) {
            super(message);
        }
}
