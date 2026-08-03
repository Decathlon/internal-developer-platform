package com.decathlon.idp_core.domain.model.inbound_connectors.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

/**
 * Unit tests for {@link WebhookConnector} invariants.
 */
@DisplayName("WebhookConnector Tests")
class WebhookConnectorTest {

  @Test
  @DisplayName("Should default mappings to empty list when mappings are null")
  void shouldDefaultMappingsToEmptyListWhenMappingsAreNull() {
    WebhookConnector connector = new WebhookConnector(UUID.randomUUID(), "github-dora",
        "GitHub DORA", "desc", false, null,
        new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));

    assertThat(connector.mappings()).isEmpty();
    assertThat(connector.enabled()).isFalse();
  }

  @Test
  @DisplayName("Should keep mappings empty when constructed with an empty list")
  void shouldKeepMappingsEmptyWhenMappingsAreEmpty() {
    WebhookConnector connector = new WebhookConnector(UUID.randomUUID(), "github-dora",
        "GitHub DORA", "desc", false, List.of(),
        new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));

    assertThat(connector.mappings()).isEmpty();
    assertThat(connector.enabled()).isFalse();
  }
}
