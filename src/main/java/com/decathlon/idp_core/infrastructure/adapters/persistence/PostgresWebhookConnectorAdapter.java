package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityDynamicMappingPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.WebhookConnectorPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_dynamic_mapping.EntityDynamicMappingJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookConnectorJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookMappingLinkJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityDynamicMappingRepository;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaWebhookConnectorRepository;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.WebhookMappingLinkRepository;

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
  private final WebhookMappingLinkRepository jpaWebhookTemplateMappingRepository;
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
    Page<WebhookConnectorJpaEntity> jpaPage = jpaWebhookConnectorRepository.findAll(pageable);

    if (jpaPage.isEmpty()) {
      return jpaPage.map(mapper::toDomain);
    }

    // Collect all webhook IDs from the page
    List<UUID> webhookIds = jpaPage.stream().map(WebhookConnectorJpaEntity::getId).toList();

    // Batch load all entityTemplateIdentifier mappings for all webhooks in the page
    List<WebhookMappingLinkJpaEntity> allTemplateMappings = jpaWebhookTemplateMappingRepository
        .findByWebhookIdIn(webhookIds);

    // Collect all unique mapping IDs
    List<UUID> allMappingIds = allTemplateMappings.stream()
        .map(WebhookMappingLinkJpaEntity::getEntityMappingId).distinct().toList();

    // Batch load all entity mappings in one query
    Map<UUID, EntityDynamicMapping> allMappingsById = jpaEntityDynamicMappingRepository
        .findAllById(allMappingIds).stream()
        .collect(Collectors.toMap(EntityDynamicMappingJpaEntity::getId, mappingMapper::toDomain,
            (existing, replacement) -> existing));

    // Group mappings by webhook ID
    Map<UUID, List<EntityDynamicMapping>> mappingsByWebhookId = allTemplateMappings.stream()
        .collect(Collectors.groupingBy(WebhookMappingLinkJpaEntity::getWebhookId,
            Collectors.mapping(wtm -> allMappingsById.get(wtm.getEntityMappingId()),
                Collectors.filtering(Objects::nonNull, Collectors.toList()))));

    // Map each JPA entity to domain with its mappings
    return jpaPage.map(jpaEntity -> {
      WebhookConnector connectorWithoutMappings = mapper.toDomain(jpaEntity);
      List<EntityDynamicMapping> mappings = mappingsByWebhookId.getOrDefault(jpaEntity.getId(),
          Collections.emptyList());

      return new WebhookConnector(connectorWithoutMappings.id(),
          connectorWithoutMappings.identifier(), connectorWithoutMappings.name(),
          connectorWithoutMappings.description(), connectorWithoutMappings.enabled(), mappings,
          connectorWithoutMappings.security());
    });
  }

  @Override
  public boolean existsByIdentifier(String identifier) {
    return jpaWebhookConnectorRepository.existsByIdentifier(identifier);
  }

  @Override
  public boolean existsByTitle(String title) {
    return jpaWebhookConnectorRepository.existsByName(title);
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
        connectorWithoutMappings.identifier(), connectorWithoutMappings.name(),
        connectorWithoutMappings.description(), connectorWithoutMappings.enabled(), mappings,
        connectorWithoutMappings.security());
  }

  /// Loads all dynamic mappings associated with a webhook connector.
  /// Uses batch loading to avoid N+1 query problem.
  private List<EntityDynamicMapping> loadMappingsForWebhook(UUID webhookId) {
    List<WebhookMappingLinkJpaEntity> templateMappings = jpaWebhookTemplateMappingRepository
        .findByWebhookId(webhookId);
    List<UUID> mappingIds = templateMappings.stream()
        .map(WebhookMappingLinkJpaEntity::getEntityMappingId).toList();
    if (mappingIds.isEmpty()) {
      return List.of();
    }
    Map<UUID, EntityDynamicMapping> mappingsById = jpaEntityDynamicMappingRepository
        .findAllById(mappingIds).stream()
        .collect(Collectors.toMap(EntityDynamicMappingJpaEntity::getId, mappingMapper::toDomain));
    return mappingIds.stream().map(mappingsById::get).filter(Objects::nonNull).toList();
  }

  /// Persists the webhook's entityTemplateIdentifier mappings in the
  /// webhook_template_mapping
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
  private WebhookMappingLinkJpaEntity persistAndCreateTemplateMapping(UUID webhookId,
      EntityDynamicMapping mapping) {

    if (mapping.id() == null) {
      throw new EntityDynamicMappingNotFoundException(mapping.identifier());
    }

    return WebhookMappingLinkJpaEntity.builder().webhookId(webhookId).entityMappingId(mapping.id())
        .jsltFilter(mapping.filter()).build();
  }
}
