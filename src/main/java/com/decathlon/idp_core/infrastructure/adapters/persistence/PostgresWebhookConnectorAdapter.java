package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity_mapping.EntityDynamicMappingNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityDynamicMappingPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.WebhookConnectorPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookConnectorJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookTemplateMappingJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityDynamicMappingRepository;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaWebhookConnectorRepository;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaWebhookTemplateMappingRepository;

import lombok.RequiredArgsConstructor;

/// Persistence adapter implementing [WebhookConnectorRepositoryPort].
///
/// Delegates to Spring Data JPA and uses [WebhookConnectorPersistenceMapper]
/// to convert between JPA entities and domain models.
///
/// Handles the complex persistence of mappings across three tables:
/// - webhook_connector (core connector data)
/// - entity_dynamic_mapping (mapping configurations)
/// - webhook_template_mapping (many-to-many link)
@Component
@RequiredArgsConstructor
public class PostgresWebhookConnectorAdapter implements WebhookConnectorRepositoryPort {

  private final JpaWebhookConnectorRepository jpaWebhookConnectorRepository;
  private final JpaWebhookTemplateMappingRepository jpaWebhookTemplateMappingRepository;
  private final JpaEntityDynamicMappingRepository jpaEntityDynamicMappingRepository;
  private final EntityTemplateRepositoryPort entityTemplateRepositoryPort;
  private final EntityDynamicMappingPort entityDynamicMappingPort;
  private final WebhookConnectorPersistenceMapper mapper;
  private final EntityDynamicMappingPersistenceMapper mappingMapper;

  @Override
  public Optional<WebhookConnector> findByIdentifier(String identifier) {
    return jpaWebhookConnectorRepository.findByIdentifier(identifier)
        .map(this::loadConnectorWithMappings);
  }

  @Override
  public Page<WebhookConnector> findAll(Pageable pageable) {
    return jpaWebhookConnectorRepository.findAll(pageable).map(this::loadConnectorWithMappings);
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
    WebhookConnectorJpaEntity savedConnector = jpaWebhookConnectorRepository
        .save(mapper.toJpa(connector));
    persistTemplateMappings(savedConnector.getId(), connector);
    return loadConnectorWithMappings(savedConnector);
  }

  @Override
  public void deleteByIdentifier(String identifier) {
    jpaWebhookConnectorRepository.deleteByIdentifier(identifier);
  }

  /// Loads a connector with its associated mappings from the
  /// webhook_template_mapping table.
  /// Since WebhookConnector is a Record (immutable), we create a new instance
  /// with the loaded mappings.
  private WebhookConnector loadConnectorWithMappings(WebhookConnectorJpaEntity jpaEntity) {
    WebhookConnector connectorWithoutMappings = mapper.toDomain(jpaEntity);
    List<EntityDynamicMapping> mappings = loadMappingsForWebhook(jpaEntity.getId());

    // Since WebhookConnector is a Record, create a new instance with loaded
    // mappings
    return new WebhookConnector(connectorWithoutMappings.id(),
        connectorWithoutMappings.identifier(), connectorWithoutMappings.title(),
        connectorWithoutMappings.description(), connectorWithoutMappings.enabled(), mappings,
        connectorWithoutMappings.security());
  }

  /// Loads all dynamic mappings associated with a webhook connector.
  private List<EntityDynamicMapping> loadMappingsForWebhook(UUID webhookId) {
    return jpaWebhookTemplateMappingRepository
        .findByWebhookId(webhookId).stream().map(wtm -> jpaEntityDynamicMappingRepository
            .findById(wtm.getEntityMappingId()).map(mappingMapper::toDomain).orElse(null))
        .filter(Objects::nonNull).toList();
  }

  /// Persists the webhook's template mappings in the webhook_template_mapping
  /// table.
  /// This also persists each EntityDynamicMapping if it's new.
  private void persistTemplateMappings(UUID webhookId, WebhookConnector connector) {
    jpaWebhookTemplateMappingRepository.deleteByWebhookId(webhookId);
    var mappings = connector.mappings().stream()
        .map(mapping -> persistAndCreateTemplateMapping(webhookId, mapping)).toList();

    if (!mappings.isEmpty()) {
      jpaWebhookTemplateMappingRepository.saveAll(mappings);
    }
  }

  /// Persists a single EntityDynamicMapping and creates a
  /// WebhookTemplateMappingJpaEntity link.
  ///
  /// The mapping is expected to already exist because it is created through the
  /// dedicated inbound dynamic mapping endpoint. This method only creates the
  /// association row in webhook_template_mapping.
  private WebhookTemplateMappingJpaEntity persistAndCreateTemplateMapping(UUID webhookId,
      EntityDynamicMapping mapping) {
    EntityTemplate entityTemplate = entityTemplateRepositoryPort
        .findByIdentifier(mapping.templateIdentifier()).orElseThrow(
            () -> new EntityTemplateNotFoundException("identifier", mapping.templateIdentifier()));

    EntityDynamicMapping entityDynamicMapping = entityDynamicMappingPort
        .findByIdentifier(mapping.identifier())
        .orElseThrow(() -> new EntityDynamicMappingNotFoundException(mapping.identifier()));

    return WebhookTemplateMappingJpaEntity.builder().webhookId(webhookId)
        .templateId(entityTemplate.id()).entityMappingId(entityDynamicMapping.id())
        .jsltFilter(mapping.filter()).build();
  }
}
