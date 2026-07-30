package com.decathlon.idp_core.domain.service.principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;

/// Unit tests for PrincipalProvisioningService verifying JIT provisioning logic.
@ExtendWith(MockitoExtension.class)
class PrincipalProvisioningServiceTest {

  @Mock
  private EntityRepositoryPort entityRepository;

  @InjectMocks
  private PrincipalProvisioningService provisioningService;

  @Test
  void shouldCreateNewPrincipalOnFirstAuthentication() {
    // Given: New human principal (not in catalog)
    PrincipalInfo principalInfo = new PrincipalInfo("alice", PrincipalKind.HUMAN, "Alice Dupont",
        Map.of("email", "alice@decathlon.com"), List.of("platform-team"));

    when(entityRepository.findByTemplateIdentifierAndIdentifier("principal", "alice"))
        .thenReturn(Optional.empty());

    when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("team",
        List.of("platform-team")))
            .thenReturn(List.of(new Entity(UUID.randomUUID(), "team", "Platform Team",
                "platform-team", List.of(), List.of())));

    UUID generatedId = UUID.randomUUID();
    when(entityRepository.save(any(Entity.class))).thenAnswer(invocation -> new Entity(generatedId,
        invocation.getArgument(0, Entity.class).templateIdentifier(),
        invocation.getArgument(0, Entity.class).name(),
        invocation.getArgument(0, Entity.class).identifier(),
        invocation.getArgument(0, Entity.class).properties(),
        invocation.getArgument(0, Entity.class).relations()));

    // When: Provision principal
    provisioningService.provisionPrincipal(principalInfo);

    // Then: New principal entity created
    ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(entityRepository).save(entityCaptor.capture());

    Entity savedEntity = entityCaptor.getValue();
    assertThat(savedEntity.templateIdentifier()).isEqualTo("principal");
    assertThat(savedEntity.identifier()).isEqualTo("alice");
    assertThat(savedEntity.name()).isEqualTo("Alice Dupont");

    // Verify properties
    assertThat(savedEntity.properties()).extracting(Property::name).contains("kind", "email");
    assertThat(savedEntity.properties()).extracting(Property::value).contains("HUMAN",
        "alice@decathlon.com");

