package com.decathlon.idp_core.domain.service.webhook;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("WebhookConnectorService Tests")
@ExtendWith(MockitoExtension.class)
class WebhookConnectorServiceTest {

    @Mock
    private WebhookConnectorRepositoryPort webhookConnectorRepositoryPort;

    @Mock
    private WebhookConnectorValidationService webhookConnectorValidationService;

    private WebhookConnectorService service;

    @BeforeEach
    void setUp() {
        service = new WebhookConnectorService(webhookConnectorRepositoryPort, webhookConnectorValidationService);
    }

    @Nested
    @DisplayName("getWebhookConnector")
    class GetWebhookConnectorTests {

        @Test
        @DisplayName("Should return connector when it exists")
        void shouldReturnConnectorWhenExists() {
            WebhookConnector existing = buildWebhookConnector(UUID.randomUUID(), "github-dora", "GitHub DORA", "desc", true);
            when(webhookConnectorRepositoryPort.findByIdentifier("github-dora"))
                    .thenReturn(Optional.of(existing));

            WebhookConnector result = service.getWebhookConnector("github-dora");

            assertThat(result).isEqualTo(existing);
        }

        @Test
        @DisplayName("Should throw WebhookConnectorNotFoundException when not found")
        void shouldThrowWhenConnectorNotFound() {
            when(webhookConnectorRepositoryPort.findByIdentifier("unknown"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getWebhookConnector("unknown"))
                    .isInstanceOf(WebhookConnectorNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }

    @Nested
    @DisplayName("createWebhookConnector")
    class CreateWebhookConnectorTests {

        @Test
        @DisplayName("Should validate then save and return the connector")
        void shouldValidateAndSave() {
            WebhookConnector toCreate = buildWebhookConnector(null, "github-dora", "GitHub DORA", "desc", true);
            WebhookConnector saved = buildWebhookConnector(UUID.randomUUID(), "github-dora", "GitHub DORA", "desc", true);
            when(webhookConnectorRepositoryPort.save(any())).thenReturn(saved);

            WebhookConnector result = service.createWebhookConnector(toCreate);

            verify(webhookConnectorValidationService).validateWebhookConnectorForCreation(toCreate);
            verify(webhookConnectorRepositoryPort).save(toCreate);
            assertThat(result.id()).isNotNull();
            assertThat(result.identifier()).isEqualTo("github-dora");
        }

        @Test
        @DisplayName("Should NOT save when validation throws")
        void shouldNotSaveWhenValidationFails() {
            WebhookConnector toCreate = buildWebhookConnector(null, "github-dora", "GitHub DORA", "desc", true);
            org.mockito.Mockito.doThrow(new RuntimeException("validation error"))
                    .when(webhookConnectorValidationService).validateWebhookConnectorForCreation(toCreate);

            assertThatThrownBy(() -> service.createWebhookConnector(toCreate))
                    .hasMessageContaining("validation error");

            verify(webhookConnectorRepositoryPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateWebhookConnector")
    class UpdateWebhookConnectorTests {

        private static final UUID EXISTING_ID = UUID.randomUUID();
        private static final String IDENTIFIER = "github-dora";

        @Test
        @DisplayName("Should preserve id and identifier from the stored connector")
        void shouldPreserveIdAndIdentifier() {
            WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old title", "Old desc", true);
            WebhookConnector incoming = buildWebhookConnector(null, "ignored-from-body", "New title", "New desc", false);

            when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER)).thenReturn(Optional.of(existing));
            when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WebhookConnector result = service.updateWebhookConnector(IDENTIFIER, incoming);

            assertThat(result.id()).isEqualTo(EXISTING_ID);
            assertThat(result.identifier()).isEqualTo(IDENTIFIER);
        }

        @Test
        @DisplayName("Should apply updated fields from the incoming connector")
        void shouldApplyIncomingFields() {
            WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old title", "Old desc", true);
            WebhookConnector incoming = buildWebhookConnector(null, IDENTIFIER, "New title", "New desc", false);

            when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER)).thenReturn(Optional.of(existing));
            when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WebhookConnector result = service.updateWebhookConnector(IDENTIFIER, incoming);

            assertThat(result.title()).isEqualTo("New title");
            assertThat(result.description()).isEqualTo("New desc");
            assertThat(result.enabled()).isFalse();
        }

        @Test
        @DisplayName("Should delegate validation before saving")
        void shouldDelegateValidationBeforeSave() {
            WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old title", "Old desc", true);
            WebhookConnector incoming = buildWebhookConnector(null, IDENTIFIER, "New title", "New desc", false);

            when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER)).thenReturn(Optional.of(existing));
            when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateWebhookConnector(IDENTIFIER, incoming);

            InOrder order = org.mockito.Mockito.inOrder(webhookConnectorValidationService, webhookConnectorRepositoryPort);
            order.verify(webhookConnectorValidationService).validateWebhookConnectorForUpdate(existing, incoming);
            order.verify(webhookConnectorRepositoryPort).save(any());
        }

