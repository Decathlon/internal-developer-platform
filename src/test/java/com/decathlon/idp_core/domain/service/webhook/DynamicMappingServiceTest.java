package com.decathlon.idp_core.domain.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity_mapping.EntityDynamicMappingAlreadyExistsException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.domain.port.WebhookTemplateMappingPort;

@DisplayName("DynamicMappingService Tests")
@ExtendWith(MockitoExtension.class)
class DynamicMappingServiceTest {

  @Mock
  private EntityDynamicMappingPort entityDynamicMappingPort;

  @Mock
  private WebhookTemplateMappingPort webhookTemplateMappingPort;

  @Mock
  private EntityDynamicMappingValidationService entityDynamicMappingValidationService;

  private DynamicMappingService service;

  private static final String MAPPING_IDENTIFIER = "github_deployment_status mapping";

  @BeforeEach
  void setUp() {
    service = new DynamicMappingService(entityDynamicMappingPort, webhookTemplateMappingPort,
        entityDynamicMappingValidationService);
  }

  @Test
  @DisplayName("Should validate uniqueness, validate mapping then save")
  void shouldValidateThenSave() {
    EntityDynamicMapping mapping = buildMapping();
    when(entityDynamicMappingPort.existsByIdentifier(MAPPING_IDENTIFIER)).thenReturn(false);
    when(entityDynamicMappingPort.save(mapping)).thenReturn(mapping);

    EntityDynamicMapping result = service.createEntityDynamicMapping(mapping);

    assertThat(result).isEqualTo(mapping);
    verify(entityDynamicMappingValidationService).validateMapping(mapping);
    verify(entityDynamicMappingPort).save(mapping);
  }

  @Test
  @DisplayName("Should throw conflict and not save when identifier already exists")
  void shouldThrowWhenIdentifierAlreadyExists() {
    EntityDynamicMapping mapping = buildMapping();
    when(entityDynamicMappingPort.existsByIdentifier(MAPPING_IDENTIFIER)).thenReturn(true);

    assertThatThrownBy(() -> service.createEntityDynamicMapping(mapping))
        .isInstanceOf(EntityDynamicMappingAlreadyExistsException.class)
        .hasMessageContaining(MAPPING_IDENTIFIER);

    verify(entityDynamicMappingValidationService, never()).validateMapping(any());
    verify(entityDynamicMappingPort, never()).save(any());
  }

  private EntityDynamicMapping buildMapping() {
    return new EntityDynamicMapping(UUID.randomUUID(), MAPPING_IDENTIFIER,
        "github_deployment_status", ".deployment_status != null", ".id", ".name", Map.of(),
        Map.of());
  }
}
