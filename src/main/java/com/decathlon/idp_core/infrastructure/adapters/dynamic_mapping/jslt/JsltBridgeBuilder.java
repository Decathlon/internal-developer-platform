package com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping.jslt;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMappingConfiguration;

@Service
public class JsltBridgeBuilder {
public String build(EntityDynamicMappingConfiguration config, String stdLib) {
        // Build the JSLT script by layering: StdLib -> Custom Functions -> Entity Mapping
        return """
               %s
               
               %s
               
               %s
               """.formatted(
                stdLib,
                config.functions() != null ? config.functions() : "",
                generateMappingBody(config)
        );
    }

    private String generateMappingBody(EntityDynamicMappingConfiguration config) {
        StringBuilder sb = new StringBuilder();
        
        if (config.filter() != null && !config.filter().isBlank()) {
            sb.append("if (").append(config.filter()).append(") \n");
        }

        sb.append("{\n");
        sb.append("  \"identifier\": ").append(config.entityMapping().identifier()).append(",\n");
        sb.append("  \"name\": ").append(config.entityMapping().name()).append(",\n");
        sb.append("  \"templateIdentifier\": ").append(config.entityMapping().templateIdentifier()).append(",\n");
        sb.append("  \"properties\": {\n");
        
        config.entityMapping().properties().forEach((k, v) -> 
            sb.append("    \"").append(k).append("\": ").append(v).append(",\n")
        );
        
        sb.append("  },\n  \"relations\": {\n");
        
        config.entityMapping().relations().forEach((k, v) -> 
            sb.append("    \"").append(k).append("\": ").append(v).append(",\n")
        );
        
        sb.append("  }\n}");

        if (config.filter() != null && !config.filter().isBlank()) {
            sb.append("\n else null");
        }
        
        return sb.toString();
    }
    
}
