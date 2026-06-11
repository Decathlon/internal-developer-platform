package com.decathlon.idp_core.domain.service.entity;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.model.entity.EntityAuditInfo;
import com.decathlon.idp_core.domain.port.audit.EntityAuditPort;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;

import lombok.RequiredArgsConstructor;

/// Domain service for retrieving entity audit information.
///
/// **Business purpose:** Provides access to the audit trail of entities,
/// enabling compliance, debugging, and historical analysis. This service
/// orchestrates audit data retrieval while ensuring template existence
/// validation.
///
/// **Key responsibilities:**
/// - Retrieve audit history for entities including deleted ones
/// - Validate template existence before returning audit data
/// - Transform technical audit data into business-meaningful information
@Service
@RequiredArgsConstructor
public class EntityAuditService {

  private final EntityAuditPort entityAuditPort;
  private final EntityTemplateService entityTemplateService;

  /// Retrieves the complete audit history for a specific entity.
  ///
  /// **Business rule:** The template must exist to retrieve entity audit history.
  /// This method allows retrieving audit history for deleted entities as well,
  /// since the audit trail is stored independently.
  ///
  /// @param templateIdentifier the template identifier of the entity
  /// @param entityIdentifier the unique identifier of the entity
  /// @return list of audit information ordered by revision number (newest first)
  /// @throws
  /// com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException
  /// if the template does not exist
  @Transactional(readOnly = true)
  public List<EntityAuditInfo> getEntityAuditHistory(String templateIdentifier,
      String entityIdentifier) {
    entityTemplateService.getEntityTemplateByIdentifier(templateIdentifier);
    return entityAuditPort.getEntityAuditHistory(templateIdentifier, entityIdentifier);
  }
}
