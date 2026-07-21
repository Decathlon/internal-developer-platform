package com.decathlon.idp_core.domain.service.entity_dynamic_mapping;

import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingAlreadyInUseException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.domain.port.WebhookMappingLinkPort;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class EntityDynamicMappingService {

  private final EntityDynamicMappingPort entityDynamicMappingPort;
  private final WebhookMappingLinkPort webhookTemplateMappingPort;
  private final EntityDynamicMappingValidationService entityDynamicMappingValidationService;

  @Transactional
  public EntityDynamicMapping createEntityDynamicMapping(
      EntityDynamicMapping entityDynamicMapping) {
    validateIdentifierUniqueness(entityDynamicMapping.identifier());
    entityDynamicMappingValidationService.validateMapping(entityDynamicMapping);
    return entityDynamicMappingPort.save(entityDynamicMapping);
  }

  public Page<EntityDynamicMapping> getAllEntityDynamicMapping(Pageable pageable) {
    return entityDynamicMappingPort.findAll(pageable);
  }

  @Transactional
  public void deleteEntityDynamicMapping(String entityDynamicMappingIdentifier) {
    validateIdentifierExists(entityDynamicMappingIdentifier);
    UUID dynamicMappingIdentifier = entityDynamicMappingPort
        .findByIdentifier(entityDynamicMappingIdentifier).orElseThrow(
            () -> new EntityDynamicMappingNotFoundException(entityDynamicMappingIdentifier))
        .id();
    validateIsNotInUse(dynamicMappingIdentifier);
    entityDynamicMappingPort.deleteByIdentifier(entityDynamicMappingIdentifier);
  }

  private void validateIsNotInUse(UUID entityDynamicMappingId) {
    if (webhookTemplateMappingPort.existsByEntityMappingId(entityDynamicMappingId)) {
      List<WebhookTemplateMapping> webhookTemplateMappingList = webhookTemplateMappingPort
          .findByEntityMappingId(entityDynamicMappingId);
      List<String> webhookIdentifiers = webhookTemplateMappingList.stream()
          .map(WebhookTemplateMapping::webhookConnector)
          .filter(webhook -> webhook != null && webhook.identifier() != null)
          .map(WebhookConnector::identifier).distinct().toList();
      throw new EntityDynamicMappingAlreadyInUseException(webhookIdentifiers);
    }
  }

  private void validateIdentifierExists(String entityDynamicMappingIdentifier) {
    if (!entityDynamicMappingPort.existsByIdentifier(entityDynamicMappingIdentifier)) {
      throw new EntityDynamicMappingNotFoundException(entityDynamicMappingIdentifier);
    }
  }

  /// Ensures no other dynamic mapping already uses the provided identifier.
  ///
  /// This enforces the `entity_dynamic_mapping_identifier_key` unique constraint
  /// at the domain level, returning a meaningful conflict instead of letting the
  /// database raise a low-level integrity violation.
  ///
  /// @param identifier the candidate mapping identifier
  /// @throws EntityDynamicMappingAlreadyExistsException when the identifier is
  /// already used
  private void validateIdentifierUniqueness(String identifier) {
    if (entityDynamicMappingPort.existsByIdentifier(identifier)) {
      throw new EntityDynamicMappingAlreadyExistsException(identifier);
    }
  }

  public EntityDynamicMapping getEntityDynamicMapping(String identifier) {
    return entityDynamicMappingPort.findByIdentifier(identifier)
        .orElseThrow(() -> new EntityDynamicMappingNotFoundException(identifier));
  }

  @Transactional
  public EntityDynamicMapping updateEntityDynamicMapping(String identifier,
      @Valid EntityDynamicMapping entityDynamicMapping) {
    EntityDynamicMapping existingMapping = getEntityDynamicMapping(identifier);
    entityDynamicMappingValidationService.validateMapping(entityDynamicMapping);

    EntityDynamicMapping mergedMapping = new EntityDynamicMapping(existingMapping.id(),
        existingMapping.identifier(), entityDynamicMapping.entityTemplateIdentifier(),
        entityDynamicMapping.filter(), entityDynamicMapping.name(),
        entityDynamicMapping.description(), entityDynamicMapping.entityIdentifier(),
        entityDynamicMapping.entityName(), entityDynamicMapping.properties(),
        entityDynamicMapping.relations());

    return entityDynamicMappingPort.save(mergedMapping);
  }

}