        @Test
        @DisplayName("Should save the merged connector with correct fields")
        void shouldSaveMergedConnector() {
            WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old title", "Old desc", true);
            WebhookConnector incoming = buildWebhookConnector(null, IDENTIFIER, "New title", "New desc", false);

            when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER)).thenReturn(Optional.of(existing));
            when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateWebhookConnector(IDENTIFIER, incoming);

            var captor = ArgumentCaptor.forClass(WebhookConnector.class);
            verify(webhookConnectorRepositoryPort).save(captor.capture());
            var saved = captor.getValue();

            assertThat(saved.id()).isEqualTo(EXISTING_ID);
            assertThat(saved.identifier()).isEqualTo(IDENTIFIER);
            assertThat(saved.title()).isEqualTo("New title");
            assertThat(saved.description()).isEqualTo("New desc");
            assertThat(saved.enabled()).isFalse();
        }

        @Test
        @DisplayName("Should throw WebhookConnectorNotFoundException when connector is missing")
        void shouldThrowWhenConnectorMissing() {
            var incoming = buildWebhookConnector(null, IDENTIFIER, "New title", "New desc", true);
            when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateWebhookConnector(IDENTIFIER, incoming))
                    .isInstanceOf(WebhookConnectorNotFoundException.class)
                    .hasMessageContaining(IDENTIFIER);

            verify(webhookConnectorValidationService, never()).validateWebhookConnectorForUpdate(any(), any());
            verify(webhookConnectorRepositoryPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteWebhookConnector")
    class DeleteWebhookConnectorTests {

        @Test
        @DisplayName("Should validate existence then delete")
        void shouldValidateAndDelete() {
            service.deleteWebhookConnector("github-dora");

            var order = org.mockito.Mockito.inOrder(webhookConnectorValidationService, webhookConnectorRepositoryPort);
            order.verify(webhookConnectorValidationService).validateIdentifierExists("github-dora");
            order.verify(webhookConnectorRepositoryPort).deleteByIdentifier("github-dora");
        }

        @Test
        @DisplayName("Should NOT delete when validation throws")
        void shouldNotDeleteWhenValidationFails() {
            org.mockito.Mockito.doThrow(new WebhookConnectorNotFoundException("github-dora not found"))
                    .when(webhookConnectorValidationService).validateIdentifierExists("github-dora");

            assertThatThrownBy(() -> service.deleteWebhookConnector("github-dora"))
                    .isInstanceOf(WebhookConnectorNotFoundException.class);

            verify(webhookConnectorRepositoryPort, never()).deleteByIdentifier(any());
        }
    }

    @Nested
    @DisplayName("getAllWebhookConnector")
    class GetAllWebhookConnectorTests {

        @Test
        @DisplayName("Should return paginated connectors from repository")
        void shouldReturnPaginatedConnectors() {
            PageRequest pageable = PageRequest.of(0, 10);
            WebhookConnector c1 = buildWebhookConnector(UUID.randomUUID(), "connector-a", "A", "desc", true);
            WebhookConnector c2 = buildWebhookConnector(UUID.randomUUID(), "connector-b", "B", "desc", false);
            var page = new PageImpl<>(List.of(c1, c2), pageable, 2);
            when(webhookConnectorRepositoryPort.findAll(pageable)).thenReturn(page);

            Page<WebhookConnector> result = service.getAllWebhookConnector(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty page when no connectors exist")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(webhookConnectorRepositoryPort.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<WebhookConnector> result = service.getAllWebhookConnector(pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    private WebhookConnector buildWebhookConnector(UUID id, String identifier, String title, String description, boolean enabled) {
        EntityDynamicMapping mapping = new EntityDynamicMapping(
                "deployment",
                ".eventType == \"DEPLOYED\"",
                ".id",
                ".name",
                Map.of("environment", ".env"),
                Map.of()
        );
        WebhookSecurity security = new WebhookSecurity(
                WebhookSecurityType.HMAC_SHA256,
                Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "MY_SECRET")
        );
        return new WebhookConnector(id, identifier, title, description, enabled, List.of(mapping), security);
    }
}
