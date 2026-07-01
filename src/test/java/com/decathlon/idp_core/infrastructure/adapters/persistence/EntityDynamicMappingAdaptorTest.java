package com.decathlon.idp_core.infrastructure.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityDynamicMappingPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_mapping.EntityDynamicMappingJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityDynamicMappingRepository;

@DisplayName("EntityDynamicMappingAdaptor Tests")
@ExtendWith(MockitoExtension.class)
class EntityDynamicMappingAdaptorTest {

  @Mock
  private JpaEntityDynamicMappingRepository jpaEntityDynamicMappingRepository;

  @Mock
  private EntityDynamicMappingPersistenceMapper entityDynamicMappingPersistenceMapper;

  private EntityDynamicMappingAdaptor adaptor;

  @BeforeEach
  void setUp() {
    adaptor = new EntityDynamicMappingAdaptor(jpaEntityDynamicMappingRepository,
        entityDynamicMappingPersistenceMapper);
  }

  // ---------------------------------------------------------------------------
  // existsByIdentifier
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("existsByIdentifier")
  class ExistsByIdentifierTests {

    @Test
    @DisplayName("Should return true when mapping exists")
    void shouldReturnTrueWhenExists() {
      when(jpaEntityDynamicMappingRepository.existsByIdentifier("my-mapping")).thenReturn(true);
      assertThat(adaptor.existsByIdentifier("my-mapping")).isTrue();
    }

    @Test
    @DisplayName("Should return false when mapping does not exist")
    void shouldReturnFalseWhenNotExists() {
      when(jpaEntityDynamicMappingRepository.existsByIdentifier("unknown")).thenReturn(false);
      assertThat(adaptor.existsByIdentifier("unknown")).isFalse();
    }
  }

  // ---------------------------------------------------------------------------
  // existsByTemplateIdentifier
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("existsByTemplateIdentifier")
  class ExistsByTemplateIdentifierTests {

    @Test
    @DisplayName("Should return true when mappings exist for template")
    void shouldReturnTrueWhenExists() {
      when(jpaEntityDynamicMappingRepository.existsByTemplateIdentifier("web-service"))
          .thenReturn(true);
      assertThat(adaptor.existsByTemplateIdentifier("web-service")).isTrue();
    }

    @Test
    @DisplayName("Should return false when no mappings for template")
    void shouldReturnFalseWhenNotExists() {
      when(jpaEntityDynamicMappingRepository.existsByTemplateIdentifier("unknown"))
          .thenReturn(false);
      assertThat(adaptor.existsByTemplateIdentifier("unknown")).isFalse();
    }
  }

  // ---------------------------------------------------------------------------
  // findByIdentifier
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("findByIdentifier")
  class FindByIdentifierTests {

    @Test
    @DisplayName("Should return mapped domain when entity found")
    void shouldReturnDomainWhenFound() {
      var jpa = buildJpaEntity("my-mapping");
      var domain = buildDomainMapping("my-mapping");
      when(jpaEntityDynamicMappingRepository.findByIdentifier("my-mapping"))
          .thenReturn(Optional.of(jpa));
      when(entityDynamicMappingPersistenceMapper.toDomain(jpa)).thenReturn(domain);

      Optional<EntityDynamicMapping> result = adaptor.findByIdentifier("my-mapping");

      assertThat(result).isPresent().contains(domain);
    }

    @Test
    @DisplayName("Should return empty optional when entity not found")
    void shouldReturnEmptyWhenNotFound() {
      when(jpaEntityDynamicMappingRepository.findByIdentifier("unknown"))
          .thenReturn(Optional.empty());

      Optional<EntityDynamicMapping> result = adaptor.findByIdentifier("unknown");

      assertThat(result).isEmpty();
    }
  }

  // ---------------------------------------------------------------------------
  // findByTemplateIdentifier
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("findByTemplateIdentifier")
  class FindByTemplateIdentifierTests {

