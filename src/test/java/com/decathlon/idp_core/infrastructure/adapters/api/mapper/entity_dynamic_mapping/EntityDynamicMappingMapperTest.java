package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.MappingAction;
import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.RelationMappingDtoOut;

/// Unit tests for EntityDynamicMappingMapper, focusing on null safety and edge cases
/// in the toRelationMappingDtoOut conversion.
@DisplayName("EntityDynamicMappingMapper Unit Tests")
class EntityDynamicMappingMapperTest {

  private final EntityDynamicMappingMapper mapper = new EntityDynamicMappingMapper();

  @Test
  @DisplayName("Should convert entity dynamic mapping with valid relations to DTO")
  void fromEntityMappingToDto_with_valid_relations() {
    List<RelationMapping> relations = List.of(
        new RelationMapping("api-link", List.of(".repository.full_name")),
        new RelationMapping("dependency", List.of(".dependencies[*].identifier")));

    EntityDynamicMapping mapping = new EntityDynamicMapping(null, // id
        "test-mapping", "microservice", ".action == \"pushed\"", MappingAction.UPDATE_ENTITY,
        "Test Mapping", "Test Description", ".repository.full_name", ".repository.name",
        Map.of("applicationName", ".repository.name"), relations);

    EntityDynamicMappingDtoOut dto = mapper.fromEntityMappingToDto(mapping);

    assertThat(dto).isNotNull();
    assertThat(dto.entity()).isNotNull();
    assertThat(dto.entity().relations()).isNotNull().hasSize(2);
    assertThat(dto.entity().relations().getFirst().name()).isEqualTo("api-link");
    assertThat(dto.entity().relations().getFirst().targetEntityIdentifiers())
        .containsExactly(".repository.full_name");
    assertThat(dto.entity().relations().get(1).name()).isEqualTo("dependency");
    assertThat(dto.entity().relations().get(1).targetEntityIdentifiers())
        .containsExactly(".dependencies[*].identifier");
  }

  @Test
  @DisplayName("Should preserve the DELETE action when converting to DTO")
  void fromEntityMappingToDto_preserves_delete_action() {
    EntityDynamicMapping mapping = new EntityDynamicMapping(null, "delete-mapping", "microservice",
        ".action == \"deleted\"", MappingAction.DELETE_ENTITY, "Delete Mapping", "description",
        ".id", ".name", Map.of(), List.of());

    EntityDynamicMappingDtoOut dto = mapper.fromEntityMappingToDto(mapping);

    assertThat(dto.action()).isEqualTo(MappingAction.DELETE_ENTITY);
  }

  @Test
  @DisplayName("Should handle null relations list in entity mapping")
  void fromEntityMappingToDto_with_null_relations() {
    EntityDynamicMapping mapping = new EntityDynamicMapping(null, // id
        "test-mapping", "microservice", ".action == \"pushed\"", MappingAction.UPDATE_ENTITY,
        "Test Mapping", "Test Description", ".repository.full_name", ".repository.name",
        Map.of("applicationName", ".repository.name"), null); // null relations

    EntityDynamicMappingDtoOut dto = mapper.fromEntityMappingToDto(mapping);

    assertThat(dto).isNotNull();
    assertThat(dto.entity()).isNotNull();
    assertThat(dto.entity().relations()).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("Should handle empty relations list in entity mapping")
  void fromEntityMappingToDto_with_empty_relations() {
    EntityDynamicMapping mapping = new EntityDynamicMapping(null, // id
        "test-mapping", "microservice", ".action == \"pushed\"", MappingAction.UPDATE_ENTITY,
        "Test Mapping", "Test Description", ".repository.full_name", ".repository.name",
        Map.of("applicationName", ".repository.name"), List.of()); // empty relations

    EntityDynamicMappingDtoOut dto = mapper.fromEntityMappingToDto(mapping);

    assertThat(dto).isNotNull();
    assertThat(dto.entity()).isNotNull();
    assertThat(dto.entity().relations()).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("Should reject RelationMapping with null expressions")
  void relationMapping_with_null_expressions_should_throw() {
    List<String> expressions = null;

    assertThatThrownBy(() -> new RelationMapping("api-link", expressions))
        .isInstanceOf(EntityDynamicMappingConfigurationException.class)
        .hasMessageContaining("array of strings");
  }

  @Test
  @DisplayName("Should reject RelationMapping with null name")
  void relationMapping_with_null_name_should_throw() {
    List<String> expressions = List.of(".dependencies[*].identifier");

    assertThatThrownBy(() -> new RelationMapping(null, expressions))
        .isInstanceOf(EntityDynamicMappingConfigurationException.class)
        .hasMessageContaining("mandatory");
  }

  @Test
  @DisplayName("Should handle null properties in entity mapping")
  void fromEntityMappingToDto_with_null_properties() {
    EntityDynamicMapping mapping = new EntityDynamicMapping(null, "test-mapping", "microservice",
        ".action == \"pushed\"", MappingAction.UPDATE_ENTITY, "Test Mapping", "Test Description",
        ".repository.full_name", ".repository.name", null, // null properties
        List.of());

    EntityDynamicMappingDtoOut dto = mapper.fromEntityMappingToDto(mapping);

    assertThat(dto).isNotNull();
    assertThat(dto.entity()).isNotNull();
    assertThat(dto.entity().properties()).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("Should verify immutability of relations in DTO (defensive copy)")
  void fromEntityMappingToDto_relations_are_defensively_copied() {
    List<RelationMapping> originalRelations = List
        .of(new RelationMapping("api-link", List.of(".repository.full_name")));

    EntityDynamicMapping mapping = new EntityDynamicMapping(null, // id
        "test-mapping", "microservice", ".action == \"pushed\"", MappingAction.UPDATE_ENTITY,
        "Test Mapping", "Test Description", ".repository.full_name", ".repository.name",
        Map.of("applicationName", ".repository.name"), originalRelations);

    EntityDynamicMappingDtoOut dto = mapper.fromEntityMappingToDto(mapping);

    assertThat(dto.entity().relations()).isNotNull().hasSize(1).isUnmodifiable()
        .extracting(RelationMappingDtoOut::targetEntityIdentifiers).allMatch(ids -> {
          try {
            ids.add("new-value");
            return false;
          } catch (UnsupportedOperationException _) {
            return true; // expected: list is unmodifiable
          }
        });
  }
}
