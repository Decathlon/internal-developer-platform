package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "relation_definition")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationDefinitionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    private String name;

    private String targetTemplateIdentifier;

    @Builder.Default
    private boolean required = false;

    @Builder.Default
    private boolean toMany = false;
}
