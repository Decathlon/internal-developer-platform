package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyRulesJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.EntityTemplateJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.PropertyDefinitionJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.RelationDefinitionJpaEntity;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED
)
public interface EntityTemplatePersistenceMapper {

    EntityTemplate toDomain(EntityTemplateJpaEntity jpa);

    EntityTemplateJpaEntity toJpa(EntityTemplate domain);

    PropertyDefinition toDomain(PropertyDefinitionJpaEntity jpa);

    PropertyDefinitionJpaEntity toJpa(PropertyDefinition domain);

    PropertyRules toDomain(PropertyRulesJpaEntity jpa);

    PropertyRulesJpaEntity toJpa(PropertyRules domain);

    RelationDefinition toDomain(RelationDefinitionJpaEntity jpa);

    RelationDefinitionJpaEntity toJpa(RelationDefinition domain);
}
