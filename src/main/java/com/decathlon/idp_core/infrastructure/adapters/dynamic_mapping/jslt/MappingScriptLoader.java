package com.decathlon.idp_core.infrastructure.adapters.dynamic_mapping.jslt;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class MappingScriptLoader {

    private final ResourceLoader resourceLoader;
    private String cachedStdLib;

    public MappingScriptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Loads the platform's global functions.
     * Cached in memory because this file rarely changes during runtime.
     */
    public String getStandardLibrary() {
        if (cachedStdLib == null) {
            cachedStdLib = loadResource("classpath:jslt/stdlib.jslt");
        }
        return cachedStdLib;
    }

    private String loadResource(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            // Java 25: Efficiently reading small to medium resource files
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Mapping Infrastructure Failure: Could not load " + path, e);
        }
    }
}