    @Test
    @DisplayName("Should return all domain mappings for a given template identifier")
    void shouldReturnMappingsForTemplate() {
      var jpa1 = buildJpaEntity("mapping-1");
      var jpa2 = buildJpaEntity("mapping-2");
      var domain1 = buildDomainMapping("mapping-1");
      var domain2 = buildDomainMapping("mapping-2");

      when(jpaEntityDynamicMappingRepository.findByTemplateIdentifier("web-service"))
          .thenReturn(List.of(jpa1, jpa2));
      when(entityDynamicMappingPersistenceMapper.toDomain(jpa1)).thenReturn(domain1);
      when(entityDynamicMappingPersistenceMapper.toDomain(jpa2)).thenReturn(domain2);

      List<EntityDynamicMapping> result = adaptor.findByTemplateIdentifier("web-service");

      assertThat(result).hasSize(2).containsExactly(domain1, domain2);
    }

    @Test
    @DisplayName("Should return empty list when no mappings for template")
    void shouldReturnEmptyListWhenNone() {
      when(jpaEntityDynamicMappingRepository.findByTemplateIdentifier("unknown"))
          .thenReturn(List.of());

      List<EntityDynamicMapping> result = adaptor.findByTemplateIdentifier("unknown");

      assertThat(result).isEmpty();
    }
  }

  // ---------------------------------------------------------------------------
  // save
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("save")
  class SaveTests {

    @Test
    @DisplayName("Should convert to JPA, save, then convert back to domain")
    void shouldSaveAndReturnDomain() {
      var domain = buildDomainMapping("my-mapping");
      var jpa = buildJpaEntity("my-mapping");
      var savedJpa = buildJpaEntity("my-mapping");
      var savedDomain = buildDomainMapping("my-mapping");

      when(entityDynamicMappingPersistenceMapper.toJpa(domain)).thenReturn(jpa);
      when(jpaEntityDynamicMappingRepository.save(jpa)).thenReturn(savedJpa);
      when(entityDynamicMappingPersistenceMapper.toDomain(savedJpa)).thenReturn(savedDomain);

      EntityDynamicMapping result = adaptor.save(domain);

      assertThat(result).isEqualTo(savedDomain);
      verify(jpaEntityDynamicMappingRepository).save(jpa);
    }
  }

  // ---------------------------------------------------------------------------
  // findAll (paginated)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("findAll")
  class FindAllTests {

    @Test
    @DisplayName("Should return paginated domain mappings")
    void shouldReturnPaginatedMappings() {
      var pageable = PageRequest.of(0, 10);
      var jpa = buildJpaEntity("my-mapping");
      var domain = buildDomainMapping("my-mapping");
      var jpaPage = new PageImpl<>(List.of(jpa), pageable, 1);

      when(jpaEntityDynamicMappingRepository.findAll(pageable)).thenReturn(jpaPage);
      when(entityDynamicMappingPersistenceMapper.toDomain(jpa)).thenReturn(domain);

      var result = adaptor.findAll(pageable);

      assertThat(result.getContent()).hasSize(1).containsExactly(domain);
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return empty page when no mappings exist")
    void shouldReturnEmptyPage() {
      var pageable = PageRequest.of(0, 10);
      when(jpaEntityDynamicMappingRepository.findAll(pageable))
          .thenReturn(new PageImpl<>(List.of(), pageable, 0));

      var result = adaptor.findAll(pageable);

      assertThat(result.getContent()).isEmpty();
    }
  }

  // ---------------------------------------------------------------------------
  // deleteByIdentifier
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("deleteByIdentifier")
  class DeleteByIdentifierTests {

    @Test
    @DisplayName("Should delegate delete to JPA repository")
    void shouldDelegateDeleteToRepository() {
      adaptor.deleteByIdentifier("my-mapping");
      verify(jpaEntityDynamicMappingRepository).deleteByIdentifier("my-mapping");
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private EntityDynamicMappingJpaEntity buildJpaEntity(String identifier) {
    return new EntityDynamicMappingJpaEntity(UUID.randomUUID(), identifier, "web-service",
        ".filter", "name", "desc", ".id", ".title", "{}", "{}");
  }

  private EntityDynamicMapping buildDomainMapping(String identifier) {
    return new EntityDynamicMapping(UUID.randomUUID(), identifier, "web-service", ".filter", "name",
        "desc", ".id", ".title", Map.of(), Map.of());
  }
}
