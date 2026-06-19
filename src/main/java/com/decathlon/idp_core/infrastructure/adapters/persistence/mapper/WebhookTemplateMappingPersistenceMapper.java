package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import java.util.UUID;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookTemplateMappingJpaEntity;

/// Persistence mapper for [WebhookTemplateMapping].
///
/// Maps the association entity between webhook connector, entity template and
/// dynamic mapping configuration. Foreign keys are managed explicitly by adapters
/// when persisting new links.
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {
    WebhookConnectorPersistenceMapper.class, EntityTemplatePersistenceMapper.class,
    EntityDynamicMappingPersistenceMapper.class}, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WebhookTemplateMappingPersistenceMapper {

  /// Maps JPA association data to the domain model.
  ///
  /// @param jpa persisted association entity
  /// @return mapped domain model
  @Mapping(target = "id", source = "id")
  @Mapping(target = "webhookConnector", source = "webhookConnector")
  @Mapping(target = "entityTemplate", source = "entityTemplate")
  @Mapping(target = "entityDynamicMapping", source = "entityMapping")
  @Mapping(target = "jsltFilter", source = "jsltFilter")
  WebhookTemplateMapping toDomain(WebhookTemplateMappingJpaEntity jpa);

  /// Maps domain model to JPA association entity.
  ///
  /// All technical IDs are preserved from the domain model.
  ///
  /// @param domain domain mapping object
  /// @return fully mapped JPA association entity
  @Mapping(target = "id", source = "id")
  @Mapping(target = "webhookId", source = "webhookConnector.id")
  @Mapping(target = "templateId", source = "entityTemplate.id")
  @Mapping(target = "entityMappingId", source = "entityDynamicMapping.id")
  @Mapping(target = "jsltFilter", source = "jsltFilter")
  @Mapping(target = "webhookConnector", ignore = true)
  @Mapping(target = "entityTemplate", ignore = true)
  @Mapping(target = "entityMapping", ignore = true)
  WebhookTemplateMappingJpaEntity toJpa(WebhookTemplateMapping domain);

  /// Builds a link row with explicit foreign keys.
  ///
  /// @param webhookId webhook connector technical id
  /// @param templateId target entity template technical id
  /// @param entityMappingId dynamic mapping technical id
  /// @param jsltFilter JSLT filter expression
  /// @return link entity ready for persistence
  default WebhookTemplateMappingJpaEntity toJpa(UUID webhookId, UUID templateId,
      UUID entityMappingId, String jsltFilter) {
    return WebhookTemplateMappingJpaEntity.builder().webhookId(webhookId).templateId(templateId)
        .entityMappingId(entityMappingId).jsltFilter(jsltFilter).build();
  }
}
