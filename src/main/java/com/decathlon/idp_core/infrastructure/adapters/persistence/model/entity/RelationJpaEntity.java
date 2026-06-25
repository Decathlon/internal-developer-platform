package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "relation")
@Audited
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "target_template_identifier", nullable = false)
  private String targetTemplateIdentifier;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "relation_target_entities", schema = "idp_core", joinColumns = @JoinColumn(name = "relation_id"))
  @BatchSize(size = 50)
  private List<RelationTargetJpaEntity> targetEntities;
}
