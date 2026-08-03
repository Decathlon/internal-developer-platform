package com.decathlon.idp_core.domain.port;

import java.util.List;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;

public interface WebhookMappingLinkPort {

  boolean existsByEntityMappingId(UUID id);

  List<WebhookTemplateMapping> findByEntityMappingId(UUID id);

}
