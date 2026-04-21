package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template;

import java.util.Collections;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@EqualsAndHashCode
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

    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "entity_template_properties_definitions",
        joinColumns = @JoinColumn(name = "entity_template_id"),
        inverseJoinColumns = @JoinColumn(name = "properties_definitions_id"))
    @OrderBy("name ASC")
    private Set<PropertyDefinitionJpaEntity> propertiesDefinitions = new LinkedHashSet<>();

    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "entity_template_relations_definitions",
        joinColumns = @JoinColumn(name = "entity_template_id"),
        inverseJoinColumns = @JoinColumn(name = "relations_definitions_id"))
    @OrderBy("name ASC")
    private Set<RelationDefinitionJpaEntity> relationsDefinitions = new LinkedHashSet<>();

    /// Returns an unmodifiable view of the internal collection to prevent external mutation.
    public Set<PropertyDefinitionJpaEntity> getPropertiesDefinitions() {
        return Collections.unmodifiableSet(propertiesDefinitions);
    }

    /// Defensive copy setter to prevent external mutation of the internal collection.
    public void setPropertiesDefinitions(Set<PropertyDefinitionJpaEntity> propertiesDefinitions) {
        this.propertiesDefinitions.clear();
        if (propertiesDefinitions != null) {
            this.propertiesDefinitions.addAll(propertiesDefinitions);
        }
    }

    /// Returns an unmodifiable view of the internal collection to prevent external mutation.
    public Set<RelationDefinitionJpaEntity> getRelationsDefinitions() {
        return Collections.unmodifiableSet(relationsDefinitions);
    }

    /// Defensive copy setter to prevent external mutation of the internal collection.
    public void setRelationsDefinitions(Set<RelationDefinitionJpaEntity> relationsDefinitions) {
        this.relationsDefinitions.clear();
        if (relationsDefinitions != null) {
            this.relationsDefinitions.addAll(relationsDefinitions);
        }
    }
}
