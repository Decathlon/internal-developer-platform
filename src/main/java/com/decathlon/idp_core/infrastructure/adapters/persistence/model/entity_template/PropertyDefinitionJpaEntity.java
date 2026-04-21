package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyRulesJpaEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "property_definition")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDefinitionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private PropertyType type;

    @Builder.Default
    private boolean required = false;

    @OneToOne(cascade = CascadeType.ALL)
    private PropertyRulesJpaEntity rules;
}
