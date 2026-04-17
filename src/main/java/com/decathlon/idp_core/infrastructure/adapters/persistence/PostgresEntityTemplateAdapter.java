package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.ports.EntityTemplateRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityTemplatePersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyRulesJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.EntityTemplateJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.PropertyDefinitionJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.RelationDefinitionJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityTemplateRepository;

import lombok.RequiredArgsConstructor;

/// PostgreSQL persistence adapter implementing [EntityTemplateRepositoryPort].
///
/// **Infrastructure specifics:**
/// - Uses JPA/Hibernate for object-relational mapping to PostgreSQL
/// - Implements optimized entity graph loading with `@EntityGraph` to prevent N+1 queries
/// - Handles domain-to-JPA mapping through [EntityTemplatePersistenceMapper]
/// - Manages entity relationship cascading and orphan removal
/// - Ensures transactional consistency for complex entity template operations
///
/// **Performance optimizations:**
/// - Entity graphs fetch properties and relations in single query
/// - Bulk operations minimize database round trips
/// - Lazy loading configured appropriately for relationship navigation
@Component
@RequiredArgsConstructor
public class PostgresEntityTemplateAdapter implements EntityTemplateRepositoryPort {
/// - Entity graphs fetch properties and relations in single query
/// - Bulk operations minimize database round trips
/// - Lazy loading configured appropriately for relationship navigation

    private final JpaEntityTemplateRepository jpaEntityTemplateRepository;
    private final EntityTemplatePersistenceMapper mapper;

    @Override
    public Optional<EntityTemplate> findByIdentifier(String templateIdentifier) {
        return jpaEntityTemplateRepository.findByIdentifier(templateIdentifier).map(mapper::toDomain);
    }

