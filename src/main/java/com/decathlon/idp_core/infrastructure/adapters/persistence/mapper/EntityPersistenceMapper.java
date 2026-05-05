package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

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

    @Mapping(target = "value", source = "value", qualifiedByName = "propertyValueFromString")
    Property toDomain(PropertyJpaEntity jpa);

    @Mapping(target = "value", source = "value", qualifiedByName = "propertyValueToString")
    PropertyJpaEntity toJpa(Property domain);

    Relation toDomain(RelationJpaEntity jpa);

    RelationJpaEntity toJpa(Relation domain);

    /// Converts a domain property value (carried as [Object] to preserve the
    /// original JSON type) into its canonical String representation for storage.
    @Named("propertyValueToString")
    default String propertyValueToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /// Promotes a persisted String value to the domain [Object] representation.
    /// Persistence is the source of truth for textual storage; richer typing
    /// (Number/Boolean) is reconstructed by the API output mapper using the template.
    @Named("propertyValueFromString")
    default Object propertyValueFromString(String value) {
        return value;
    }
}
