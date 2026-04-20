package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EntityPersistenceMapper {

    Entity toDomain(EntityJpaEntity jpa);

    EntityJpaEntity toJpa(Entity domain);

    Property toDomain(PropertyJpaEntity jpa);

    PropertyJpaEntity toJpa(Property domain);

    Relation toDomain(RelationJpaEntity jpa);

    RelationJpaEntity toJpa(Relation domain);
}
