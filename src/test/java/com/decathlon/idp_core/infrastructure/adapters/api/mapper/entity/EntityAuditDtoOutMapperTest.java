package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.entity.EntityAuditInfo;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit.EntityAuditDtoOut;

/// Unit tests for EntityAuditDtoOutMapper.
/// Covers mapping of domain audit information to API response DTOs with null safety.
@DisplayName("EntityAuditDtoOutMapper Tests")
class EntityAuditDtoOutMapperTest {

  private EntityAuditDtoOutMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new EntityAuditDtoOutMapper();
  }

  @Nested
  @DisplayName("Single EntityAuditInfo Mapping Tests")
  class SingleAuditInfoMappingTests {

    @Test
    @DisplayName("Should map EntityAuditInfo with null snapshot to EntityAuditDtoOut")
    void shouldMapAuditInfoWithNullSnapshot() {
      // Given
      var auditInfo = new EntityAuditInfo(1L, Instant.now(), "CREATED", "test-user", null);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRevisionNumber()).isEqualTo(1L);
      assertThat(result.getRevisionType()).isEqualTo("CREATED");
      assertThat(result.getModifiedBy()).isEqualTo("test-user");
      assertThat(result.getSnapshot()).isNull();
    }

    @Test
    @DisplayName("Should map EntityAuditInfo with snapshot containing properties and relations")
    void shouldMapAuditInfoWithSnapshot() {
      // Given
      UUID entityId = UUID.randomUUID();
      Instant revisionDate = Instant.now();

      var propertySnapshot = new EntityAuditInfo.PropertySnapshot(UUID.randomUUID(), "env", "PROD",
          Map.of("name_mod", false, "value_mod", true));
      var relationSnapshot = new EntityAuditInfo.RelationSnapshot(UUID.randomUUID(), "dependency",
          "service", List.of("service-1", "service-2"),
          Map.of("name_mod", false, "target_template_identifier_mod", false));

      var entitySnapshot = new EntityAuditInfo.EntitySnapshot(entityId, "web-service",
          "Web API Service", "web-api-v1", Map.of("name_mod", true), List.of(propertySnapshot),
          List.of(relationSnapshot));

      var auditInfo = new EntityAuditInfo(5L, revisionDate, "UPDATED", "developer", entitySnapshot);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRevisionNumber()).isEqualTo(5L);
      assertThat(result.getRevisionType()).isEqualTo("UPDATED");
      assertThat(result.getModifiedBy()).isEqualTo("developer");
      assertThat(result.getRevisionDate()).isEqualTo(revisionDate);

      // Verify snapshot
      assertThat(result.getSnapshot()).isNotNull();
      assertThat(result.getSnapshot().getId()).isEqualTo(entityId);
      assertThat(result.getSnapshot().getTemplateIdentifier()).isEqualTo("web-service");
      assertThat(result.getSnapshot().getName()).isEqualTo("Web API Service");
      assertThat(result.getSnapshot().getIdentifier()).isEqualTo("web-api-v1");

      // Verify properties
      assertThat(result.getSnapshot().getProperties()).hasSize(1);
      assertThat(result.getSnapshot().getProperties().get(0).getName()).isEqualTo("env");
      assertThat(result.getSnapshot().getProperties().get(0).getValue()).isEqualTo("PROD");

      // Verify relations
      assertThat(result.getSnapshot().getRelations()).hasSize(1);
      assertThat(result.getSnapshot().getRelations().get(0).getName()).isEqualTo("dependency");
      assertThat(result.getSnapshot().getRelations().get(0).getTargetEntityIdentifiers())
          .containsExactly("service-1", "service-2");
    }

    @Test
    @DisplayName("Should map DELETED revision type audit info")
    void shouldMapDeletedRevisionType() {
      // Given
      var propertySnapshot = new EntityAuditInfo.PropertySnapshot(UUID.randomUUID(), "status",
          "active", Map.of());
      var entitySnapshot = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "web-service",
          "Service", "web-api", Map.of(), List.of(propertySnapshot), List.of());

      var auditInfo = new EntityAuditInfo(10L, Instant.now(), "DELETED", "admin", entitySnapshot);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRevisionType()).isEqualTo("DELETED");
      assertThat(result.getSnapshot()).isNotNull();
      assertThat(result.getSnapshot().getProperties()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle null EntityAuditInfo")
    void shouldHandleNullAuditInfo() {
      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(null);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should map audit info with empty properties and relations")
    void shouldMapAuditInfoWithEmptyCollections() {
      // Given
      var entitySnapshot = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template", "Name",
          "identifier", Map.of(), List.of(), List.of());
      var auditInfo = new EntityAuditInfo(1L, Instant.now(), "CREATED", "user", entitySnapshot);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getSnapshot()).isNotNull();
      assertThat(result.getSnapshot().getProperties()).isEmpty();
      assertThat(result.getSnapshot().getRelations()).isEmpty();
    }

    @Test
    @DisplayName("Should map audit info with null properties and relations collections")
    void shouldMapAuditInfoWithNullCollections() {
      // Given
      var entitySnapshot = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template", "Name",
          "identifier", Map.of(), null, null);
      var auditInfo = new EntityAuditInfo(1L, Instant.now(), "UPDATED", "user", entitySnapshot);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getSnapshot()).isNotNull();
      assertThat(result.getSnapshot().getProperties()).isEmpty();
      assertThat(result.getSnapshot().getRelations()).isEmpty();
    }

    @Test
    @DisplayName("Should map audit info with null targetEntityIdentifiers in relation")
    void shouldMapRelationWithNullTargetIdentifiers() {
      // Given
      var relationSnapshot = new EntityAuditInfo.RelationSnapshot(UUID.randomUUID(), "relation",
          "target-template", null, Map.of());
      var entitySnapshot = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template", "Name",
          "id", Map.of(), List.of(), List.of(relationSnapshot));

      var auditInfo = new EntityAuditInfo(1L, Instant.now(), "CREATED", "user", entitySnapshot);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);

      // Then
      assertThat(result.getSnapshot().getRelations()).hasSize(1);
      assertThat(result.getSnapshot().getRelations().getFirst().getTargetEntityIdentifiers())
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("List Mapping Tests")
  class ListMappingTests {

    @Test
    @DisplayName("Should map list of EntityAuditInfo to list of EntityAuditDtoOut")
    void shouldMapAuditInfoList() {
      // Given
      var auditInfo1 = new EntityAuditInfo(1L, Instant.now(), "CREATED", "user1", null);
      var auditInfo2 = new EntityAuditInfo(2L, Instant.now(), "UPDATED", "user2", null);
      var auditInfoList = List.of(auditInfo1, auditInfo2);

      // When
      List<EntityAuditDtoOut> result = mapper.fromEntityAuditInfoList(auditInfoList);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getRevisionNumber()).isEqualTo(1L);
      assertThat(result.get(0).getModifiedBy()).isEqualTo("user1");
      assertThat(result.get(1).getRevisionNumber()).isEqualTo(2L);
      assertThat(result.get(1).getModifiedBy()).isEqualTo("user2");
    }

    @Test
    @DisplayName("Should map list with audit infos containing snapshots")
    void shouldMapAuditInfoListWithSnapshots() {
      // Given
      var snapshot1 = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template1", "Name1",
          "id1", Map.of(), List.of(), List.of());
      var snapshot2 = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template2", "Name2",
          "id2", Map.of(), List.of(), List.of());

      var auditInfo1 = new EntityAuditInfo(1L, Instant.now(), "CREATED", "user1", snapshot1);
      var auditInfo2 = new EntityAuditInfo(2L, Instant.now(), "UPDATED", "user2", snapshot2);

      // When
      List<EntityAuditDtoOut> result = mapper
          .fromEntityAuditInfoList(List.of(auditInfo1, auditInfo2));

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getSnapshot()).isNotNull();
      assertThat(result.get(0).getSnapshot().getName()).isEqualTo("Name1");
      assertThat(result.get(1).getSnapshot()).isNotNull();
      assertThat(result.get(1).getSnapshot().getName()).isEqualTo("Name2");
    }

    @Test
    @DisplayName("Should handle null list")
    void shouldHandleNullList() {
      // When
      List<EntityAuditDtoOut> result = mapper.fromEntityAuditInfoList(null);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty list")
    void shouldHandleEmptyList() {
      // When
      List<EntityAuditDtoOut> result = mapper.fromEntityAuditInfoList(List.of());

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should map list with mixed null and non-null snapshots")
    void shouldMapListWithMixedSnapshots() {
      // Given
      var snapshot = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template", "Name", "id",
          Map.of(), List.of(), List.of());

      var auditInfo1 = new EntityAuditInfo(1L, Instant.now(), "CREATED", "user1", null);
      var auditInfo2 = new EntityAuditInfo(2L, Instant.now(), "UPDATED", "user2", snapshot);

      // When
      List<EntityAuditDtoOut> result = mapper
          .fromEntityAuditInfoList(List.of(auditInfo1, auditInfo2));

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getSnapshot()).isNull();
      assertThat(result.get(1).getSnapshot()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Edge Cases and Defensive Copying Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle property snapshot with null values")
    void shouldHandlePropertySnapshotWithNullValues() {
      // Given
      var propertySnapshot = new EntityAuditInfo.PropertySnapshot(null, null, null, Map.of());
      var entitySnapshot = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template", "Name",
          "id", Map.of(), List.of(propertySnapshot), List.of());

      var auditInfo = new EntityAuditInfo(1L, Instant.now(), "CREATED", "user", entitySnapshot);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);

      // Then
      assertThat(result.getSnapshot().getProperties()).hasSize(1);
      assertThat(result.getSnapshot().getProperties().get(0).getId()).isNull();
      assertThat(result.getSnapshot().getProperties().get(0).getName()).isNull();
      assertThat(result.getSnapshot().getProperties().get(0).getValue()).isNull();
    }

    @Test
    @DisplayName("Should preserve immutability by copying targetEntityIdentifiers")
    void shouldPreserveImmutabilityOfTargetIdentifiers() {
      // Given
      List<String> originalIdentifiers = new java.util.ArrayList<>(List.of("id1", "id2"));
      var relationSnapshot = new EntityAuditInfo.RelationSnapshot(UUID.randomUUID(), "relation",
          "target", originalIdentifiers, Map.of());
      var entitySnapshot = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template", "Name",
          "id", Map.of(), List.of(), List.of(relationSnapshot));

      var auditInfo = new EntityAuditInfo(1L, Instant.now(), "CREATED", "user", entitySnapshot);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);
      originalIdentifiers.add("id3");

      // Then - Verify that the DTO list wasn't affected by the original list
      // modification
      assertThat(result.getSnapshot().getRelations().get(0).getTargetEntityIdentifiers())
          .containsExactly("id1", "id2");
    }

    @Test
    @DisplayName("Should handle relation with empty targetEntityIdentifiers")
    void shouldHandleRelationWithEmptyTargetIdentifiers() {
      // Given
      var relationSnapshot = new EntityAuditInfo.RelationSnapshot(UUID.randomUUID(), "relation",
          "target", List.of(), Map.of());
      var entitySnapshot = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template", "Name",
          "id", Map.of(), List.of(), List.of(relationSnapshot));

      var auditInfo = new EntityAuditInfo(1L, Instant.now(), "CREATED", "user", entitySnapshot);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);

      // Then
      assertThat(result.getSnapshot().getRelations().get(0).getTargetEntityIdentifiers()).isEmpty();
    }

    @Test
    @DisplayName("Should map multiple properties and relations correctly")
    void shouldMapMultiplePropertiesAndRelations() {
      // Given
      var prop1 = new EntityAuditInfo.PropertySnapshot(UUID.randomUUID(), "prop1", "value1",
          Map.of("name_mod", false, "value_mod", true));
      var prop2 = new EntityAuditInfo.PropertySnapshot(UUID.randomUUID(), "prop2", "value2",
          Map.of("name_mod", true, "value_mod", false));
      var prop3 = new EntityAuditInfo.PropertySnapshot(UUID.randomUUID(), "prop3", "value3",
          Map.of());

      var rel1 = new EntityAuditInfo.RelationSnapshot(UUID.randomUUID(), "rel1", "t1",
          List.of("id1"), Map.of("name_mod", false));
      var rel2 = new EntityAuditInfo.RelationSnapshot(UUID.randomUUID(), "rel2", "t2",
          List.of("id2", "id3"), Map.of("target_template_identifier_mod", true));

      var entitySnapshot = new EntityAuditInfo.EntitySnapshot(UUID.randomUUID(), "template", "Name",
          "id", Map.of("name_mod", true), List.of(prop1, prop2, prop3), List.of(rel1, rel2));

      var auditInfo = new EntityAuditInfo(1L, Instant.now(), "CREATED", "user", entitySnapshot);

      // When
      EntityAuditDtoOut result = mapper.fromEntityAuditInfo(auditInfo);

      // Then
      assertThat(result.getSnapshot().getProperties()).hasSize(3);
      assertThat(result.getSnapshot().getRelations()).hasSize(2);
      assertThat(result.getSnapshot().getProperties().get(0).getName()).isEqualTo("prop1");
      assertThat(result.getSnapshot().getProperties().get(1).getName()).isEqualTo("prop2");
      assertThat(result.getSnapshot().getProperties().get(2).getName()).isEqualTo("prop3");
      assertThat(result.getSnapshot().getRelations().get(0).getName()).isEqualTo("rel1");
      assertThat(result.getSnapshot().getRelations().get(1).getName()).isEqualTo("rel2");
    }
  }
}
