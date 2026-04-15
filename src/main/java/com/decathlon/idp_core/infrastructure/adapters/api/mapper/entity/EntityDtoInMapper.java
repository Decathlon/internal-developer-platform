package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDtoIn;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class EntityDtoInMapper {
      public Entity fromEntityDtoInToEntity(EntityDtoIn entityDtoIn, String entityTemplateIdentifier) {
        Entity.EntityBuilder entityBuilder = Entity.builder();
        entityBuilder.name(entityDtoIn.getName());
        entityBuilder.templateIdentifier(entityTemplateIdentifier);
        entityBuilder.identifier(entityDtoIn.getIdentifier());

        List<Property> properties = entityDtoIn.getProperties() == null ? Collections.emptyList()
                : entityDtoIn.getProperties().entrySet().stream()
                        .map((Map.Entry<String, Object> entry) -> {
                            String value;
                            if (entry.getValue() != null) {
                                value = String.valueOf(entry.getValue());
                            } else {
                                value = null;
                            }
                            return Property.builder()
                                    .name(entry.getKey())
                                    .value(value)
                                    .build();
                        })
                        .toList();

        List<Relation> relations = entityDtoIn.getRelations() == null ? Collections.emptyList()
                : entityDtoIn.getRelations().stream()
                        .map(relDto -> Relation.builder()
                                .name(relDto.getName())
                                .targetEntityIdentifiers(relDto.getTargetEntityIdentifiers())
                                .build())
                        .toList();

        entityBuilder.properties(properties);
        entityBuilder.relations(relations);

        return entityBuilder.build();
    }

}
