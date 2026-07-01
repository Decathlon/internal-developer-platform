package com.decathlon.idp_core.domain.exception.entity_mapping;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_DYNAMIC_MAPPING_NOT_FOUND;

public class EntityDynamicMappingNotFoundException extends RuntimeException {

  public EntityDynamicMappingNotFoundException(String identifier) {
    super(String.format(ENTITY_DYNAMIC_MAPPING_NOT_FOUND, identifier));
  }
}
