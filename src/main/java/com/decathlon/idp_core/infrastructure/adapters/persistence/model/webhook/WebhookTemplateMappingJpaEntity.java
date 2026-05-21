package com.decathlon.idp_core.infrastructure.adapters.persistence.model.webhook;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// JPA entity for the `webhook_template_mapping` table.
///
/// Stores the link between a webhook connector and the entity template referenced
/// by one inbound mapping rule.
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

    @Column(name = "webhook_id", nullable = false)
    private UUID webhookId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "jslt_filter", columnDefinition = "TEXT")
    private String jsltFilter;
}
