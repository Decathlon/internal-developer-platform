package com.decathlon.idp_core.domain.port.audit;

import java.util.List;

import com.decathlon.idp_core.domain.model.entity.EntityAuditInfo;

/// Port interface for retrieving entity audit information.
///
/// **Port contract:** Defines operations for accessing historical revision data
/// of entities. Implementations should interact with the audit storage system
/// (Hibernate Envers) to provide audit trail information.
///
/// **Hexagonal architecture:** This is a **driven port** (outbound), implemented
/// by infrastructure adapters and used by domain services to access audit data.
public interface EntityAuditPort {

  /// Retrieves all audit revisions for a specific entity.
  ///
  /// @param templateIdentifier the template identifier of the entity
  /// @param entityIdentifier the unique identifier of the entity
  /// @return list of audit information ordered by revision number (newest first)
  List<EntityAuditInfo> getEntityAuditHistory(String templateIdentifier, String entityIdentifier);

}
