package com.decathlon.idp_core.domain.port;

import java.util.List;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;

public interface EntityDynamicMappingPort {

  List<EntityDynamicMapping> findByTemplateIdentifier(String templateIdentifier);
  Boolean existsByTemplateIdentifier(String templateIdentifier);
}
