package com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.*;

/// JPA entity mapping to the `webhook_connector` PostgreSQL table.
///
/// The `security` JSONB column is stored as a raw JSON string and deserialized
/// in [WebhookConnectorPersistenceMapper] using Jackson.
/// The webhook security payload follows the generic `{ type, config }` contract at the adapter boundary.
@Entity
@Table(name = "webhook_connector")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConnectorJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /// Business key used in the webhook URL: POST /webhooks/{identifier}
  @Column(nullable = false, unique = true, length = 255)
  private String identifier;

  /// Human-readable name displayed in the management UI
  @Column(nullable = false, length = 255)
  private String title;

  /// Optional description of the connector purpose
  @Column(columnDefinition = "TEXT")
  private String description;

  /// When false, the connector rejects all inbound events without processing them
  @Column(nullable = false)
  private Boolean enabled;

  /// JSONB security configuration — deserialized to WebhookSecurity by the
  /// mapper.
  /// The "type" discriminator field drives polymorphic deserialization.
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String security;

  /// Timestamp of connector creation
  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  /// Timestamp of last connector update
  @LastModifiedDate
  @Column(nullable = false)
  private Instant updatedAt;
}
