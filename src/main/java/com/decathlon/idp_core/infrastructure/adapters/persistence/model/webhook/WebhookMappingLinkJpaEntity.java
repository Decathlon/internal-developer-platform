package com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook;

import java.util.UUID;

import jakarta.persistence.*;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_dynamic_mapping.EntityDynamicMappingJpaEntity;

import lombok.*;

/// JPA entity for the webhook mapping link, representing the association between a webhook connector,
/// an entity template, and the dynamic mapping (with JSLT filter) to apply during ingestion.
///
/// The table uses a composite primary key `(webhook_id, entity_mapping_id)`; there is no
/// surrogate id column.
@Entity
@Table(name = "webhook_mapping_link")
@IdClass(WebhookMappingLinkJpaEntity.WebhookMappingLinkId.class)
@Getter
@Setter
@Builder
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class WebhookMappingLinkJpaEntity {

  /// Foreign key to the parent webhook connector. Part of the composite primary
  /// key.
  @Id
  @ToString.Include
  @Column(name = "webhook_id", nullable = false)
  private UUID webhookId;

  /// Foreign key to the dynamic mapping configuration. Part of the composite
  /// primary key.
  @Id
  @ToString.Include
  @Column(name = "entity_mapping_id", nullable = false)
  private UUID entityMappingId;

  /// The JSLT filter expression applied during event ingestion.
  /// Typically derived from the dynamic mapping configuration, but stored here
  /// for direct access and querying.
  ///
  @ToString.Include
  @Column(name = "jslt_filter", columnDefinition = "TEXT")
  private String jsltFilter;

  /// Lazy-loaded relationship to the webhook connector (optional, for
  /// navigation).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "webhook_id", nullable = false, insertable = false, updatable = false)
  private WebhookConnectorJpaEntity webhookConnector;

  /// Lazy-loaded relationship to the dynamic mapping (optional, for navigation).
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entity_mapping_id", nullable = false, insertable = false, updatable = false)
  private EntityDynamicMappingJpaEntity entityMapping;

  /// Composite primary key class for [WebhookMappingLinkJpaEntity].
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class WebhookMappingLinkId implements java.io.Serializable {
    private UUID webhookId;
    private UUID entityMappingId;
  }
}
