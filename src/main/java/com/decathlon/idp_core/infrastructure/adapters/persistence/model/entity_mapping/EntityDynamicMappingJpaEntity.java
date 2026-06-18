package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_mapping;

import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.*;

/// JPA entity for the `entity_dynamic_mapping` table.
///
/// Stores dynamic mapping configurations used by webhook connectors to transform
/// inbound events into entities (JSLT filters, property/relation mappings).
@Entity
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Table(name = "entity_dynamic_mapping")
@NoArgsConstructor
@AllArgsConstructor
public class EntityDynamicMappingJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "identifier", nullable = false, unique = true)
  private String identifier;

  @Column(name = "template_identifier", nullable = false)
  private String templateIdentifier;

  @Column(nullable = false)
  private String filter;

  @Column(name = "entity_identifier", nullable = false)
  private String entityIdentifier;

  @Column(name = "entity_title", nullable = false)
  private String entityTitle;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String properties;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String relations;
}
