package com.decathlon.idp_core.domain.exception.entity_dynamic_mapping;

import lombok.Getter;

@Getter
public class EntityDynamicMappingJsltErrorException extends RuntimeException {
  public EntityDynamicMappingJsltErrorException(String message) {
    super(message);
  }
}
