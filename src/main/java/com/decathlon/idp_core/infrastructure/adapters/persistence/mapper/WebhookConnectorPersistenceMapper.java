package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import com.decathlon.idp_core.domain.model.webhook.WebhookConnector;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common.WebhookConnectorJsonbHelper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook.WebhookConnectorJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

/**
 * MapStruct persistence mapper for {@link WebhookConnector}.
 *
 * <p>Follows the same contract as the other persistence mappers in the project:
 * domain model ↔ JPA entity, with JSONB conversions delegated to a dedicated helper.
 */
@Mapper(componentModel = SPRING, uses = WebhookConnectorJsonbHelper.class)
public interface WebhookConnectorPersistenceMapper {

    @Mapping(target = "mappings", qualifiedByName = "jsonToMappings")
    @Mapping(target = "security", qualifiedByName = "jsonToSecurity")
    WebhookConnector toDomain(WebhookConnectorJpaEntity jpa);

    @Mapping(target = "mappings", qualifiedByName = "mappingsToJson")
    @Mapping(target = "security", qualifiedByName = "securityToJson")
    WebhookConnectorJpaEntity toJpa(WebhookConnector domain);
}
