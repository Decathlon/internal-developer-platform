package com.decathlon.idp_core.infrastructure.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.WebhookTemplateMappingPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookTemplateMappingJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaWebhookTemplateMappingRepository;

@DisplayName("WebhookTemplateMappingAdaptor Tests")
@ExtendWith(MockitoExtension.class)
class WebhookTemplateMappingAdaptorTest {

  @Mock
  private JpaWebhookTemplateMappingRepository jpaWebhookTemplateMappingRepository;

  @Mock
  private WebhookTemplateMappingPersistenceMapper webhookTemplateMappingPersistenceMapper;

  private WebhookTemplateMappingAdaptor adaptor;

  @BeforeEach
  void setUp() {
    adaptor = new WebhookTemplateMappingAdaptor(jpaWebhookTemplateMappingRepository,
        webhookTemplateMappingPersistenceMapper);
  }

  // ---------------------------------------------------------------------------
  // findByTemplateId
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("findByTemplateId")
  class FindByTemplateIdTests {

    @Test
    @DisplayName("Should return mapped domain objects for a given template id")
    void shouldReturnMappedDomainObjects() {
      UUID templateId = UUID.randomUUID();
      var jpa = buildJpaEntity(templateId);
      var domain = buildDomain("my-webhook");

      when(jpaWebhookTemplateMappingRepository.findByTemplateId(templateId))
          .thenReturn(List.of(jpa));
      when(webhookTemplateMappingPersistenceMapper.toDomain(jpa)).thenReturn(domain);

      var result = adaptor.findByTemplateId(templateId);

      assertThat(result).hasSize(1).containsExactly(domain);
    }

    @Test
    @DisplayName("Should return empty list when no mappings for template")
    void shouldReturnEmptyListWhenNone() {
      UUID templateId = UUID.randomUUID();
      when(jpaWebhookTemplateMappingRepository.findByTemplateId(templateId)).thenReturn(List.of());

      var result = adaptor.findByTemplateId(templateId);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple domain objects when several mappings exist")
    void shouldReturnMultipleMappings() {
      UUID templateId = UUID.randomUUID();
      var jpa1 = buildJpaEntity(templateId);
      var jpa2 = buildJpaEntity(templateId);
      var domain1 = buildDomain("webhook-a");
      var domain2 = buildDomain("webhook-b");

      when(jpaWebhookTemplateMappingRepository.findByTemplateId(templateId))
          .thenReturn(List.of(jpa1, jpa2));
      when(webhookTemplateMappingPersistenceMapper.toDomain(jpa1)).thenReturn(domain1);
      when(webhookTemplateMappingPersistenceMapper.toDomain(jpa2)).thenReturn(domain2);

      var result = adaptor.findByTemplateId(templateId);

      assertThat(result).hasSize(2).containsExactlyInAnyOrder(domain1, domain2);
    }
  }

  // ---------------------------------------------------------------------------
  // existsByEntityMappingId
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("existsByEntityMappingId")
  class ExistsByEntityMappingIdTests {

    @Test
    @DisplayName("Should return true when mapping exists for entity mapping id")
    void shouldReturnTrueWhenExists() {
      UUID mappingId = UUID.randomUUID();
      when(jpaWebhookTemplateMappingRepository.existsByEntityMappingId(mappingId)).thenReturn(true);
      assertThat(adaptor.existsByEntityMappingId(mappingId)).isTrue();
    }

    @Test
    @DisplayName("Should return false when no mapping exists for entity mapping id")
    void shouldReturnFalseWhenNotExists() {
      UUID mappingId = UUID.randomUUID();
      when(jpaWebhookTemplateMappingRepository.existsByEntityMappingId(mappingId))
          .thenReturn(false);
      assertThat(adaptor.existsByEntityMappingId(mappingId)).isFalse();
    }
  }

  // ---------------------------------------------------------------------------
  // findByEntityMappingId
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("findByEntityMappingId")
  class FindByEntityMappingIdTests {

    @Test
    @DisplayName("Should return mapped domain objects for a given entity mapping id")
    void shouldReturnMappedDomainObjects() {
      UUID entityMappingId = UUID.randomUUID();
      var jpa = buildJpaEntity(UUID.randomUUID());
      var domain = buildDomain("my-webhook");

      when(jpaWebhookTemplateMappingRepository.findByEntityMappingId(entityMappingId))
          .thenReturn(List.of(jpa));
      when(webhookTemplateMappingPersistenceMapper.toDomain(jpa)).thenReturn(domain);

      var result = adaptor.findByEntityMappingId(entityMappingId);

      assertThat(result).hasSize(1).containsExactly(domain);
    }

    @Test
    @DisplayName("Should return empty list when no mappings for entity mapping id")
    void shouldReturnEmptyListWhenNone() {
      UUID entityMappingId = UUID.randomUUID();
      when(jpaWebhookTemplateMappingRepository.findByEntityMappingId(entityMappingId))
          .thenReturn(List.of());

      var result = adaptor.findByEntityMappingId(entityMappingId);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return multiple domain objects for the same entity mapping id")
    void shouldReturnMultipleMappingsForSameMappingId() {
      UUID entityMappingId = UUID.randomUUID();
      var jpa1 = buildJpaEntity(UUID.randomUUID());
      var jpa2 = buildJpaEntity(UUID.randomUUID());
      var domain1 = buildDomain("webhook-a");
      var domain2 = buildDomain("webhook-b");

      when(jpaWebhookTemplateMappingRepository.findByEntityMappingId(entityMappingId))
          .thenReturn(List.of(jpa1, jpa2));
      when(webhookTemplateMappingPersistenceMapper.toDomain(jpa1)).thenReturn(domain1);
      when(webhookTemplateMappingPersistenceMapper.toDomain(jpa2)).thenReturn(domain2);

      var result = adaptor.findByEntityMappingId(entityMappingId);

      assertThat(result).hasSize(2).containsExactlyInAnyOrder(domain1, domain2);
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private WebhookTemplateMappingJpaEntity buildJpaEntity(UUID templateId) {
    return WebhookTemplateMappingJpaEntity.builder().id(UUID.randomUUID())
        .webhookId(UUID.randomUUID()).templateId(templateId).entityMappingId(UUID.randomUUID())
        .jsltFilter(".action == \"push\"").build();
  }

  private WebhookTemplateMapping buildDomain(String webhookIdentifier) {
    WebhookConnector connector = new WebhookConnector(UUID.randomUUID(), webhookIdentifier,
        webhookIdentifier + " Title", "desc", true, List.of(),
        new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));
    return new WebhookTemplateMapping(UUID.randomUUID(), connector, null, null, null);
  }
}
