package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationTargetJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityPersistenceMapper Test Suite")
class EntityPersistenceMapperTest {

  @Mock
  private JpaEntityRepository entityRepository;

  @InjectMocks
  private EntityPersistenceMapper mapper;

  // =========================================================================
  // Entity Mapping Tests - toDomain()
  // =========================================================================

  @Nested
  @DisplayName("Entity toDomain() Tests")
  class EntityToDomainTests {

    @Test
    @DisplayName("Should return null when JPA entity is null")
    void toDomain_WithNullJpaEntity_ReturnsNull() {
      assertNull(mapper.toDomain((EntityJpaEntity) null));
    }

    @Test
    @DisplayName("Should convert JPA entity with all properties to domain")
    void toDomain_WithValidJpaEntity_ReturnsDomainEntity() {
      UUID id = UUID.randomUUID();
      PropertyJpaEntity prop1 = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("prop1")
          .value("value1").build();
      PropertyJpaEntity prop2 = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("prop2")
          .value("value2").build();
      Set<PropertyJpaEntity> properties = Set.of(prop1, prop2);

      RelationTargetJpaEntity target = new RelationTargetJpaEntity(UUID.randomUUID(), "target-id");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target))).build();
      Set<RelationJpaEntity> relations = Set.of(rel1);

      EntityJpaEntity jpa = EntityJpaEntity.builder().id(id).templateIdentifier("template-id")
          .name("entity-name").identifier("entity-id").properties(properties).relations(relations)
          .build();

      Entity domain = mapper.toDomain(jpa);

      assertNotNull(domain);
      assertEquals(id, domain.id());
      assertEquals("template-id", domain.templateIdentifier());
      assertEquals("entity-name", domain.name());
      assertEquals("entity-id", domain.identifier());
      assertEquals(2, domain.properties().size());
      assertEquals(1, domain.relations().size());
    }

    @Test
    @DisplayName("Should sort properties by name in domain entity")
    void toDomain_WithProperties_SortsPropertiesByName() {
      UUID id = UUID.randomUUID();
      PropertyJpaEntity propZ = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("z-prop")
          .value("z-value").build();
      PropertyJpaEntity propA = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("a-prop")
          .value("a-value").build();
      PropertyJpaEntity propM = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("m-prop")
          .value("m-value").build();
      Set<PropertyJpaEntity> properties = Set.of(propZ, propA, propM);

      EntityJpaEntity jpa = EntityJpaEntity.builder().id(id).templateIdentifier("template-id")
          .name("entity-name").identifier("entity-id").properties(properties)
          .relations(new HashSet<>()).build();

      Entity domain = mapper.toDomain(jpa);

      assertEquals("a-prop", domain.properties().get(0).name());
      assertEquals("m-prop", domain.properties().get(1).name());
      assertEquals("z-prop", domain.properties().get(2).name());
    }

    @Test
    @DisplayName("Should sort relations by name in domain entity")
    void toDomain_WithRelations_SortsRelationsByName() {
      UUID id = UUID.randomUUID();
      RelationJpaEntity relZ = RelationJpaEntity.builder().id(UUID.randomUUID()).name("z-rel")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();
      RelationJpaEntity relA = RelationJpaEntity.builder().id(UUID.randomUUID()).name("a-rel")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();
      RelationJpaEntity relM = RelationJpaEntity.builder().id(UUID.randomUUID()).name("m-rel")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();
      Set<RelationJpaEntity> relations = Set.of(relZ, relA, relM);

      EntityJpaEntity jpa = EntityJpaEntity.builder().id(id).templateIdentifier("template-id")
          .name("entity-name").identifier("entity-id").properties(new HashSet<>())
          .relations(relations).build();

      Entity domain = mapper.toDomain(jpa);

      assertEquals("a-rel", domain.relations().get(0).name());
      assertEquals("m-rel", domain.relations().get(1).name());
      assertEquals("z-rel", domain.relations().get(2).name());
    }

    @Test
    @DisplayName("Should handle null properties collection")
    void toDomain_WithNullProperties_ReturnsEmptyList() {
      UUID id = UUID.randomUUID();
      EntityJpaEntity jpa = EntityJpaEntity.builder().id(id).templateIdentifier("template-id")
          .name("entity-name").identifier("entity-id").properties(null).relations(new HashSet<>())
          .build();

      Entity domain = mapper.toDomain(jpa);

      assertNotNull(domain.properties());
      assertTrue(domain.properties().isEmpty());
    }

    @Test
    @DisplayName("Should handle null relations collection")
    void toDomain_WithNullRelations_ReturnsEmptyList() {
      UUID id = UUID.randomUUID();
      EntityJpaEntity jpa = EntityJpaEntity.builder().id(id).templateIdentifier("template-id")
          .name("entity-name").identifier("entity-id").properties(new HashSet<>()).relations(null)
          .build();

      Entity domain = mapper.toDomain(jpa);

      assertNotNull(domain.relations());
      assertTrue(domain.relations().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty properties and relations collections")
    void toDomain_WithEmptyCollections_ReturnsEntity() {
      UUID id = UUID.randomUUID();
      EntityJpaEntity jpa = EntityJpaEntity.builder().id(id).templateIdentifier("template-id")
          .name("entity-name").identifier("entity-id").properties(new HashSet<>())
          .relations(new HashSet<>()).build();

      Entity domain = mapper.toDomain(jpa);

      assertNotNull(domain);
      assertTrue(domain.properties().isEmpty());
      assertTrue(domain.relations().isEmpty());
    }
  }

  // =========================================================================
  // Entity Mapping Tests - toJpa()
  // =========================================================================

  @Nested
  @DisplayName("Entity toJpa() Tests")
  class EntityToJpaTests {

    @Test
    @DisplayName("Should return null when domain entity is null")
    void toJpa_WithNullDomainEntity_ReturnsNull() {
      assertNull(mapper.toJpa((Entity) null));
    }

    @Test
    @DisplayName("Should convert domain entity to JPA with properties and relations")
    void toJpa_WithValidDomainEntity_ReturnsJpaEntity() {
      UUID id = UUID.randomUUID();
      UUID targetId = UUID.randomUUID();
      EntityJpaEntity targetEntity = EntityJpaEntity.builder().id(targetId)
          .templateIdentifier("target-tmpl").identifier("target-id").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id"))).thenReturn(List.of(targetEntity));

      Property prop = new Property(UUID.randomUUID(), "prop1", "value1");
      Relation rel = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of("target-id"));
      Entity domain = new Entity(id, "template-id", "entity-name", "entity-id", List.of(prop),
          List.of(rel));

      long beforeCall = Instant.now().toEpochMilli();
      EntityJpaEntity jpa = mapper.toJpa(domain);
      long afterCall = Instant.now().toEpochMilli();

      assertNotNull(jpa);
      assertEquals(id, jpa.getId());
      assertEquals("template-id", jpa.getTemplateIdentifier());
      assertEquals("entity-name", jpa.getName());
      assertEquals("entity-id", jpa.getIdentifier());
      assertTrue(jpa.getUpdatedAt() >= beforeCall && jpa.getUpdatedAt() <= afterCall);
      assertEquals(1, jpa.getProperties().size());
      assertEquals(1, jpa.getRelations().size());
    }

    @Test
    @DisplayName("Should convert domain list to JPA set for properties")
    void toJpa_WithProperties_ConvertsToSet() {
      UUID id = UUID.randomUUID();
      Property prop1 = new Property(UUID.randomUUID(), "prop1", "value1");
      Property prop2 = new Property(UUID.randomUUID(), "prop2", "value2");
      Entity domain = new Entity(id, "template-id", "entity-name", "entity-id",
          List.of(prop1, prop2), List.of());

      EntityJpaEntity jpa = mapper.toJpa(domain);

      assertEquals(2, jpa.getProperties().size());
      assertEquals(2, new HashSet<>(jpa.getProperties()).size());
    }

    @Test
    @DisplayName("Should handle null properties in domain entity")
    void toJpa_WithNullProperties_CreatesEmptySet() {
      UUID id = UUID.randomUUID();
      Entity domain = new Entity(id, "template-id", "entity-name", "entity-id", null, List.of());

      EntityJpaEntity jpa = mapper.toJpa(domain);

      assertNotNull(jpa.getProperties());
      assertTrue(jpa.getProperties().isEmpty());
    }

    @Test
    @DisplayName("Should handle null relations in domain entity")
    void toJpa_WithNullRelations_CreatesEmptySet() {
      UUID id = UUID.randomUUID();
      Entity domain = new Entity(id, "template-id", "entity-name", "entity-id", List.of(), null);

      EntityJpaEntity jpa = mapper.toJpa(domain);

      assertNotNull(jpa.getRelations());
      assertTrue(jpa.getRelations().isEmpty());
    }

    @Test
    @DisplayName("Should update timestamp in JPA entity")
    void toJpa_UpdatesTimestamp() {
      UUID id = UUID.randomUUID();
      Entity domain = new Entity(id, "template-id", "entity-name", "entity-id", List.of(),
          List.of());

      EntityJpaEntity jpa = mapper.toJpa(domain);

      assertTrue(jpa.getUpdatedAt() > 0);
    }
  }

  // =========================================================================
  // Entity Mapping Tests - toJpaWithMerge()
  // =========================================================================

  @Nested
  @DisplayName("Entity toJpaWithMerge() Tests")
  class EntityToJpaWithMergeTests {

    @Test
    @DisplayName("Should return null when domain entity is null")
    void toJpaWithMerge_WithNullDomainEntity_ReturnsNull() {
      EntityJpaEntity existing = EntityJpaEntity.builder().id(UUID.randomUUID()).build();
      assertNull(mapper.toJpaWithMerge(null, existing));
    }

    @Test
    @DisplayName("Should update existing JPA entity with domain data")
    void toJpaWithMerge_WithValidDomainEntity_UpdatesExistingJpaEntity() {
      UUID id = UUID.randomUUID();
      UUID existingId = UUID.randomUUID();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("old-template").name("old-name").identifier("old-id")
          .properties(new HashSet<>()).relations(new HashSet<>()).build();

      Entity domain = new Entity(id, "new-template", "new-name", "new-id", List.of(), List.of());

      long beforeCall = Instant.now().toEpochMilli();
      EntityJpaEntity result = mapper.toJpaWithMerge(domain, existing);
      long afterCall = Instant.now().toEpochMilli();

      assertEquals(existingId, result.getId());
      assertEquals("new-template", result.getTemplateIdentifier());
      assertEquals("new-name", result.getName());
      assertEquals("new-id", result.getIdentifier());
      assertTrue(result.getUpdatedAt() >= beforeCall && result.getUpdatedAt() <= afterCall);
    }

    @Test
    @DisplayName("Should clear existing properties when domain has no properties")
    void toJpaWithMerge_WithNullProperties_ClearsExistingProperties() {
      UUID existingId = UUID.randomUUID();
      PropertyJpaEntity existingProp = PropertyJpaEntity.builder().id(UUID.randomUUID())
          .name("prop1").value("value1").build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id")
          .properties(new HashSet<>(Set.of(existingProp))).relations(new HashSet<>()).build();

      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", null, List.of());

      mapper.toJpaWithMerge(domain, existing);

      assertTrue(existing.getProperties().isEmpty());
    }

    @Test
    @DisplayName("Should clear existing properties when domain has empty properties")
    void toJpaWithMerge_WithEmptyProperties_ClearsExistingProperties() {
      UUID existingId = UUID.randomUUID();
      PropertyJpaEntity existingProp = PropertyJpaEntity.builder().id(UUID.randomUUID())
          .name("prop1").value("value1").build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id")
          .properties(new HashSet<>(Set.of(existingProp))).relations(new HashSet<>()).build();

      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(), List.of());

      mapper.toJpaWithMerge(domain, existing);

      assertTrue(existing.getProperties().isEmpty());
    }

    @Test
    @DisplayName("Should clear existing relations when domain has no relations")
    void toJpaWithMerge_WithNullRelations_ClearsExistingRelations() {
      UUID existingId = UUID.randomUUID();
      RelationJpaEntity existingRel = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl").targetEntities(new HashSet<>()).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(existingRel))).build();

      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(), null);

      mapper.toJpaWithMerge(domain, existing);

      assertTrue(existing.getRelations().isEmpty());
    }

    @Test
    @DisplayName("Should clear existing relations when domain has empty relations")
    void toJpaWithMerge_WithEmptyRelations_ClearsExistingRelations() {
      UUID existingId = UUID.randomUUID();
      RelationJpaEntity existingRel = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl").targetEntities(new HashSet<>()).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(existingRel))).build();

      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(), List.of());

      mapper.toJpaWithMerge(domain, existing);

      assertTrue(existing.getRelations().isEmpty());
    }
  }

  // =========================================================================
  // Property Mapping Tests - mergeProperties()
  // =========================================================================

  @Nested
  @DisplayName("Property Merge Tests")
  class PropertyMergeTests {

    @Test
    @DisplayName("Should remove properties not present in domain entity")
    void mergeProperties_RemovesPropertiesNotInDomain() {
      UUID existingId = UUID.randomUUID();
      PropertyJpaEntity prop1 = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("prop1")
          .value("value1").build();
      PropertyJpaEntity prop2 = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("prop2")
          .value("value2").build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id")
          .properties(new HashSet<>(Set.of(prop1, prop2))).relations(new HashSet<>()).build();

      Property domainProp1 = new Property(UUID.randomUUID(), "prop1", "updated-value1");
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(domainProp1),
          List.of());

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getProperties().size());
      assertTrue(existing.getProperties().stream().anyMatch(p -> p.getName().equals("prop1")));
    }

    @Test
    @DisplayName("Should update value of existing property")
    void mergeProperties_UpdatesExistingProperty() {
      UUID existingId = UUID.randomUUID();
      PropertyJpaEntity prop1 = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("prop1")
          .value("old-value").build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id")
          .properties(new HashSet<>(Set.of(prop1))).relations(new HashSet<>()).build();

      Property domainProp1 = new Property(UUID.randomUUID(), "prop1", "new-value");
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(domainProp1),
          List.of());

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getProperties().size());
      assertEquals("new-value", existing.getProperties().iterator().next().getValue());
    }

    @Test
    @DisplayName("Should add new property not present in existing")
    void mergeProperties_AddsNewProperty() {
      UUID existingId = UUID.randomUUID();
      PropertyJpaEntity prop1 = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("prop1")
          .value("value1").build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id")
          .properties(new HashSet<>(Set.of(prop1))).relations(new HashSet<>()).build();

      Property domainProp1 = new Property(UUID.randomUUID(), "prop1", "value1");
      Property domainProp2 = new Property(UUID.randomUUID(), "prop2", "value2");
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id",
          List.of(domainProp1, domainProp2), List.of());

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(2, existing.getProperties().size());
      assertTrue(existing.getProperties().stream().anyMatch(p -> p.getName().equals("prop2")));
    }

    @Test
    @DisplayName("Should handle mixed merge: remove, update, and add properties")
    void mergeProperties_HandlesMixedOperations() {
      UUID existingId = UUID.randomUUID();
      PropertyJpaEntity prop1 = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("prop1")
          .value("value1").build();
      PropertyJpaEntity prop2 = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("prop2")
          .value("value2").build();
      PropertyJpaEntity prop3 = PropertyJpaEntity.builder().id(UUID.randomUUID()).name("prop3")
          .value("value3").build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id")
          .properties(new HashSet<>(Set.of(prop1, prop2, prop3))).relations(new HashSet<>())
          .build();

      Property domainProp1 = new Property(UUID.randomUUID(), "prop1", "updated-value1");
      Property domainProp4 = new Property(UUID.randomUUID(), "prop4", "value4");
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id",
          List.of(domainProp1, domainProp4), List.of());

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(2, existing.getProperties().size());
      assertTrue(existing.getProperties().stream()
          .anyMatch(p -> p.getName().equals("prop1") && p.getValue().equals("updated-value1")));
      assertTrue(existing.getProperties().stream().anyMatch(p -> p.getName().equals("prop4")));
    }
  }

  // =========================================================================
  // Relation Mapping Tests - mergeRelations()
  // =========================================================================

  @Nested
  @DisplayName("Relation Merge Tests")
  class RelationMergeTests {

    @Test
    @DisplayName("Should remove relations not present in domain entity")
    void mergeRelations_RemovesRelationsNotInDomain() {
      UUID existingId = UUID.randomUUID();
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();
      RelationJpaEntity rel2 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel2")
          .targetTemplateIdentifier("tmpl2").targetEntities(new HashSet<>()).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1, rel2))).build();

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of());
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      assertTrue(existing.getRelations().stream().anyMatch(r -> r.getName().equals("rel1")));
    }

    @Test
    @DisplayName("Should add new relation not present in existing")
    void mergeRelations_AddsNewRelation() {
      UUID existingId = UUID.randomUUID();
      UUID targetId = UUID.randomUUID();
      EntityJpaEntity targetEntity = EntityJpaEntity.builder().id(targetId)
          .templateIdentifier("tmpl1").identifier("target-id").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id"))).thenReturn(List.of(targetEntity));

      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of());
      Relation domainRel2 = new Relation(UUID.randomUUID(), "rel2", "tmpl1", List.of("target-id"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1, domainRel2));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(2, existing.getRelations().size());
      assertTrue(existing.getRelations().stream().anyMatch(r -> r.getName().equals("rel2")));
    }

    @Test
    @DisplayName("Should not update relation when no change detected")
    void mergeRelations_DoesNotUpdateUnchangedRelation() {
      UUID existingId = UUID.randomUUID();
      UUID targetId = UUID.randomUUID();
      RelationTargetJpaEntity target = new RelationTargetJpaEntity(targetId, "target-id");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target))).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      EntityJpaEntity targetEntity = EntityJpaEntity.builder().id(targetId)
          .templateIdentifier("tmpl1").identifier("target-id").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id"))).thenReturn(List.of(targetEntity));

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of("target-id"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertEquals("tmpl1", updatedRel.getTargetTemplateIdentifier());
    }

    @Test
    @DisplayName("Should update relation when target template identifier changes")
    void mergeRelations_UpdatesRelationWhenTemplateIdentifierChanges() {
      UUID existingId = UUID.randomUUID();
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("old-tmpl").targetEntities(new HashSet<>()).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "new-tmpl", List.of());
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertEquals("new-tmpl", updatedRel.getTargetTemplateIdentifier());
    }

    @Test
    @DisplayName("Should update relation when target entities change in count")
    void mergeRelations_UpdatesRelationWhenTargetCountChanges() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();
      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1))).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      UUID targetId2 = UUID.randomUUID();
      EntityJpaEntity targetEntity1 = EntityJpaEntity.builder().id(targetId1)
          .templateIdentifier("tmpl1").identifier("target-id-1").build();
      EntityJpaEntity targetEntity2 = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl1").identifier("target-id-2").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-1", "target-id-2"))).thenReturn(List.of(targetEntity1, targetEntity2));

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1",
          List.of("target-id-1", "target-id-2"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertEquals(2, updatedRel.getTargetEntities().size());
    }

    @Test
    @DisplayName("Should update relation when target entities content changes")
    void mergeRelations_UpdatesRelationWhenTargetEntitiesChange() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();
      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1))).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      UUID targetId2 = UUID.randomUUID();
      EntityJpaEntity targetEntity2 = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl1").identifier("target-id-2").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-2"))).thenReturn(List.of(targetEntity2));

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1",
          List.of("target-id-2"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertEquals(1, updatedRel.getTargetEntities().size());
      assertTrue(updatedRel.getTargetEntities().stream()
          .anyMatch(t -> t.getTargetEntityIdentifier().equals("target-id-2")));
    }
  }

  // =========================================================================
  // Relation Change Detection Tests - hasRelationChanged()
  // =========================================================================

  @Nested
  @DisplayName("Relation Change Detection Tests")
  class RelationChangeDetectionTests {

    @Test
    @DisplayName("Should detect change when target template identifier differs")
    void hasRelationChanged_DifferentTemplateIdentifier_ReturnsTrue() {
      RelationTargetJpaEntity target = new RelationTargetJpaEntity(UUID.randomUUID(), "target-id");
      RelationJpaEntity existing = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("old-tmpl").targetEntities(new HashSet<>(Set.of(target)))
          .build();

      Relation domain = new Relation(UUID.randomUUID(), "rel1", "new-tmpl", List.of("target-id"));

      boolean changed = mapper.hasRelationChanged(existing, domain);

      assertTrue(changed);
    }

    @Test
    @DisplayName("Should detect change when target entities count differs")
    void hasRelationChanged_DifferentTargetCount_ReturnsTrue() {
      UUID targetId1 = UUID.randomUUID();
      UUID targetId2 = UUID.randomUUID();
      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationJpaEntity existing = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1))).build();

      EntityJpaEntity targetEntity1 = EntityJpaEntity.builder().id(targetId1)
          .templateIdentifier("tmpl1").identifier("target-id-1").build();
      EntityJpaEntity targetEntity2 = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl1").identifier("target-id-2").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-1", "target-id-2"))).thenReturn(List.of(targetEntity1, targetEntity2));

      Relation domain = new Relation(UUID.randomUUID(), "rel1", "tmpl1",
          List.of("target-id-1", "target-id-2"));

      boolean changed = mapper.hasRelationChanged(existing, domain);

      assertTrue(changed);
    }

    @Test
    @DisplayName("Should detect change when target entities content differs")
    void hasRelationChanged_DifferentTargetContent_ReturnsTrue() {
      UUID targetId1 = UUID.randomUUID();
      UUID targetId2 = UUID.randomUUID();
      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationJpaEntity existing = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1))).build();

      EntityJpaEntity targetEntity2 = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl1").identifier("target-id-2").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-2"))).thenReturn(List.of(targetEntity2));

      Relation domain = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of("target-id-2"));

      boolean changed = mapper.hasRelationChanged(existing, domain);

      assertTrue(changed);
    }

    @Test
    @DisplayName("Should not detect change when relation is identical")
    void hasRelationChanged_IdenticalRelation_ReturnsFalse() {
      UUID targetId = UUID.randomUUID();
      RelationTargetJpaEntity target = new RelationTargetJpaEntity(targetId, "target-id");
      RelationJpaEntity existing = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target))).build();

      EntityJpaEntity targetEntity = EntityJpaEntity.builder().id(targetId)
          .templateIdentifier("tmpl1").identifier("target-id").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id"))).thenReturn(List.of(targetEntity));

      Relation domain = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of("target-id"));

      boolean changed = mapper.hasRelationChanged(existing, domain);

      assertFalse(changed);
    }

    @Test
    @DisplayName("Should handle null target entity identifiers in domain")
    void hasRelationChanged_NullTargetIdentifiers_HandlesProperly() {
      RelationJpaEntity existing = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();

      Relation domain = new Relation(UUID.randomUUID(), "rel1", "tmpl1", null);

      boolean changed = mapper.hasRelationChanged(existing, domain);

      assertFalse(changed);
    }
  }

  // =========================================================================
  // Property Mapping Tests - toDomain() and toJpa()
  // =========================================================================

  @Nested
  @DisplayName("Property Mapping Tests")
  class PropertyMappingTests {

    @Test
    @DisplayName("Should return null when JPA property is null")
    void propertyToDomain_WithNullJpaProperty_ReturnsNull() {
      assertNull(mapper.toDomain((PropertyJpaEntity) null));
    }

    @Test
    @DisplayName("Should convert JPA property to domain property")
    void propertyToDomain_WithValidJpaProperty_ReturnsDomainProperty() {
      UUID id = UUID.randomUUID();
      PropertyJpaEntity jpa = PropertyJpaEntity.builder().id(id).name("prop-name")
          .value("prop-value").build();

      Property domain = mapper.toDomain(jpa);

      assertNotNull(domain);
      assertEquals(id, domain.id());
      assertEquals("prop-name", domain.name());
      assertEquals("prop-value", domain.value());
    }

    @Test
    @DisplayName("Should return null when domain property is null")
    void propertyToJpa_WithNullDomainProperty_ReturnsNull() {
      assertNull(mapper.toJpa((Property) null));
    }

    @Test
    @DisplayName("Should convert domain property to JPA property")
    void propertyToJpa_WithValidDomainProperty_ReturnsJpaProperty() {
      UUID id = UUID.randomUUID();
      Property domain = new Property(id, "prop-name", "prop-value");

      PropertyJpaEntity jpa = mapper.toJpa(domain);

      assertNotNull(jpa);
      assertEquals(id, jpa.getId());
      assertEquals("prop-name", jpa.getName());
      assertEquals("prop-value", jpa.getValue());
    }

    @Test
    @DisplayName("Should handle null property value")
    void propertyMapping_WithNullValue_HandlesCorrectly() {
      UUID id = UUID.randomUUID();
      Property domain = new Property(id, "prop-name", null);

      PropertyJpaEntity jpa = mapper.toJpa(domain);

      assertNull(jpa.getValue());
    }
  }

  // =========================================================================
  // Relation Mapping Tests - toDomain() and toJpa()
  // =========================================================================

  @Nested
  @DisplayName("Relation Mapping Tests")
  class RelationMappingTests {

    @Test
    @DisplayName("Should return null when JPA relation is null")
    void relationToDomain_WithNullJpaRelation_ReturnsNull() {
      assertNull(mapper.toDomain((RelationJpaEntity) null));
    }

    @Test
    @DisplayName("Should convert JPA relation to domain relation with sorted identifiers")
    void relationToDomain_WithValidJpaRelation_ReturnsDomainRelation() {
      UUID id = UUID.randomUUID();
      RelationTargetJpaEntity targetZ = new RelationTargetJpaEntity(UUID.randomUUID(), "z-target");
      RelationTargetJpaEntity targetA = new RelationTargetJpaEntity(UUID.randomUUID(), "a-target");
      RelationTargetJpaEntity targetM = new RelationTargetJpaEntity(UUID.randomUUID(), "m-target");
      RelationJpaEntity jpa = RelationJpaEntity.builder().id(id).name("rel-name")
          .targetTemplateIdentifier("target-tmpl")
          .targetEntities(new HashSet<>(Set.of(targetZ, targetA, targetM))).build();

      Relation domain = mapper.toDomain(jpa);

      assertNotNull(domain);
      assertEquals(id, domain.id());
      assertEquals("rel-name", domain.name());
      assertEquals("target-tmpl", domain.targetTemplateIdentifier());
      assertEquals(3, domain.targetEntityIdentifiers().size());
      assertEquals("a-target", domain.targetEntityIdentifiers().get(0));
      assertEquals("m-target", domain.targetEntityIdentifiers().get(1));
      assertEquals("z-target", domain.targetEntityIdentifiers().get(2));
    }

    @Test
    @DisplayName("Should handle null target entities in JPA relation")
    void relationToDomain_WithNullTargetEntities_ReturnsEmptyList() {
      UUID id = UUID.randomUUID();
      RelationJpaEntity jpa = RelationJpaEntity.builder().id(id).name("rel-name")
          .targetTemplateIdentifier("target-tmpl").targetEntities(null).build();

      Relation domain = mapper.toDomain(jpa);

      assertNotNull(domain.targetEntityIdentifiers());
      assertTrue(domain.targetEntityIdentifiers().isEmpty());
    }

    @Test
    @DisplayName("Should filter out null target entity identifiers")
    void relationToDomain_WithNullIdentifiers_FiltersThemOut() {
      UUID id = UUID.randomUUID();
      RelationTargetJpaEntity validTarget = new RelationTargetJpaEntity(UUID.randomUUID(),
          "valid-id");
      RelationTargetJpaEntity nullTarget = new RelationTargetJpaEntity(UUID.randomUUID(), null);
      RelationJpaEntity jpa = RelationJpaEntity.builder().id(id).name("rel-name")
          .targetTemplateIdentifier("target-tmpl")
          .targetEntities(new HashSet<>(Set.of(validTarget, nullTarget))).build();

      Relation domain = mapper.toDomain(jpa);

      assertEquals(1, domain.targetEntityIdentifiers().size());
      assertEquals("valid-id", domain.targetEntityIdentifiers().get(0));
    }

    @Test
    @DisplayName("Should return null when domain relation is null")
    void relationToJpa_WithNullDomainRelation_ReturnsNull() {
      assertNull(mapper.toJpa((Relation) null));
    }

    @Test
    @DisplayName("Should convert domain relation to JPA relation with resolved targets")
    void relationToJpa_WithValidDomainRelation_ReturnsJpaRelation() {
      UUID id = UUID.randomUUID();
      UUID targetId = UUID.randomUUID();
      EntityJpaEntity targetEntity = EntityJpaEntity.builder().id(targetId)
          .templateIdentifier("target-tmpl").identifier("target-id").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("target-tmpl",
          List.of("target-id"))).thenReturn(List.of(targetEntity));

      Relation domain = new Relation(id, "rel-name", "target-tmpl", List.of("target-id"));

      RelationJpaEntity jpa = mapper.toJpa(domain);

      assertNotNull(jpa);
      assertEquals(id, jpa.getId());
      assertEquals("rel-name", jpa.getName());
      assertEquals("target-tmpl", jpa.getTargetTemplateIdentifier());
      assertEquals(1, jpa.getTargetEntities().size());
    }

    @Test
    @DisplayName("Should handle null target entity identifiers in domain relation")
    void relationToJpa_WithNullTargetIdentifiers_CreatesEmptySet() {
      UUID id = UUID.randomUUID();
      Relation domain = new Relation(id, "rel-name", "target-tmpl", null);

      RelationJpaEntity jpa = mapper.toJpa(domain);

      assertNotNull(jpa.getTargetEntities());
      assertTrue(jpa.getTargetEntities().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty target entity identifiers")
    void relationToJpa_WithEmptyTargetIdentifiers_CreatesEmptySet() {
      UUID id = UUID.randomUUID();
      Relation domain = new Relation(id, "rel-name", "target-tmpl", List.of());

      RelationJpaEntity jpa = mapper.toJpa(domain);

      assertNotNull(jpa.getTargetEntities());
      assertTrue(jpa.getTargetEntities().isEmpty());
    }
  }

  // =========================================================================
  // Batch Resolution Tests - resolveBatchTargetEntities()
  // =========================================================================

  @Nested
  @DisplayName("Batch Target Entity Resolution Tests")
  class BatchResolutionTests {

    @Test
    @DisplayName("Should resolve multiple target identifiers in single batch query")
    void resolveBatchTargetEntities_WithMultipleIdentifiers_ReturnsBatchResolution() {
      UUID targetId1 = UUID.randomUUID();
      UUID targetId2 = UUID.randomUUID();
      EntityJpaEntity targetEntity1 = EntityJpaEntity.builder().id(targetId1)
          .templateIdentifier("tmpl").identifier("id-1").build();
      EntityJpaEntity targetEntity2 = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl").identifier("id-2").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl",
          List.of("id-1", "id-2"))).thenReturn(List.of(targetEntity1, targetEntity2));

      Relation domain = new Relation(UUID.randomUUID(), "rel", "tmpl", List.of("id-1", "id-2"));

      RelationJpaEntity jpa = mapper.toJpa(domain);

      assertEquals(2, jpa.getTargetEntities().size());
      assertTrue(jpa.getTargetEntities().stream()
          .anyMatch(t -> t.getTargetEntityUuid().equals(targetId1)));
      assertTrue(jpa.getTargetEntities().stream()
          .anyMatch(t -> t.getTargetEntityUuid().equals(targetId2)));
      verify(entityRepository, times(1)).findAllByTemplateIdentifierAndIdentifierIn("tmpl",
          List.of("id-1", "id-2"));
    }

    @Test
    @DisplayName("Should return empty set when target identifiers list is null")
    void resolveBatchTargetEntities_WithNullIdentifiers_ReturnsEmptySet() {
      Relation domain = new Relation(UUID.randomUUID(), "rel", "tmpl", null);

      RelationJpaEntity jpa = mapper.toJpa(domain);

      assertTrue(jpa.getTargetEntities().isEmpty());
      verify(entityRepository, never()).findAllByTemplateIdentifierAndIdentifierIn(any(), any());
    }

    @Test
    @DisplayName("Should return empty set when target identifiers list is empty")
    void resolveBatchTargetEntities_WithEmptyIdentifiers_ReturnsEmptySet() {
      Relation domain = new Relation(UUID.randomUUID(), "rel", "tmpl", List.of());

      RelationJpaEntity jpa = mapper.toJpa(domain);

      assertTrue(jpa.getTargetEntities().isEmpty());
      verify(entityRepository, never()).findAllByTemplateIdentifierAndIdentifierIn(any(), any());
    }

    @Test
    @DisplayName("Should preserve only successfully resolved entities")
    void resolveBatchTargetEntities_WithPartialResolution_PreservesResolvedOnly() {
      UUID targetId1 = UUID.randomUUID();
      EntityJpaEntity targetEntity1 = EntityJpaEntity.builder().id(targetId1)
          .templateIdentifier("tmpl").identifier("id-1").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl",
          List.of("id-1", "id-2"))).thenReturn(List.of(targetEntity1));

      Relation domain = new Relation(UUID.randomUUID(), "rel", "tmpl", List.of("id-1", "id-2"));

      RelationJpaEntity jpa = mapper.toJpa(domain);

      assertEquals(1, jpa.getTargetEntities().size());
      assertEquals("id-1", jpa.getTargetEntities().iterator().next().getTargetEntityIdentifier());
    }

    @Test
    @DisplayName("Should return empty set when no entities are resolved")
    void resolveBatchTargetEntities_WithNoResolution_ReturnsEmptySet() {
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl",
          List.of("id-1", "id-2"))).thenReturn(List.of());

      Relation domain = new Relation(UUID.randomUUID(), "rel", "tmpl", List.of("id-1", "id-2"));

      RelationJpaEntity jpa = mapper.toJpa(domain);

      assertTrue(jpa.getTargetEntities().isEmpty());
    }

    @Test
    @DisplayName("Should batch resolve during merge operations")
    void resolveBatchTargetEntities_DuringMerge_UsesBatchResolution() {
      UUID existingId = UUID.randomUUID();
      UUID targetId = UUID.randomUUID();
      EntityJpaEntity targetEntity = EntityJpaEntity.builder().id(targetId)
          .templateIdentifier("tmpl1").identifier("target-id").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id"))).thenReturn(List.of(targetEntity));

      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of("target-id"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      verify(entityRepository, atLeastOnce()).findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id"));
    }
  }

  // =========================================================================
  // Integration Tests - Full Entity Lifecycle
  // =========================================================================

  @Nested
  @DisplayName("Integration Tests - Full Entity Lifecycle")
  class IntegrationTests {

    @Test
    @DisplayName("Should perform complete roundtrip: Domain -> JPA -> Domain")
    void integrationTest_RoundTrip_PreservesData() {
      UUID targetId = UUID.randomUUID();
      EntityJpaEntity targetEntity = EntityJpaEntity.builder().id(targetId)
          .templateIdentifier("target-tmpl").identifier("target-id").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("target-tmpl",
          List.of("target-id"))).thenReturn(List.of(targetEntity));

      Property domainProp = new Property(UUID.randomUUID(), "prop1", "value1");
      Relation domainRel = new Relation(UUID.randomUUID(), "rel1", "target-tmpl",
          List.of("target-id"));
      Entity originalDomain = new Entity(UUID.randomUUID(), "tmpl-id", "entity-name", "entity-id",
          List.of(domainProp), List.of(domainRel));

      EntityJpaEntity jpa = mapper.toJpa(originalDomain);
      Entity resultDomain = mapper.toDomain(jpa);

      assertEquals(originalDomain.id(), resultDomain.id());
      assertEquals(originalDomain.templateIdentifier(), resultDomain.templateIdentifier());
      assertEquals(originalDomain.name(), resultDomain.name());
      assertEquals(originalDomain.identifier(), resultDomain.identifier());
      assertEquals(originalDomain.properties().size(), resultDomain.properties().size());
      assertEquals(originalDomain.relations().size(), resultDomain.relations().size());
    }

    @Test
    @DisplayName("Should handle complete merge lifecycle with multiple changes")
    void integrationTest_MergeLifecycle_HandlesComplexScenario() {
      UUID existingId = UUID.randomUUID();
      PropertyJpaEntity existingProp1 = PropertyJpaEntity.builder().id(UUID.randomUUID())
          .name("prop1").value("old-value").build();
      PropertyJpaEntity existingProp2 = PropertyJpaEntity.builder().id(UUID.randomUUID())
          .name("prop2").value("value2").build();
      RelationJpaEntity existingRel1 = RelationJpaEntity.builder().id(UUID.randomUUID())
          .name("rel1").targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("old-name").identifier("old-id")
          .properties(new HashSet<>(Set.of(existingProp1, existingProp2)))
          .relations(new HashSet<>(Set.of(existingRel1))).build();

      UUID targetId = UUID.randomUUID();
      EntityJpaEntity targetEntity = EntityJpaEntity.builder().id(targetId)
          .templateIdentifier("tmpl1").identifier("target-id").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id"))).thenReturn(List.of(targetEntity));

      Property newProp1 = new Property(UUID.randomUUID(), "prop1", "new-value");
      Property newProp3 = new Property(UUID.randomUUID(), "prop3", "value3");
      Relation newRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of("target-id"));
      Entity domain = new Entity(UUID.randomUUID(), "new-template", "new-name", "new-id",
          List.of(newProp1, newProp3), List.of(newRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(existingId, existing.getId());
      assertEquals("new-template", existing.getTemplateIdentifier());
      assertEquals("new-name", existing.getName());
      assertEquals("new-id", existing.getIdentifier());
      assertEquals(2, existing.getProperties().size());
      assertTrue(existing.getProperties().stream()
          .anyMatch(p -> p.getName().equals("prop1") && p.getValue().equals("new-value")));
      assertTrue(existing.getProperties().stream().anyMatch(p -> p.getName().equals("prop3")));
      assertEquals(1, existing.getRelations().size());
    }

    @Test
    @DisplayName("Should handle complex scenario with null collections")
    void integrationTest_WithNullCollections_HandlesGracefully() {
      UUID existingId = UUID.randomUUID();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>()).build();

      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", null, null);

      EntityJpaEntity result = mapper.toJpaWithMerge(domain, existing);

      assertNotNull(result);
      assertTrue(result.getProperties().isEmpty());
      assertTrue(result.getRelations().isEmpty());
    }
  }

  // =========================================================================
  // Additional Relation Merge Tests for Full Coverage
  // =========================================================================

  @Nested
  @DisplayName("Advanced Relation Merge Scenarios")
  class AdvancedRelationMergeTests {

    @Test
    @DisplayName("Should add target entities to existing relation without duplicates")
    void mergeRelations_AddTargetEntities_AvoidsDuplicates() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();
      UUID targetId2 = UUID.randomUUID();
      UUID targetId3 = UUID.randomUUID();

      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationTargetJpaEntity target2 = new RelationTargetJpaEntity(targetId2, "target-id-2");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1, target2)))
          .build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      EntityJpaEntity targetEntity1 = EntityJpaEntity.builder().id(targetId1)
          .templateIdentifier("tmpl1").identifier("target-id-1").build();
      EntityJpaEntity targetEntity2 = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl1").identifier("target-id-2").build();
      EntityJpaEntity targetEntity3 = EntityJpaEntity.builder().id(targetId3)
          .templateIdentifier("tmpl1").identifier("target-id-3").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-1", "target-id-2", "target-id-3")))
              .thenReturn(List.of(targetEntity1, targetEntity2, targetEntity3));

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1",
          List.of("target-id-1", "target-id-2", "target-id-3"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertEquals(3, updatedRel.getTargetEntities().size());
    }

    @Test
    @DisplayName("Should remove target entities from existing relation")
    void mergeRelations_RemoveTargetEntities_Success() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();
      UUID targetId2 = UUID.randomUUID();

      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationTargetJpaEntity target2 = new RelationTargetJpaEntity(targetId2, "target-id-2");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1, target2)))
          .build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      EntityJpaEntity targetEntity1 = EntityJpaEntity.builder().id(targetId1)
          .templateIdentifier("tmpl1").identifier("target-id-1").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-1"))).thenReturn(List.of(targetEntity1));

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1",
          List.of("target-id-1"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertEquals(1, updatedRel.getTargetEntities().size());
      assertTrue(updatedRel.getTargetEntities().stream()
          .anyMatch(t -> t.getTargetEntityIdentifier().equals("target-id-1")));
    }

    @Test
    @DisplayName("Should clear all target entities when domain relation has empty targets")
    void mergeRelations_ClearAllTargetEntities_Success() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();

      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1))).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of());
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertTrue(updatedRel.getTargetEntities().isEmpty());
    }

    @Test
    @DisplayName("Should replace all target entities when domain relation has different targets")
    void mergeRelations_ReplaceAllTargetEntities_Success() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();
      UUID targetId2 = UUID.randomUUID();

      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1))).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      EntityJpaEntity targetEntity2 = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl1").identifier("target-id-2").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-2"))).thenReturn(List.of(targetEntity2));

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1",
          List.of("target-id-2"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertEquals(1, updatedRel.getTargetEntities().size());
      assertTrue(updatedRel.getTargetEntities().stream()
          .anyMatch(t -> t.getTargetEntityIdentifier().equals("target-id-2")));
      assertFalse(updatedRel.getTargetEntities().stream()
          .anyMatch(t -> t.getTargetEntityIdentifier().equals("target-id-1")));
    }

    @Test
    @DisplayName("Should handle mixed operations on relations: add, update, remove")
    void mergeRelations_MixedOperations_Success() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();
      UUID targetId2 = UUID.randomUUID();

      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1))).build();
      RelationJpaEntity rel2 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel2")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1, rel2))).build();

      EntityJpaEntity targetEntity2 = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl1").identifier("target-id-2").build();
      EntityJpaEntity targetEntity1 = EntityJpaEntity.builder().id(targetId1)
          .templateIdentifier("tmpl1").identifier("target-id-1").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-2"))).thenReturn(List.of(targetEntity2));
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl2",
          List.of("target-id-1"))).thenReturn(List.of(targetEntity1));

      // rel1: update target entities (change from id-1 to id-2)
      // rel2: remove (not in domain)
      // rel3: add new
      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1",
          List.of("target-id-2"));
      Relation domainRel3 = new Relation(UUID.randomUUID(), "rel3", "tmpl2",
          List.of("target-id-1"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1, domainRel3));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(2, existing.getRelations().size());
      assertTrue(existing.getRelations().stream().anyMatch(r -> r.getName().equals("rel1")));
      assertTrue(existing.getRelations().stream().anyMatch(r -> r.getName().equals("rel3")));
      assertFalse(existing.getRelations().stream().anyMatch(r -> r.getName().equals("rel2")));
    }

    @Test
    @DisplayName("Should not update relation when target entities are identical regardless of order")
    void mergeRelations_IdenticalTargetsDifferentOrder_NoUpdate() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();
      UUID targetId2 = UUID.randomUUID();

      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationTargetJpaEntity target2 = new RelationTargetJpaEntity(targetId2, "target-id-2");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1, target2)))
          .build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      EntityJpaEntity targetEntity1 = EntityJpaEntity.builder().id(targetId1)
          .templateIdentifier("tmpl1").identifier("target-id-1").build();
      EntityJpaEntity targetEntity2 = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl1").identifier("target-id-2").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-2", "target-id-1"))).thenReturn(List.of(targetEntity1, targetEntity2));

      // Same targets, different order
      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1",
          List.of("target-id-2", "target-id-1"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertEquals(2, updatedRel.getTargetEntities().size());
      assertTrue(updatedRel.getTargetEntities().contains(target1));
      assertTrue(updatedRel.getTargetEntities().contains(target2));
    }

    @Test
    @DisplayName("Should handle relation with null target identifiers during merge")
    void mergeRelations_WithNullTargetIdentifiers_ClearsTargets() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();

      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1))).build();
      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1))).build();

      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1", null);
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(1, existing.getRelations().size());
      RelationJpaEntity updatedRel = existing.getRelations().iterator().next();
      assertTrue(updatedRel.getTargetEntities().isEmpty());
    }

    @Test
    @DisplayName("Should detect no change when both existing and domain have empty targets")
    void hasRelationChanged_BothHaveEmptyTargets_ReturnsFalse() {
      RelationJpaEntity existing = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>()).build();

      Relation domain = new Relation(UUID.randomUUID(), "rel1", "tmpl1", List.of());

      boolean changed = mapper.hasRelationChanged(existing, domain);

      assertFalse(changed);
    }

    @Test
    @DisplayName("Should handle multiple relations with various target entity changes")
    void mergeRelations_MultipleRelationsVariousChanges_Success() {
      UUID existingId = UUID.randomUUID();
      UUID targetId1 = UUID.randomUUID();
      UUID targetId2 = UUID.randomUUID();
      UUID targetId3 = UUID.randomUUID();

      RelationTargetJpaEntity target1 = new RelationTargetJpaEntity(targetId1, "target-id-1");
      RelationTargetJpaEntity target2 = new RelationTargetJpaEntity(targetId2, "target-id-2");

      RelationJpaEntity rel1 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel1")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target1))).build();
      RelationJpaEntity rel2 = RelationJpaEntity.builder().id(UUID.randomUUID()).name("rel2")
          .targetTemplateIdentifier("tmpl1").targetEntities(new HashSet<>(Set.of(target2))).build();

      EntityJpaEntity existing = EntityJpaEntity.builder().id(existingId)
          .templateIdentifier("template").name("name").identifier("id").properties(new HashSet<>())
          .relations(new HashSet<>(Set.of(rel1, rel2))).build();

      EntityJpaEntity targetEntity1 = EntityJpaEntity.builder().id(targetId1)
          .templateIdentifier("tmpl1").identifier("target-id-1").build();
      EntityJpaEntity targetEntity2Resolved = EntityJpaEntity.builder().id(targetId2)
          .templateIdentifier("tmpl1").identifier("target-id-2").build();
      EntityJpaEntity targetEntity3 = EntityJpaEntity.builder().id(targetId3)
          .templateIdentifier("tmpl1").identifier("target-id-3").build();
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-1", "target-id-3"))).thenReturn(List.of(targetEntity1, targetEntity3));
      when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("tmpl1",
          List.of("target-id-2"))).thenReturn(List.of(targetEntity2Resolved));

      // rel1: add target-id-3 to existing target-id-1
      // rel2: keep unchanged (target-id-2)
      Relation domainRel1 = new Relation(UUID.randomUUID(), "rel1", "tmpl1",
          List.of("target-id-1", "target-id-3"));
      Relation domainRel2 = new Relation(UUID.randomUUID(), "rel2", "tmpl1",
          List.of("target-id-2"));
      Entity domain = new Entity(UUID.randomUUID(), "template", "name", "id", List.of(),
          List.of(domainRel1, domainRel2));

      mapper.toJpaWithMerge(domain, existing);

      assertEquals(2, existing.getRelations().size());

      RelationJpaEntity updatedRel1 = existing.getRelations().stream()
          .filter(r -> r.getName().equals("rel1")).findFirst().orElse(null);
      assertNotNull(updatedRel1);
      assertEquals(2, updatedRel1.getTargetEntities().size());

      RelationJpaEntity updatedRel2 = existing.getRelations().stream()
          .filter(r -> r.getName().equals("rel2")).findFirst().orElse(null);
      assertNotNull(updatedRel2);
      assertEquals(1, updatedRel2.getTargetEntities().size());
    }
  }
}
