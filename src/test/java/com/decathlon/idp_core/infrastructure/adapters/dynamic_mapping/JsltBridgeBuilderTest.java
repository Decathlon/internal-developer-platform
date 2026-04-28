package com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMappingConfiguration;
import com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping.jslt.JsltBridgeBuilder;

class JsltBridgeBuilderTest {
    private final JsltBridgeBuilder bridge = new JsltBridgeBuilder();

    @Test
    void shouldAssembleLayeredScript() {
        var config = new EntityDynamicMappingConfiguration(
            "test", 
            ".id != null", 
            "def custom() 1", 
            new EntityDynamicMapping(".id", ".template", ".name", Map.of(), Map.of())
        );
        
        String result = bridge.build(config, "def std() 0");
        
        assertTrue(result.contains("def std()"));
        assertTrue(result.contains("def custom()"));
        assertTrue(result.contains("if (.id != null)"));
    }
}