package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;
import com.decathlon.idp_core.domain.port.WebhookMappingLinkPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.WebhookMappingLinkPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.WebhookMappingLinkRepository;

import lombok.RequiredArgsConstructor;

/// Persistence adapter for webhook-entityTemplateIdentifier mapping read operations.
@Component
@RequiredArgsConstructor
public class WebhookTemplateMappingAdaptor implements WebhookMappingLinkPort {

  private final WebhookMappingLinkRepository jpaWebhookTemplateMappingRepository;
  private final WebhookMappingLinkPersistenceMapper webhookMappingLinkPersistenceMapper;

  @Override
  public boolean existsByEntityMappingId(UUID id) {
    return jpaWebhookTemplateMappingRepository.existsByEntityMappingId(id);
  }

  @Override
  public List<WebhookTemplateMapping> findByEntityMappingId(UUID id) {
    return jpaWebhookTemplateMappingRepository.findByEntityMappingId(id).stream()
        .map(webhookMappingLinkPersistenceMapper::toDomain).toList();
  }

}