    // Verify relations
    assertThat(savedEntity.relations()).hasSize(1);
    assertThat(savedEntity.relations().get(0).name()).isEqualTo("member_of");
    assertThat(savedEntity.relations().get(0).targetTemplateIdentifier()).isEqualTo("team");
    assertThat(savedEntity.relations().get(0).targetEntityIdentifiers()).contains("platform-team");
  }

  @Test
  void shouldCreateServiceAccountPrincipal() {
    // Given: New service account principal
    PrincipalInfo principalInfo = new PrincipalInfo("github-connector",
        PrincipalKind.SERVICE_ACCOUNT, "GitHub Actions Webhook",
        Map.of("client_id", "github-connector", "origin", "github"), List.of("devops-tools"));

    when(entityRepository.findByTemplateIdentifierAndIdentifier("principal", "github-connector"))
        .thenReturn(Optional.empty());

    UUID generatedId = UUID.randomUUID();
    when(entityRepository.save(any(Entity.class))).thenAnswer(invocation -> new Entity(generatedId,
        invocation.getArgument(0, Entity.class).templateIdentifier(),
        invocation.getArgument(0, Entity.class).name(),
        invocation.getArgument(0, Entity.class).identifier(),
        invocation.getArgument(0, Entity.class).properties(),
        invocation.getArgument(0, Entity.class).relations()));

    // When: Provision service account
    provisioningService.provisionPrincipal(principalInfo);

    // Then: Service account entity created
    ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(entityRepository).save(entityCaptor.capture());

    Entity savedEntity = entityCaptor.getValue();
    assertThat(savedEntity.templateIdentifier()).isEqualTo("principal");
    assertThat(savedEntity.identifier()).isEqualTo("github-connector");
    assertThat(savedEntity.name()).isEqualTo("GitHub Actions Webhook");

    // Verify properties
    assertThat(savedEntity.properties()).extracting(Property::name).contains("kind", "client_id",
        "origin");
    assertThat(savedEntity.properties()).extracting(Property::value).contains("SERVICE_ACCOUNT",
        "github-connector", "github");
  }

  @Test
  void shouldHandlePrincipalWithNoGroups() {
    // Given: Principal without group membership
    PrincipalInfo principalInfo = new PrincipalInfo("bob", PrincipalKind.HUMAN, "Bob Smith",
        Map.of("email", "bob@example.com"), List.of());

    when(entityRepository.findByTemplateIdentifierAndIdentifier("principal", "bob"))
        .thenReturn(Optional.empty());

    UUID generatedId = UUID.randomUUID();
    when(entityRepository.save(any(Entity.class))).thenAnswer(invocation -> new Entity(generatedId,
        invocation.getArgument(0, Entity.class).templateIdentifier(),
        invocation.getArgument(0, Entity.class).name(),
        invocation.getArgument(0, Entity.class).identifier(),
        invocation.getArgument(0, Entity.class).properties(),
        invocation.getArgument(0, Entity.class).relations()));

    // When: Provision principal
    provisioningService.provisionPrincipal(principalInfo);

    // Then: Principal created without relations
    ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(entityRepository).save(entityCaptor.capture());

    Entity savedEntity = entityCaptor.getValue();
    assertThat(savedEntity.relations()).isEmpty();
  }

  @Test
  void shouldCreateNewPrincipalOnFirstAuthenticationFromUnkownGroups() {
    // Given: New human principal (not in catalog)
    PrincipalInfo principalInfo = new PrincipalInfo("alice", PrincipalKind.HUMAN, "Alice Dupont",
        Map.of("email", "alice@decathlon.com"), List.of("platform-team"));

    when(entityRepository.findByTemplateIdentifierAndIdentifier("principal", "alice"))
        .thenReturn(Optional.empty());

    when(entityRepository.findAllByTemplateIdentifierAndIdentifierIn("team",
        List.of("platform-team"))).thenReturn(List.of());

    UUID generatedId = UUID.randomUUID();
    when(entityRepository.save(any(Entity.class))).thenAnswer(invocation -> new Entity(generatedId,
        invocation.getArgument(0, Entity.class).templateIdentifier(),
        invocation.getArgument(0, Entity.class).name(),
        invocation.getArgument(0, Entity.class).identifier(),
        invocation.getArgument(0, Entity.class).properties(),
        invocation.getArgument(0, Entity.class).relations()));

    // When: Provision principal
    provisioningService.provisionPrincipal(principalInfo);

    // Then: New principal entity created
    ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
    verify(entityRepository).save(entityCaptor.capture());

    Entity savedEntity = entityCaptor.getValue();
    assertThat(savedEntity.templateIdentifier()).isEqualTo("principal");
    assertThat(savedEntity.identifier()).isEqualTo("alice");
    assertThat(savedEntity.name()).isEqualTo("Alice Dupont");

    // Verify properties
    assertThat(savedEntity.properties()).extracting(Property::name).contains("kind", "email");
    assertThat(savedEntity.properties()).extracting(Property::value).contains("HUMAN",
        "alice@decathlon.com");

    // verify relations are empty since the group does not exist
    assertThat(savedEntity.relations()).isEmpty();
  }

  @Test
  void shouldRetrievePrincipalByIdentifier() {
    // Given: Existing principal in catalog
    UUID principalId = UUID.randomUUID();
    Entity principal = new Entity(principalId, "principal", "Alice Dupont", "alice",
        List.of(new Property(UUID.randomUUID(), "kind", "HUMAN")), List.of());

    when(entityRepository.findByTemplateIdentifierAndIdentifier("principal", "alice"))
        .thenReturn(Optional.of(principal));

    // When: Retrieve principal
    Optional<Entity> result = provisioningService.getPrincipal("alice");

    // Then: Principal returned
    assertThat(result).isPresent();
    assertThat(result.get().identifier()).isEqualTo("alice");
  }

  @Test
  void shouldReturnEmptyWhenPrincipalNotFound() {
    // Given: Principal does not exist
    when(entityRepository.findByTemplateIdentifierAndIdentifier("principal", "unknown"))
        .thenReturn(Optional.empty());

    // When: Retrieve non-existent principal
    Optional<Entity> result = provisioningService.getPrincipal("unknown");

    // Then: Empty optional returned
    assertThat(result).isEmpty();
  }
}
