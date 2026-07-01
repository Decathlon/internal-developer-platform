package com.decathlon.idp_core.domain.exception.entity_mapping;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_DYNAMIC_MAPPING_ALREADY_IN_USE;

import java.util.List;

public class EntityDynamicMappingAlreadyInUseException extends RuntimeException {

  public EntityDynamicMappingAlreadyInUseException(List<String> identifier) {
    super(ENTITY_DYNAMIC_MAPPING_ALREADY_IN_USE.formatted(identifier));
  }
}
