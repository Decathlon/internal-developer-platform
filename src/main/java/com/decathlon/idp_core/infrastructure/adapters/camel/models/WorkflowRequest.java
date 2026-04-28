package com.decathlon.idp_core.infrastructure.adapters.camel.models;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMappingConfiguration;

import lombok.Data;

@Data
public class WorkflowRequest {
    private String name;
    private String description;
    private TriggerConfig trigger;
    private EntityDynamicMappingConfiguration mapping;
    private SecurityConfig security;
  
}
