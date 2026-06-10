package com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook;

import java.util.UUID;

import jakarta.persistence.*;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_mapping.EntityDynamicMappingJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.EntityTemplateJpaEntity;

import lombok.*;

/// JPA entity for the webhook template mapping, representing the association between a webhook connector,
/// Stores the relationship between a webhook connector, an entity template,
/// and the dynamic mapping (with JSLT filter) to apply during ingestion.
@Entity
@Table(name = "webhook_template_mapping")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookTemplateMappingJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /// Foreign key to the parent webhook connector.
  /// Stored as a direct column for query performance.
  ///
  @Column(name = "webhook_id", nullable = false)
  private UUID webhookId;

  /// Foreign key to the entity template.
  /// Stored as a direct column for query performance.
  @Column(name = "template_id", nullable = false)
  private UUID templateId;

  /// Foreign key to the dynamic mapping configuration.
  /// Stored as a direct column for query performance.
  @Column(name = "entity_mapping_id", nullable = false)
  private UUID entityMappingId;

  /// The JSLT filter expression applied during event ingestion.
  /// Typically derived from the dynamic mapping configuration, but stored here
  /// for direct access and querying.
  @Column(name = "jslt_filter", columnDefinition = "TEXT")
  private String jsltFilter;

  /// Lazy-loaded relationship to the webhook connector (optional, for
  /// navigation).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "webhook_id", nullable = false, insertable = false, updatable = false)
  private WebhookConnectorJpaEntity webhookConnector;

  /// Lazy-loaded relationship to the entity template (optional, for navigation).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id", nullable = false, insertable = false, updatable = false)
  private EntityTemplateJpaEntity entityTemplate;

  /// Lazy-loaded relationship to the dynamic mapping (optional, for navigation).
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entity_mapping_id", nullable = false, insertable = false, updatable = false)
  private EntityDynamicMappingJpaEntity entityMapping;
}
