package com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping.jslt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.decathlon.idp_core.domain.exception.EntityDynamicMappingException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMappingConfiguration;
import com.decathlon.idp_core.domain.port.EntityDynamicMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;

public class JsltEntityMapper implements EntityDynamicMapper {

    private final ObjectMapper objectMapper;
    private final JsltBridgeBuilder bridgeBuilder;
    private final MappingScriptLoader loader;
    private final Map<Integer, Expression> cache = new ConcurrentHashMap<>();

    public JsltEntityMapper(ObjectMapper objectMapper, JsltBridgeBuilder bridgeBuilder,
            MappingScriptLoader loader) {
        this.objectMapper = objectMapper;
        this.bridgeBuilder = bridgeBuilder;
        this.loader = loader;
    }

    @Override
    public Entity map(String rawJson, EntityDynamicMappingConfiguration config) {
        try {
            // Get or Compile the JSLT Expression (Cache by config hash)
            Expression jslt = cache.computeIfAbsent(config.hashCode(), k -> {
                String fullScript = bridgeBuilder.build(config, loader.getStandardLibrary());

                return Parser.compileString(fullScript);
            });

            // Execute Transformation
            JsonNode input = objectMapper.readTree(rawJson);
            JsonNode output = jslt.apply(input);

            // Handle Filtering (If JSLT returned null, we return null)
            if (output.isNull() || output.isMissingNode()) {
                 throw new EntityDynamicMappingException("Error mapping entity. Mapping result in a null Entity");
            }

            // Map to Domain Entity Record
            return objectMapper.treeToValue(output, Entity.class);

        } catch (Exception e) {
            throw new EntityDynamicMappingException("Error mapping entity",e);
        }
    }

}