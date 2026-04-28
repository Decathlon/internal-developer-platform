package com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMappingConfiguration;
import com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping.jslt.JsltBridgeBuilder;
import com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping.jslt.JsltEntityMapper;
import com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping.jslt.MappingScriptLoader;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class JsltEntityMapperTest {
    @Mock private MappingScriptLoader loader;
    private JsltEntityMapper mapper;

    @BeforeEach
    void setup() {
        when(loader.getStandardLibrary()).thenReturn("def slugify(t) 0");
        mapper = new JsltEntityMapper(new ObjectMapper(), new JsltBridgeBuilder(), loader);
    }

    @Test
    void shouldMapRawJsonToDomainRecord() {
        String rawJson = "{\"identifier\": \"test-entity\", \"name\": \"My Project\", \"template\": \"test-template\"}";
        var config = new EntityDynamicMappingConfiguration(
            "sonar", null, null, 
            new EntityDynamicMapping(".identifier", ".template", ".name", Map.of(), Map.of())
        );

        Entity result = mapper.map(rawJson, config);

        assertEquals("test-entity", result.identifier());
        assertEquals("My Project", result.name());
        assertEquals("test-template", result.templateIdentifier());
    }

    @Test
    void shouldMapWithCustomFunction() {
        // Custom JSLT function to uppercase the name
        String customJslt = "def toUpper(s) if ($s) uppercase($s) else  null";
        when(loader.getStandardLibrary()).thenReturn(customJslt);
        mapper = new JsltEntityMapper(new ObjectMapper(), new JsltBridgeBuilder(), loader);

        String rawJson = "{\"identifier\": \"test-entity\", \"name\": \"my project\", \"template\": \"test-template\"}";
        var config = new EntityDynamicMappingConfiguration(
            "sonar", null, "def customName(n) toUpper($n)",
            new EntityDynamicMapping(
                ".identifier", ".template", "customName(.name)", Map.of(), Map.of())
        );

        Entity result = mapper.map(rawJson, config);

        assertEquals("test-entity", result.identifier());
        assertEquals("MY PROJECT", result.name());
        assertEquals("test-template", result.templateIdentifier());
    }

    @Test
    void shouldMapWithMultipleCustomFunctions() {
        // Custom JSLT functions: slugify and prefix
        String customJslt = "def slugify(s) if ($s) string:replace($s, ' ', '-') else null\ndef prefix(s, p) $p + $s";
        when(loader.getStandardLibrary()).thenReturn(customJslt);
        mapper = new JsltEntityMapper(new ObjectMapper(), new JsltBridgeBuilder(), loader);

        String rawJson = "{\"identifier\": \"entity-1\", \"name\": \"My Project\", \"template\": \"test-template\"}";
        var config = new EntityDynamicMappingConfiguration(
            "sonar", null, "def customId(id) prefix(slugify($id), 'ID-')",
            new EntityDynamicMapping(
                "customId(.identifier)", ".template", ".name", Map.of(), Map.of())
        );

        Entity result = mapper.map(rawJson, config);

        assertEquals("ID-entity-1", result.identifier());
        assertEquals("My Project", result.name());
        assertEquals("test-template", result.templateIdentifier());
    }
}
