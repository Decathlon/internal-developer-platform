package com.decathlon.idp_core.infrastructure.adapters.persistence;

import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.WebhookConnectorPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookTemplateMappingJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaWebhookConnectorRepository;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaWebhookTemplateMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing {@link WebhookConnectorRepositoryPort}.
 * Delegates to Spring Data JPA and uses {@link WebhookConnectorPersistenceMapper}
 * to convert between JPA entities and domain models.
 */
@Component
@RequiredArgsConstructor
public class PostgresWebhookConnectorAdapter implements WebhookConnectorRepositoryPort {

    private final JpaWebhookConnectorRepository jpaWebhookConnectorRepository;
    private final JpaWebhookTemplateMappingRepository jpaWebhookTemplateMappingRepository;
    private final EntityTemplateRepositoryPort entityTemplateRepositoryPort;
    private final WebhookConnectorPersistenceMapper mapper;

    @Override
    public Optional<WebhookConnector> findByIdentifier(String identifier) {
        return jpaWebhookConnectorRepository.findByIdentifier(identifier).map(mapper::toDomain);
    }

    @Override
    public Page<WebhookConnector> findAll(Pageable pageable) {
        return jpaWebhookConnectorRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdentifier(String identifier) {
        return jpaWebhookConnectorRepository.existsByIdentifier(identifier);
    }

    @Override
    public boolean existsByTitle(String title) {
        return jpaWebhookConnectorRepository.existsByTitle(title);
    }

    @Override
    public WebhookConnector save(WebhookConnector connector) {
        var savedConnector = jpaWebhookConnectorRepository.save(mapper.toJpa(connector));
        persistTemplateMappings(savedConnector.getId(), connector);
        return mapper.toDomain(savedConnector);
    }

    @Override
    public void deleteByIdentifier(String identifier) {
        jpaWebhookConnectorRepository.deleteByIdentifier(identifier);
    }

    private void persistTemplateMappings(UUID webhookId, WebhookConnector connector) {
        jpaWebhookTemplateMappingRepository.deleteByWebhookId(webhookId);

        var mappings = connector.mappings().stream()
                .map(mapping -> toJpaTemplateMapping(webhookId, mapping))
                .toList();

        if (!mappings.isEmpty()) {
            jpaWebhookTemplateMappingRepository.saveAll(mappings);
        }
    }

    private WebhookTemplateMappingJpaEntity toJpaTemplateMapping(UUID webhookId, EntityDynamicMapping mapping) {
        EntityTemplate entityTemplate = entityTemplateRepositoryPort.findByIdentifier(mapping.templateIdentifier())
                .orElseThrow(() -> new EntityTemplateNotFoundException("identifier", mapping.templateIdentifier()));

        return WebhookTemplateMappingJpaEntity.builder()
                .webhookId(webhookId)
                .templateId(entityTemplate.id())
                .jsltFilter(mapping.filter())
                .build();
    }
}
