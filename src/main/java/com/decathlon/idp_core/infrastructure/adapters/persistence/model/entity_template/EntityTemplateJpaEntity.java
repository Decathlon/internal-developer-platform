package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "entity_template")
@NoArgsConstructor
@AllArgsConstructor
public class EntityTemplateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String identifier;

    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "entity_template_properties_definitions",
        joinColumns = @JoinColumn(name = "entity_template_id"),
        inverseJoinColumns = @JoinColumn(name = "properties_definitions_id"))
    @OrderBy("name ASC")
    private Set<PropertyDefinitionJpaEntity> propertiesDefinitions = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "entity_template_relations_definitions",
        joinColumns = @JoinColumn(name = "entity_template_id"),
        inverseJoinColumns = @JoinColumn(name = "relations_definitions_id"))
    @OrderBy("name ASC")
    private Set<RelationDefinitionJpaEntity> relationsDefinitions = new LinkedHashSet<>();
}
