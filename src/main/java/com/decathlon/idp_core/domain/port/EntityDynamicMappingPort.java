package com.decathlon.idp_core.domain.port;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;

public interface EntityDynamicMappingPort {

  List<EntityDynamicMapping> findByTemplateIdentifier(String templateIdentifier);

  Boolean existsByTemplateIdentifier(String templateIdentifier);

  boolean existsByIdentifier(String identifier);

  Optional<EntityDynamicMapping> findByIdentifier(String identifier);

  EntityDynamicMapping save(EntityDynamicMapping entityDynamicMapping);

  Page<EntityDynamicMapping> findAll(Pageable pageable);

  void deleteByIdentifier(String identifier);

}
