package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "relation")
@Audited(withModifiedFlag = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  @EqualsAndHashCode.Include
  private String name;

  @Column(name = "target_template_identifier", nullable = false)
  private String targetTemplateIdentifier;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "relation_target_entities", schema = "idp_core", joinColumns = @JoinColumn(name = "relation_id"))
  @BatchSize(size = 50)
  private Set<RelationTargetJpaEntity> targetEntities;
}
