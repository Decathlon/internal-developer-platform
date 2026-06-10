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
  @DisplayName("Should default mappings to empty list and disable connector when mappings are null")
  void shouldDefaultMappingsToEmptyListAndDisableWhenMappingsAreNull() {
    WebhookConnector connector = new WebhookConnector(UUID.randomUUID(), "github-dora",
        "GitHub DORA", "desc", true, null, new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));

    assertThat(connector.mappings()).isEmpty();
    assertThat(connector.enabled()).isFalse();
  }

  @Test
  @DisplayName("Should disable connector when mappings are empty")
  void shouldDisableWhenMappingsAreEmpty() {
    WebhookConnector connector = new WebhookConnector(UUID.randomUUID(), "github-dora",
        "GitHub DORA", "desc", true, List.of(),
        new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));

    assertThat(connector.mappings()).isEmpty();
    assertThat(connector.enabled()).isFalse();
  }
}
