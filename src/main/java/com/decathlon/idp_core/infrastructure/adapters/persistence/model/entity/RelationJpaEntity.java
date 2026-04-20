package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "relation")
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

    @ElementCollection
    @CollectionTable(name = "relation_target_entities",
        joinColumns = @JoinColumn(name = "relation_id"),
        indexes = @Index(columnList = "relation_id"))
    @Column(name = "target_entity_identifier")
    private List<String> targetEntityIdentifiers;
}
