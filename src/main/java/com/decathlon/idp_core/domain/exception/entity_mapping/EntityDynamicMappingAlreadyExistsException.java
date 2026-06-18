package com.decathlon.idp_core.domain.exception.entity_mapping;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_DYNAMIC_MAPPING_ALREADY_EXISTS;

public class EntityDynamicMappingAlreadyExistsException extends RuntimeException {

  public EntityDynamicMappingAlreadyExistsException(String identifier) {
    super(String.format(ENTITY_DYNAMIC_MAPPING_ALREADY_EXISTS, identifier));
  }
}
