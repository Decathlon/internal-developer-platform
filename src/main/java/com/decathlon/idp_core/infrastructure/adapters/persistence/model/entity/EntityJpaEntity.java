package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@jakarta.persistence.Entity
@Data
@Table(name = "entity")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_identifier")
    private String templateIdentifier;

    private String name;

    private String identifier;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "entity_properties",
        joinColumns = @JoinColumn(name = "entity_id"),
        inverseJoinColumns = @JoinColumn(name = "property_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"entity_id", "property_id"}),
        indexes = @Index(columnList = "entity_id"))
    private List<PropertyJpaEntity> properties;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "entity_relations",
        joinColumns = @JoinColumn(name = "entity_id"),
        inverseJoinColumns = @JoinColumn(name = "relation_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"entity_id", "relation_id"}),
        indexes = @Index(columnList = "entity_id"))
    private List<RelationJpaEntity> relations;
}
