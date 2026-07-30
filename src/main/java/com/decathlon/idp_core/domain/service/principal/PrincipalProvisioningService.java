package com.decathlon.idp_core.domain.service.principal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;

import lombok.RequiredArgsConstructor;

/// Domain service orchestrating Just-In-Time (JIT) provisioning of Principal entities.
///
/// **Business purpose:** Ensures every authenticated principal (human or service account)
/// has a corresponding Entity in the catalog with template_identifier="principal".
/// This enables unified identity management and audit tracking across the platform.
///
/// **Key responsibilities:**
/// - Create Principal entities on first authentication
/// - Map authentication claims to entity properties and relations
/// - Maintain referential integrity with team/group entities
///
/// **Design rationale:** Separates authentication (infrastructure concern) from
/// identity management (business concern). The authentication layer extracts PrincipalInfo,
/// this service persists it as domain entities.
@Service
@RequiredArgsConstructor
public class PrincipalProvisioningService {

  private static final String PRINCIPAL_TEMPLATE_IDENTIFIER = "principal";

  private final EntityRepositoryPort entityRepository;

  /// Provisions a Principal entity based on authentication information.
  ///
  /// **Contract:** Performs JIT provisioning:
  /// - If principal does not exist: Creates new Principal entity
  /// - If principal exists: Returns it without modification (updates handled by
  /// separate endpoint)
  /// - Returns the provisioned Principal entity
  ///
  /// **Thread-safety:** Uses database constraints to handle concurrent first-time
  /// logins.
  /// If two requests race to create the same principal, one will succeed and the
  /// other
  /// will return the existing principal.
  ///
  /// @param principalInfo extracted authentication information
  /// @return the provisioned Principal entity
  @Transactional
  public Entity provisionPrincipal(PrincipalInfo principalInfo) {
    Optional<Entity> existingPrincipal = entityRepository.findByTemplateIdentifierAndIdentifier(
        PRINCIPAL_TEMPLATE_IDENTIFIER, principalInfo.identifier());

    if (existingPrincipal.isPresent()) {
      return existingPrincipal.get();
    }

    return createNewPrincipal(principalInfo);
  }

  /// Retrieves a Principal entity by its identifier.
  ///
  /// **Contract:** Returns the Principal entity if it exists, empty otherwise.
  ///
  /// @param identifier unique principal identifier
  /// @return optional containing the principal entity
  @Transactional(readOnly = true)
  public Optional<Entity> getPrincipal(String identifier) {
    return entityRepository.findByTemplateIdentifierAndIdentifier(PRINCIPAL_TEMPLATE_IDENTIFIER,
        identifier);
  }

  /// Creates a new Principal entity in the catalog.
  ///
  /// **Business logic:** Maps PrincipalInfo to entity properties and relations.
  /// If a concurrent creation occurs, the existing entity is returned.
  ///
  /// @param principalInfo extracted authentication information
  /// @return the newly created or existing Principal entity
  private Entity createNewPrincipal(PrincipalInfo principalInfo) {
    Entity newPrincipal = new Entity(null, PRINCIPAL_TEMPLATE_IDENTIFIER, principalInfo.name(),
        principalInfo.identifier(), buildProperties(principalInfo), buildRelations(principalInfo));
    try {
      return entityRepository.save(newPrincipal);
    } catch (DataIntegrityViolationException e) {
      return entityRepository.findByTemplateIdentifierAndIdentifier(PRINCIPAL_TEMPLATE_IDENTIFIER,
          principalInfo.identifier()).orElse(newPrincipal);
    }
  }

  /// Builds properties for the principal entity based on the provided
  /// PrincipalInfo.
  ///
  /// **Business logic:** Includes the principal kind and any non-blank
  /// attributes.
  ///
  /// @param principalInfo the principal information containing attributes
  /// @return list of properties for the principal entity
  private List<Property> buildProperties(PrincipalInfo principalInfo) {
    List<Property> properties = new ArrayList<>();
    properties.add(new Property(null, "kind", principalInfo.kind().name()));

    principalInfo.attributes().forEach((key, value) -> {
      if (value != null && !value.isBlank()) {
        properties.add(new Property(null, key, value));
      }
    });

    return properties;
  }

  /// Builds relations for the principal based on group memberships.
  ///
  /// **Business logic:** Only includes relations to existing teams. If a group
  /// does not correspond to a team entity, it is ignored.
  ///
  /// @param principalInfo the principal information containing group memberships
  /// @return list of relations to existing teams
  private List<Relation> buildRelations(PrincipalInfo principalInfo) {
    if (principalInfo.groups().isEmpty()) {
      return List.of();
    }

    // Batch query to find all existing teams in one DB round-trip
    List<String> validGroups = entityRepository
        .findAllByTemplateIdentifierAndIdentifierIn("team", principalInfo.groups()).stream()
        .map(Entity::identifier).toList();

    if (validGroups.isEmpty()) {
      return List.of();
    }

    return List.of(new Relation(null, "member_of", "team", validGroups));
  }
}
