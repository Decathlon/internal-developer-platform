package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.WebhookConnectorPersistenceMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Technical helper for JSONB serialization/deserialization in the persistence layer.
 *
 * <p>Provides named conversion methods used by {@link WebhookConnectorPersistenceMapper}
 * via MapStruct's {@code qualifiedByName} annotation. This is a pure infrastructure utility,
 * not a domain mapper.
 */
@Component
public class WebhookConnectorJsonbHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Named("jsonToMappings")
    public List<EntityDynamicMapping> toMappings(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid webhook connector mappings JSONB", e);
        }
    }

    @Named("mappingsToJson")
    public String toJson(List<EntityDynamicMapping> mappings) {
        try {
            return OBJECT_MAPPER.writeValueAsString(mappings);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize webhook connector mappings", e);
        }
    }

    @Named("jsonToSecurity")
    public WebhookSecurity toSecurity(String json) {
        if (!StringUtils.hasText(json)) {
            return new WebhookSecurity(WebhookSecurityType.NONE, java.util.Map.of());
        }
        try {
            return OBJECT_MAPPER.readValue(json, WebhookSecurity.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid webhook connector security JSONB", e);
        }
    }

    @Named("securityToJson")
    public String toSecurityJson(WebhookSecurity security) {
        try {
            return OBJECT_MAPPER.writeValueAsString(security);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize webhook connector security", e);
        }
    }
}