    @Override
    public Optional<EntityTemplate> findById(UUID id) {
        return jpaEntityTemplateRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<EntityTemplate> findAll(Pageable pageable) {
        return jpaEntityTemplateRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdentifier(String identifier) {
        return jpaEntityTemplateRepository.existsByIdentifier(identifier);
    }

    @Override
    public EntityTemplate save(EntityTemplate entityTemplate) {
        EntityTemplateJpaEntity jpaEntity;
        if (entityTemplate.id() != null) {
            // Update: fetch the managed JPA entity and merge in-place
            jpaEntity = jpaEntityTemplateRepository.findById(entityTemplate.id())
                    .orElseGet(() -> mapper.toJpa(entityTemplate));
            mergeIntoExisting(jpaEntity, entityTemplate);
        } else {
            jpaEntity = mapper.toJpa(entityTemplate);
        }
        return mapper.toDomain(jpaEntityTemplateRepository.save(jpaEntity));
    }

    @Override
    public void deleteByIdentifier(String identifier) {
        jpaEntityTemplateRepository.deleteByIdentifier(identifier);
    }

    // ── Merge helpers to update a managed JPA entity from domain values ──

    private void mergeIntoExisting(EntityTemplateJpaEntity jpa, EntityTemplate domain) {
        jpa.setIdentifier(domain.identifier());
        jpa.setDescription(domain.description());
        mergePropertyDefinitions(jpa, domain);
        mergeRelationDefinitions(jpa, domain);
    }

    private void mergePropertyDefinitions(EntityTemplateJpaEntity jpa, EntityTemplate domain) {
        // Work on a mutable copy — getter returns an unmodifiable view
        Set<PropertyDefinitionJpaEntity> existing = new LinkedHashSet<>(jpa.getPropertiesDefinitions());

        if (domain.propertiesDefinitions() == null) {
            jpa.setPropertiesDefinitions(new LinkedHashSet<>());
            return;
        }

        Map<String, PropertyDefinitionJpaEntity> existingByName = existing.stream()
                .collect(Collectors.toMap(PropertyDefinitionJpaEntity::getName, Function.identity()));

        Set<String> updatedNames = domain.propertiesDefinitions().stream()
                .map(p -> p.name())
                .collect(Collectors.toSet());

        // Remove properties no longer present
        existing.removeIf(p -> !updatedNames.contains(p.getName()));

        // Update existing or add new
        for (var domProp : domain.propertiesDefinitions()) {
            PropertyDefinitionJpaEntity ex = existingByName.get(domProp.name());
            if (ex != null) {
                ex.setDescription(domProp.description());
                ex.setType(domProp.type());
                ex.setRequired(domProp.required());
                mergeRules(ex, domProp.rules());
            } else {
                PropertyDefinitionJpaEntity newProp = PropertyDefinitionJpaEntity.builder()
                        .id(domProp.id())
                        .name(domProp.name())
                        .description(domProp.description())
                        .type(domProp.type())
                        .required(domProp.required())
                        .rules(domProp.rules() != null ? toRulesJpa(domProp.rules()) : null)
                        .build();
                existing.add(newProp);
            }
        }

        // Push the mutated copy back through the defensive setter
        jpa.setPropertiesDefinitions(existing);
    }

    private void mergeRules(PropertyDefinitionJpaEntity jpaProp,
                            com.decathlon.idp_core.domain.model.entity_template.PropertyRules domRules) {
        if (domRules == null) {
            // No rules in the updated domain – leave existing rules unchanged
            return;
        }
        PropertyRulesJpaEntity ex = jpaProp.getRules();
        if (ex != null) {
            // Update the managed entity in-place — Hibernate tracks the dirty fields
            ex.setFormat(domRules.format());
            ex.setEnumValues(domRules.enumValues() != null ? domRules.enumValues().toArray(new String[0]) : null);
            ex.setRegex(domRules.regex());
            ex.setMaxLength(domRules.maxLength());
            ex.setMinLength(domRules.minLength());
            ex.setMaxValue(domRules.maxValue());
            ex.setMinValue(domRules.minValue());
            // Re-set the reference so Hibernate detects the association as dirty
            jpaProp.setRules(ex);
        } else {
            jpaProp.setRules(toRulesJpa(domRules));
        }
    }

    private PropertyRulesJpaEntity toRulesJpa(com.decathlon.idp_core.domain.model.entity_template.PropertyRules d) {
        return PropertyRulesJpaEntity.builder()
                .id(d.id()).format(d.format())
                .enumValues(d.enumValues() != null ? d.enumValues().toArray(new String[0]) : null)
                .regex(d.regex()).maxLength(d.maxLength()).minLength(d.minLength())
                .maxValue(d.maxValue()).minValue(d.minValue()).build();
    }

    private void mergeRelationDefinitions(EntityTemplateJpaEntity jpa, EntityTemplate domain) {
        // Work on a mutable copy — getter returns an unmodifiable view
        Set<RelationDefinitionJpaEntity> existing = new LinkedHashSet<>(jpa.getRelationsDefinitions());

        if (domain.relationsDefinitions() == null) {
            jpa.setRelationsDefinitions(new LinkedHashSet<>());
            return;
        }

        Map<String, RelationDefinitionJpaEntity> existingByName = existing.stream()
                .collect(Collectors.toMap(RelationDefinitionJpaEntity::getName, Function.identity()));

        Set<String> updatedNames = domain.relationsDefinitions().stream()
                .map(r -> r.name())
                .collect(Collectors.toSet());

        // Remove relations no longer present
        existing.removeIf(r -> !updatedNames.contains(r.getName()));

        // Update existing or add new
        for (var domRel : domain.relationsDefinitions()) {
            RelationDefinitionJpaEntity ex = existingByName.get(domRel.name());
            if (ex != null) {
                ex.setTargetEntityIdentifier(domRel.targetEntityIdentifier());
                ex.setRequired(domRel.required());
                ex.setToMany(domRel.toMany());
            } else {
                RelationDefinitionJpaEntity newRel = RelationDefinitionJpaEntity.builder()
                        .name(domRel.name())
                        .targetEntityIdentifier(domRel.targetEntityIdentifier())
                        .required(domRel.required())
                        .toMany(domRel.toMany())
                        .build();
                existing.add(newRel);
            }
        }

        // Push the mutated copy back through the defensive setter
        jpa.setRelationsDefinitions(existing);
    }
}
