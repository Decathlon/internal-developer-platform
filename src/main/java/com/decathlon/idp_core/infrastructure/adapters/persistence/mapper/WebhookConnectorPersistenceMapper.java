package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common.WebhookConnectorJsonbHelper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookConnectorJpaEntity;

/// MapStruct persistence mapper for [WebhookConnector].
///
/// Maps the connector's direct fields (identifier, name, description, enabled, security).
/// The mappings list is handled separately by
/// [com.decathlon.idp_core.infrastructure.adapters.persistence.PostgresWebhookConnectorAdapter]
/// through the `webhook_template_mapping` table because it requires dedicated persistence
/// for `entity_dynamic_mapping` rows.
@Mapper(componentModel = SPRING, uses = WebhookConnectorJsonbHelper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface WebhookConnectorPersistenceMapper {

  @Mapping(target = "mappings", ignore = true)
  @Mapping(target = "security", qualifiedByName = "jsonToSecurity")
  WebhookConnector toDomain(WebhookConnectorJpaEntity jpa);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "security", qualifiedByName = "securityToJson")
  WebhookConnectorJpaEntity toJpa(WebhookConnector domain);
}
