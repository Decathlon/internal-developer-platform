package com.decathlon.idp_core.domain.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateInUseByWebhookMappingException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;
import com.decathlon.idp_core.domain.port.WebhookTemplateMappingPort;

@DisplayName("WebhookTemplateMappingService Tests")
@ExtendWith(MockitoExtension.class)
class WebhookTemplateMappingServiceTest {

  @Mock
  private WebhookTemplateMappingPort webhookTemplateMappingPort;

  private WebhookTemplateMappingService service;

  @BeforeEach
  void setUp() {
    service = new WebhookTemplateMappingService(webhookTemplateMappingPort);
  }

  // ---------------------------------------------------------------------------
  // findByTemplateId
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("findByTemplateId")
  class FindByTemplateIdTests {

    @Test
    @DisplayName("Should return all mappings for a given template id")
    void shouldReturnMappingsForTemplateId() {
      UUID templateId = UUID.randomUUID();
      WebhookTemplateMapping mapping = buildMapping("my-webhook");
      when(webhookTemplateMappingPort.findByTemplateId(templateId)).thenReturn(List.of(mapping));

      List<WebhookTemplateMapping> result = service.findByTemplateId(templateId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo(mapping);
    }

    @Test
    @DisplayName("Should return empty list when no mappings found for template")
    void shouldReturnEmptyListWhenNoMappings() {
      UUID templateId = UUID.randomUUID();
      when(webhookTemplateMappingPort.findByTemplateId(templateId)).thenReturn(List.of());

      List<WebhookTemplateMapping> result = service.findByTemplateId(templateId);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple mappings when several webhooks reference the template")
    void shouldReturnMultipleMappings() {
      UUID templateId = UUID.randomUUID();
      WebhookTemplateMapping m1 = buildMapping("webhook-a");
      WebhookTemplateMapping m2 = buildMapping("webhook-b");
      when(webhookTemplateMappingPort.findByTemplateId(templateId)).thenReturn(List.of(m1, m2));

      List<WebhookTemplateMapping> result = service.findByTemplateId(templateId);

      assertThat(result).hasSize(2).extracting(wm -> wm.webhookConnector().identifier())
          .containsExactlyInAnyOrder("webhook-a", "webhook-b");
    }
  }

  // ---------------------------------------------------------------------------
  // validateTemplateNotInUseMapping
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("validateTemplateNotInUseMapping")
  class ValidateTemplateNotInUseMappingTests {

    @Test
    @DisplayName("Should pass when template is not used by any webhook mapping")
    void shouldPassWhenTemplateNotInUse() {
      UUID templateId = UUID.randomUUID();
      when(webhookTemplateMappingPort.findByTemplateId(templateId)).thenReturn(List.of());

      assertThatCode(() -> service.validateTemplateNotInUseMapping(templateId))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw EntityTemplateInUseByWebhookMappingException when template is in use")
    void shouldThrowWhenTemplateIsInUse() {
      UUID templateId = UUID.randomUUID();
      WebhookTemplateMapping mapping = buildMapping("github-dora");
      when(webhookTemplateMappingPort.findByTemplateId(templateId)).thenReturn(List.of(mapping));

      assertThatThrownBy(() -> service.validateTemplateNotInUseMapping(templateId))
          .isInstanceOf(EntityTemplateInUseByWebhookMappingException.class)
          .hasMessageContaining("github-dora");
    }

    @Test
    @DisplayName("Should include all referencing webhook identifiers in the exception message")
    void shouldIncludeAllWebhookIdentifiersInException() {
      UUID templateId = UUID.randomUUID();
      WebhookTemplateMapping m1 = buildMapping("webhook-a");
      WebhookTemplateMapping m2 = buildMapping("webhook-b");
      when(webhookTemplateMappingPort.findByTemplateId(templateId)).thenReturn(List.of(m1, m2));

      assertThatThrownBy(() -> service.validateTemplateNotInUseMapping(templateId))
          .isInstanceOf(EntityTemplateInUseByWebhookMappingException.class)
          .hasMessageContaining("webhook-a").hasMessageContaining("webhook-b");
    }

    @Test
    @DisplayName("Should deduplicate webhook identifiers when same webhook appears multiple times")
    void shouldDeduplicateWebhookIdentifiers() {
      UUID templateId = UUID.randomUUID();
      // Same webhook appears twice (two different template mappings for the same
      // webhook)
      WebhookConnector sameWebhook = buildWebhookConnector("same-webhook");
      WebhookTemplateMapping m1 = new WebhookTemplateMapping(UUID.randomUUID(), sameWebhook, null,
          null, null);
      WebhookTemplateMapping m2 = new WebhookTemplateMapping(UUID.randomUUID(), sameWebhook, null,
          null, null);
      when(webhookTemplateMappingPort.findByTemplateId(templateId)).thenReturn(List.of(m1, m2));

      assertThatThrownBy(() -> service.validateTemplateNotInUseMapping(templateId))
          .isInstanceOf(EntityTemplateInUseByWebhookMappingException.class).satisfies(ex -> {
            // "same-webhook" should appear only once in the message
            String msg = ex.getMessage();
            assertThat(msg.indexOf("same-webhook")).isEqualTo(msg.lastIndexOf("same-webhook"));
          });
    }

    @Test
    @DisplayName("Should skip webhooks with null identifier when building exception message")
    void shouldSkipNullWebhookIdentifier() {
      UUID templateId = UUID.randomUUID();
      // Mapping where webhookConnector is not null but id is null
      WebhookConnector webhookWithNullId = new WebhookConnector(null, "valid-webhook", "Title",
          "desc", false, List.of(), new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));
      WebhookTemplateMapping m1 = new WebhookTemplateMapping(UUID.randomUUID(), webhookWithNullId,
          null, null, null);
      when(webhookTemplateMappingPort.findByTemplateId(templateId)).thenReturn(List.of(m1));

      // Should still throw but with empty webhook list (null id filtered out)
      assertThatThrownBy(() -> service.validateTemplateNotInUseMapping(templateId))
          .isInstanceOf(EntityTemplateInUseByWebhookMappingException.class);
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private WebhookTemplateMapping buildMapping(String webhookIdentifier) {
    WebhookConnector connector = buildWebhookConnector(webhookIdentifier);
    return new WebhookTemplateMapping(UUID.randomUUID(), connector, null, null, null);
  }

  private WebhookConnector buildWebhookConnector(String identifier) {
    return new WebhookConnector(UUID.randomUUID(), identifier, identifier + " Title", "desc", true,
        List.of(), new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));
  }
}
