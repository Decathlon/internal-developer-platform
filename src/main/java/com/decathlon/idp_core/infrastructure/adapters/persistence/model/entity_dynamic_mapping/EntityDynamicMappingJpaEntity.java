package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_dynamic_mapping;

import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.EntityTemplateJpaEntity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/// JPA entity for the `entity_dynamic_mapping` table.
///
/// Stores dynamic mapping configurations used by webhook connectors to transform
/// inbound events into entities (JSLT filters, property/relation mappings).
@Entity
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
@Table(name = "entity_dynamic_mapping")
@NoArgsConstructor
@AllArgsConstructor
public class EntityDynamicMappingJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @ToString.Include
  private UUID id;

  @Column(name = "identifier", nullable = false, unique = true)
  @ToString.Include
  private String identifier;

  /// Foreign key to the parent entity entityTemplateIdentifier
  /// (`entity_template.id`).
  ///
  /// Persisted as the entityTemplateIdentifier UUID even though the public API
  /// (DTO In) exposes
  /// the entityTemplateIdentifier business identifier. The identifier is resolved
  /// to this UUID
  /// in the persistence adapter before saving.
  @Column(name = "template_id", nullable = false)
  private UUID entityTemplateId;

  /// Lazy, read-only navigation to the referenced entityTemplateIdentifier.
  ///
  /// Used to expose the entityTemplateIdentifier business identifier when mapping
  /// back to the
  /// domain model, without triggering an additional explicit query. Marked as
  /// non-insertable/non-updatable because the `template_id` column above owns
  /// the foreign key.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", insertable = false, updatable = false)
  @EqualsAndHashCode.Exclude
  private EntityTemplateJpaEntity template;

  @Column(nullable = false)
  private String filter;

  @Column(nullable = false)
  @ToString.Include
  private String name;

  @ToString.Include
  private String description;

  @Column(name = "entity_identifier", nullable = false)
  private String entityIdentifier;

  @Column(name = "entity_name", nullable = false)
  private String entityName;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String properties;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String relations;
}
