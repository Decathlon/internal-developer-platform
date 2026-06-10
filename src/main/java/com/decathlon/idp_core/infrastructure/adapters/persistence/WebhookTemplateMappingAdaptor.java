package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;
import com.decathlon.idp_core.domain.port.WebhookTemplateMappingPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.WebhookTemplateMappingPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaWebhookTemplateMappingRepository;

import lombok.RequiredArgsConstructor;

/// Persistence adapter for webhook-template mapping read operations.
@Component
@RequiredArgsConstructor
public class WebhookTemplateMappingAdaptor implements WebhookTemplateMappingPort {

  private final JpaWebhookTemplateMappingRepository jpaWebhookTemplateMappingRepository;
  private final WebhookTemplateMappingPersistenceMapper webhookTemplateMappingPersistenceMapper;

  /// Finds mappings by template technical id.
  ///
  /// @param templateId entity template UUID
  /// @return mapped domain associations
  @Override
  public List<WebhookTemplateMapping> findByTemplateId(UUID templateId) {
    return jpaWebhookTemplateMappingRepository.findByTemplateId(templateId).stream()
        .map(webhookTemplateMappingPersistenceMapper::toDomain).toList();
  }

}
