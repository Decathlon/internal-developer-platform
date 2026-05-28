package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDtoInCommonFields;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityUpdateDtoIn;

@DisplayName("EntityDtoInMapper Tests")
class EntityDtoInMapperTest {

    private EntityDtoInMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EntityDtoInMapper();
    }

    @Test
    @DisplayName("Should map create DTO to Entity with populated properties and relations")
    void shouldMapCreateDtoToEntity() {
        // Given
        var properties = new LinkedHashMap<String, String>();
        properties.put("environment", "prod");
        properties.put("port", "8080");

        var relation = EntityDtoInCommonFields.RelationDtoIn.builder()
                .name("depends-on")
                .targetEntityIdentifiers(List.of("gateway", "database"))
                .build();

        var commonFields = EntityDtoInCommonFields.builder()
                .name("payment-service")
                .properties(properties)
                .relations(List.of(relation))
                .build();

        var createDto = EntityCreateDtoIn.builder()
                .identifier("payment-service-1")
                .entityDtoInCommonFields(commonFields)
                .build();

        // When
        Entity result = mapper.fromPostEntityDtoInToEntity(createDto, "service-template");

        // Then
        assertThat(result.id()).isNull();
        assertThat(result.templateIdentifier()).isEqualTo("service-template");
        assertThat(result.name()).isEqualTo("payment-service");
        assertThat(result.identifier()).isEqualTo("payment-service-1");

        assertThat(result.properties())
                .hasSize(2)
                .extracting(property -> property.name() + "=" + property.value())
                .containsExactly("environment=prod", "port=8080");

        assertThat(result.relations()).hasSize(1);
        var mappedRelation = result.relations().getFirst();
        assertThat(mappedRelation.id()).isNull();
        assertThat(mappedRelation.name()).isEqualTo("depends-on");
        assertThat(mappedRelation.targetTemplateIdentifier()).isNull();
        assertThat(mappedRelation.targetEntityIdentifiers()).containsExactly("gateway", "database");
    }

    @Test
    @DisplayName("Should map update DTO using path identifier and handle null collections")
    void shouldMapUpdateDtoToEntityWithNullCollections() {
        // Given
        var commonFields = EntityDtoInCommonFields.builder()
                .name("catalog-service")
                .properties(null)
                .relations(null)
                .build();

        var updateDto = EntityUpdateDtoIn.builder()
                .entityDtoInCommonFields(commonFields)
                .build();

        // When
        Entity result = mapper.fromPutEntityDtoInToEntity(updateDto, "service-template", "catalog-service-42");

        // Then
        assertThat(result.id()).isNull();
        assertThat(result.templateIdentifier()).isEqualTo("service-template");
        assertThat(result.name()).isEqualTo("catalog-service");
        assertThat(result.identifier()).isEqualTo("catalog-service-42");
        assertThat(result.properties()).isEmpty();
        assertThat(result.relations()).isEmpty();
    }
}
