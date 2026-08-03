package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookMappingLinkJpaEntity;

public interface WebhookMappingLinkRepository
    extends
      JpaRepository<WebhookMappingLinkJpaEntity, WebhookMappingLinkJpaEntity.WebhookMappingLinkId> {
  /// Deletes all mappings associated with a webhook connector.
  void deleteByWebhookId(UUID webhookId);

  /// Retrieves all mappings associated with a webhook connector.
  List<WebhookMappingLinkJpaEntity> findByWebhookId(UUID webhookId);

  List<WebhookMappingLinkJpaEntity> findByWebhookIdIn(List<UUID> webhookIds);

  boolean existsByEntityMappingId(UUID entityMappingId);

  List<WebhookMappingLinkJpaEntity> findByEntityMappingId(UUID entityMappingId);
}
