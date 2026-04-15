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

@Component
@RequiredArgsConstructor
public class PostgresEntityTemplateAdapter implements EntityTemplateRepositoryPort {

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
        if (entityTemplate.getId() != null) {
            // Update: fetch the managed JPA entity and merge in-place
            jpaEntity = jpaEntityTemplateRepository.findById(entityTemplate.getId())
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
        jpa.setIdentifier(domain.getIdentifier());
        jpa.setDescription(domain.getDescription());
        mergePropertyDefinitions(jpa, domain);
        mergeRelationDefinitions(jpa, domain);
    }

    private void mergePropertyDefinitions(EntityTemplateJpaEntity jpa, EntityTemplate domain) {
        Set<PropertyDefinitionJpaEntity> existing = jpa.getPropertiesDefinitions();
        if (existing == null) {
            existing = new LinkedHashSet<>();
            jpa.setPropertiesDefinitions(existing);
        }

        if (domain.getPropertiesDefinitions() == null) {
            existing.clear();
            return;
        }

        Map<String, PropertyDefinitionJpaEntity> existingByName = existing.stream()
                .collect(Collectors.toMap(PropertyDefinitionJpaEntity::getName, Function.identity()));

        Set<String> updatedNames = domain.getPropertiesDefinitions().stream()
                .map(p -> p.getName())
                .collect(Collectors.toSet());

        // Remove properties no longer present
        existing.removeIf(p -> !updatedNames.contains(p.getName()));

        // Update existing or add new
        for (var domProp : domain.getPropertiesDefinitions()) {
            PropertyDefinitionJpaEntity ex = existingByName.get(domProp.getName());
            if (ex != null) {
                ex.setDescription(domProp.getDescription());
                ex.setType(domProp.getType());
                ex.setRequired(domProp.isRequired());
                mergeRules(ex, domProp.getRules());
            } else {
                PropertyDefinitionJpaEntity newProp = PropertyDefinitionJpaEntity.builder()
                        .id(domProp.getId())
                        .name(domProp.getName())
                        .description(domProp.getDescription())
                        .type(domProp.getType())
                        .required(domProp.isRequired())
                        .rules(domProp.getRules() != null ? toRulesJpa(domProp.getRules()) : null)
                        .build();
                existing.add(newProp);
            }
        }
    }

    private void mergeRules(PropertyDefinitionJpaEntity jpaProp,
                            com.decathlon.idp_core.domain.model.entity_template.PropertyRules domRules) {
        if (domRules == null) return;
        PropertyRulesJpaEntity ex = jpaProp.getRules();
        if (ex != null) {
            ex.setFormat(domRules.getFormat());
            ex.setEnumValues(domRules.getEnumValues());
            ex.setRegex(domRules.getRegex());
            ex.setMaxLength(domRules.getMaxLength());
            ex.setMinLength(domRules.getMinLength());
            ex.setMaxValue(domRules.getMaxValue());
            ex.setMinValue(domRules.getMinValue());
        } else {
            jpaProp.setRules(toRulesJpa(domRules));
        }
    }

    private PropertyRulesJpaEntity toRulesJpa(com.decathlon.idp_core.domain.model.entity_template.PropertyRules d) {
        return PropertyRulesJpaEntity.builder()
                .id(d.getId()).format(d.getFormat()).enumValues(d.getEnumValues())
                .regex(d.getRegex()).maxLength(d.getMaxLength()).minLength(d.getMinLength())
                .maxValue(d.getMaxValue()).minValue(d.getMinValue()).build();
    }

    private void mergeRelationDefinitions(EntityTemplateJpaEntity jpa, EntityTemplate domain) {
        Set<RelationDefinitionJpaEntity> existing = jpa.getRelationsDefinitions();
        if (existing == null) {
            existing = new LinkedHashSet<>();
            jpa.setRelationsDefinitions(existing);
        }

        if (domain.getRelationsDefinitions() == null) {
            existing.clear();
            return;
        }

        Map<String, RelationDefinitionJpaEntity> existingByName = existing.stream()
                .collect(Collectors.toMap(RelationDefinitionJpaEntity::getName, Function.identity()));

        Set<String> updatedNames = domain.getRelationsDefinitions().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        // Remove relations no longer present
        existing.removeIf(r -> !updatedNames.contains(r.getName()));

        // Update existing or add new
        for (var domRel : domain.getRelationsDefinitions()) {
            RelationDefinitionJpaEntity ex = existingByName.get(domRel.getName());
            if (ex != null) {
                ex.setTargetEntityIdentifier(domRel.getTargetEntityIdentifier());
                ex.setRequired(domRel.isRequired());
                ex.setToMany(domRel.isToMany());
            } else {
                RelationDefinitionJpaEntity newRel = RelationDefinitionJpaEntity.builder()
                        .name(domRel.getName())
                        .targetEntityIdentifier(domRel.getTargetEntityIdentifier())
                        .required(domRel.isRequired())
                        .toMany(domRel.isToMany())
                        .build();
                existing.add(newRel);
            }
        }
    }
}
