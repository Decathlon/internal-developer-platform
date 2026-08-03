package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import java.util.UUID;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookMappingLinkJpaEntity;

/// Persistence mapper for [WebhookTemplateMapping].
///
/// Maps the association entity between webhook connector, entity entityTemplateIdentifier and
/// dynamic mapping configuration. Foreign keys are managed explicitly by adapters
/// when persisting new links.
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {
    WebhookConnectorPersistenceMapper.class, EntityTemplatePersistenceMapper.class,
    EntityDynamicMappingPersistenceMapper.class}, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WebhookMappingLinkPersistenceMapper {

  /// Maps JPA association data to the domain model.
  ///
  /// @param jpa persisted association entity
  /// @return mapped domain model
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "webhookConnector", source = "webhookConnector")
  @Mapping(target = "entityDynamicMapping", source = "entityMapping")
  @Mapping(target = "jsltFilter", source = "jsltFilter")
  WebhookTemplateMapping toDomain(WebhookMappingLinkJpaEntity jpa);

  /// Maps domain model to JPA association entity.
  ///
  /// All technical IDs are preserved from the domain model.
  ///
  /// @param domain domain mapping object
  /// @return fully mapped JPA association entity
  @Mapping(target = "webhookId", source = "webhookConnector.id")
  @Mapping(target = "entityMappingId", source = "entityDynamicMapping.id")
  @Mapping(target = "jsltFilter", source = "jsltFilter")
  @Mapping(target = "webhookConnector", ignore = true)
  @Mapping(target = "entityMapping", ignore = true)
  WebhookMappingLinkJpaEntity toJpa(WebhookTemplateMapping domain);

  /// Builds a link row with explicit foreign keys.
  ///
  /// @param webhookId webhook connector technical id
  /// @param entityMappingId dynamic mapping technical id
  /// @param jsltFilter JSLT filter expression
  /// @return link entity ready for persistence
  default WebhookMappingLinkJpaEntity toJpa(UUID webhookId, UUID entityMappingId,
      String jsltFilter) {
    return WebhookMappingLinkJpaEntity.builder().webhookId(webhookId)
        .entityMappingId(entityMappingId).jsltFilter(jsltFilter).build();
  }
}
