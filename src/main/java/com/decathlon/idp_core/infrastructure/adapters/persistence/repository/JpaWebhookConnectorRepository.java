package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookConnectorJpaEntity;

public interface JpaWebhookConnectorRepository
    extends
      JpaRepository<WebhookConnectorJpaEntity, UUID> {
  Optional<WebhookConnectorJpaEntity> findByIdentifier(String identifier);

  boolean existsByIdentifier(String identifier);

  boolean existsByTitle(String title);

  void deleteByIdentifier(String identifier);
}
