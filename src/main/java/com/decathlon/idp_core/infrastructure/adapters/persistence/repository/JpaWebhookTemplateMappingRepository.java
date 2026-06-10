package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookTemplateMappingJpaEntity;

public interface JpaWebhookTemplateMappingRepository
    extends
      JpaRepository<WebhookTemplateMappingJpaEntity, UUID> {

  /// Deletes all mappings associated with a webhook connector.
  void deleteByWebhookId(UUID webhookId);

  /// Retrieves all mappings associated with a webhook connector.
  List<WebhookTemplateMappingJpaEntity> findByWebhookId(UUID webhookId);
  List<WebhookTemplateMappingJpaEntity> findByTemplateId(UUID templateId);
  boolean existsByTemplateId(UUID templateId);
}
