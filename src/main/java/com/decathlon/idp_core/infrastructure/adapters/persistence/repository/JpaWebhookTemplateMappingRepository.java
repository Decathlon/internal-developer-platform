package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookTemplateMappingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaWebhookTemplateMappingRepository extends JpaRepository<WebhookTemplateMappingJpaEntity, UUID> {

    void deleteByWebhookId(UUID webhookId);
}
