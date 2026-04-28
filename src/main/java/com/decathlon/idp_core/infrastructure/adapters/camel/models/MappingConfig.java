package com.decathlon.idp_core.infrastructure.adapters.camel.models;

import lombok.Data;

@Data
public class MappingConfig {
    private String template;
    private String engine;
    private String query;

    // Getters and Setters
}
