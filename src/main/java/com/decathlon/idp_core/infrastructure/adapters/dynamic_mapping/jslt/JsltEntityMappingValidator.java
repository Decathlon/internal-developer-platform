package com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping.jslt;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMappingConfiguration;
import com.decathlon.idp_core.domain.port.EntityDynamicMapperValidator;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;


@Component
public class JsltEntityMappingValidator implements EntityDynamicMapperValidator {
    
    private final JsltBridgeBuilder bridgeBuilder;
    private final MappingScriptLoader loader;

    public JsltEntityMappingValidator(JsltBridgeBuilder bridgeBuilder, MappingScriptLoader loader) {
        this.bridgeBuilder = bridgeBuilder;
        this.loader = loader;
    }

    @Override
    public void validate(EntityDynamicMappingConfiguration config) {
        try {
            // Generate the full script as it would be executed at runtime
            String fullScript = bridgeBuilder.build(config, loader.getStandardLibrary());
            
            // Attempt compilation to catch syntax errors
            Parser.compileString(fullScript);
            
        } catch (JsltException e) {
            // Wrap the technical error with the specific JSLT message
            throw new EntityDynamicMappingConfigurationException("JSLT Syntax Error: " + e.getMessage());
        } catch (Exception e) {
            throw new EntityDynamicMappingConfigurationException("Unexpected validation error: " + e.getMessage());
        }
    }
}
