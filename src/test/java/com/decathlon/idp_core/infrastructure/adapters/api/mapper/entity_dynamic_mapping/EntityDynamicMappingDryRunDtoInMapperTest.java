package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/// Unit tests for EntityDynamicMappingDryRunDtoInMapper focusing on payload serialization.
///
/// Coverage includes:
/// - String payload pass-through (no serialization needed)
/// - Object payload serialization success branch
/// - JsonProcessingException handling in catch block
@DisplayName("EntityDynamicMappingDryRunDtoInMapper Unit Tests")
class EntityDynamicMappingDryRunDtoInMapperTest {

  private final EntityDynamicMappingDryRunDtoInMapper mapper = new EntityDynamicMappingDryRunDtoInMapper();

  @Test
  @DisplayName("Should pass through raw JSON string payload without serialization")
  void toRawPayload_passes_through_string_payload() {
    String jsonString = """
        {"action":"pushed","repository":{"full_name":"my-org/my-repo"}}
        """;

    String result = mapper.toRawPayload(jsonString);

    assertThat(result).isEqualTo(jsonString);
  }

  @Test
  @DisplayName("Should successfully serialize Object payload to JSON string")
  void toRawPayload_serializes_object_payload() {
    Object payload = java.util.Map.of("action", "pushed", "repository",
        java.util.Map.of("full_name", "my-org/my-repo"));

    String result = mapper.toRawPayload(payload);

    assertThat(result).isNotBlank().contains("action", "pushed", "my-org/my-repo");
  }

  @Test
  @DisplayName("Should throw EntityDynamicMappingConfigurationException when ObjectMapper serialization fails")
  void toRawPayload_throws_when_json_serialization_fails() throws JsonProcessingException {
    // Create a mock ObjectMapper that throws JsonProcessingException
    ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
    doThrow(new JsonProcessingException("Serialization failed") {
    }).when(mockObjectMapper).writeValueAsString(any());

    // Replace the default ObjectMapper with our mock
    ReflectionTestUtils.setField(mapper, "objectMapper", mockObjectMapper);

    Object testPayload = java.util.Map.of("key", "value");

    // Assert that EntityDynamicMappingConfigurationException is thrown with correct
    // message
    assertThatThrownBy(() -> mapper.toRawPayload(testPayload))
        .isInstanceOf(EntityDynamicMappingConfigurationException.class)
        .hasMessageContaining("Invalid dry-run payload format")
        .hasCauseInstanceOf(JsonProcessingException.class);
  }

  @Test
  @DisplayName("Should throw EntityDynamicMappingConfigurationException with cause when JSON serialization fails")
  void toRawPayload_preserves_exception_cause() throws JsonProcessingException {
    JsonProcessingException originalException = new JsonProcessingException("Root cause") {
    };
    ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
    doThrow(originalException).when(mockObjectMapper).writeValueAsString(any());

    ReflectionTestUtils.setField(mapper, "objectMapper", mockObjectMapper);

    Object testPayload = java.util.Map.of("key", "value");

    assertThatThrownBy(() -> mapper.toRawPayload(testPayload))
        .isInstanceOf(EntityDynamicMappingConfigurationException.class).hasCause(originalException);
  }
}
