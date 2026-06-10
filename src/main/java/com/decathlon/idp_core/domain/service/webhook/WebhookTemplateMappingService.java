package com.decathlon.idp_core.domain.service.webhook;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_ALREADY_MAPPED_WEBHOOK;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateInUseByWebhookMappingException;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;
import com.decathlon.idp_core.domain.port.WebhookTemplateMappingPort;

import lombok.RequiredArgsConstructor;

/// Domain service for webhook template mapping operations.
///
/// Validates template usage in webhook mappings before deletion.
@Service
@Validated
@RequiredArgsConstructor
public class WebhookTemplateMappingService {

  private final WebhookTemplateMappingPort webhookTemplateMappingPort;

  /// Retrieves all mappings for a given entity template.
  ///
  /// @param templateId template technical UUID
  /// @return list of associated webhook template mappings
  public List<WebhookTemplateMapping> findByTemplateId(UUID templateId) {
    return webhookTemplateMappingPort.findByTemplateId(templateId);
  }

  /// Validates that a template is not in use by any webhook mapping.
  ///
  /// @param entityTemplateId the entity template UUID to check
  /// @throws EntityTemplateInUseByWebhookMappingException if template is already
  /// in use
  public void validateTemplateNotInUseMapping(UUID entityTemplateId) {
    List<WebhookTemplateMapping> mappings = findByTemplateId(entityTemplateId);
    if (!mappings.isEmpty()) {
      List<String> webhookIds = mappings.stream().map(WebhookTemplateMapping::webhookConnector)
          .filter(webhook -> webhook != null && webhook.id() != null)
          .map(WebhookConnector::identifier).distinct().toList();
      throw new EntityTemplateInUseByWebhookMappingException(
          TEMPLATE_ALREADY_MAPPED_WEBHOOK.formatted(webhookIds));
    }
  }
}
