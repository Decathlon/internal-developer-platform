package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.WebhookConnectorPersistenceMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/// Technical helper for JSONB serialization/deserialization in the persistence layer.
///
/// Provides named conversion methods used by [WebhookConnectorPersistenceMapper]
/// via MapStruct's `qualifiedByName` annotation.
///
/// This is a pure utility class with no Spring dependencies, facilitating testability and reusability.
@Component
public class WebhookConnectorJsonbHelper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /// Converts JSONB string to WebhookSecurity domain model.
  /// Defaults to NONE type if JSON is empty.
  @Named("jsonToSecurity")
  public WebhookSecurity toSecurity(String json) {
    if (json == null || json.trim().isEmpty()) {
      return new WebhookSecurity(WebhookSecurityType.NONE, java.util.Map.of());
    }
    try {
      return OBJECT_MAPPER.readValue(json, WebhookSecurity.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid webhook connector security JSONB", e);
    }
  }

  /// Converts WebhookSecurity domain model to JSON string.
  @Named("securityToJson")
  public String toSecurityJson(WebhookSecurity security) {
    try {
      return OBJECT_MAPPER.writeValueAsString(security);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize webhook connector security", e);
    }
  }
}
