package com.decathlon.idp_core.infrastructure.adapters.api.mapper.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookEntityMappingDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookMappingDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookSecurityContractDtoIn;

@DisplayName("InboundWebhookMapper Tests")
class InboundWebhookMapperTest {

  private final InboundWebhookMapper mapper = new InboundWebhookMapper();

  @Test
  @DisplayName("Should use path identifier for update mapping")
  void shouldUsePathIdentifierForUpdateMapping() {
    var request = new InboundWebhookCreateDtoIn("identifier_from_body", "GitHub DORA",
        "Collect deployment events", true,
        List.of(new InboundWebhookMappingDtoIn("deployment", ".eventType == \"DEPLOYED\"",
            new InboundWebhookEntityMappingDtoIn(".id", ".name", Map.of("environment", ".env"),
                Map.of("service", ".service")))),
        new InboundWebhookSecurityContractDtoIn("HMAC_SHA256", Map.of("header_name",
            "X-Hub-Signature-256", "secret_alias", "MY_SECRET", "prefix", "sha256=")));

    WebhookConnector domain = mapper.toDomainForUpdate("identifier_from_path", request);

    assertThat(domain.id()).isNull();
    assertThat(domain.identifier()).isEqualTo("identifier_from_path");
    assertThat(domain.title()).isEqualTo("GitHub DORA");
    assertThat(domain.mappings()).hasSize(1);
    assertThat(domain.security().type()).isEqualTo(WebhookSecurityType.HMAC_SHA256);
    assertThat(domain.security().config()).containsEntry("prefix", "sha256=");
  }

  @Test
  @DisplayName("Should throw for unknown security type")
  void shouldThrowForUnknownSecurityType() {
    var request = new InboundWebhookCreateDtoIn("my-connector", "Custom Security",
        "Uses custom security", true,
        List.of(new InboundWebhookMappingDtoIn("deployment", "true",
            new InboundWebhookEntityMappingDtoIn(".id", ".name", Map.of(), Map.of()))),
        new InboundWebhookSecurityContractDtoIn("CUSTOM_UNKNOWN_TYPE",
            Map.of("customKey", "customValue")));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.toDomain(request)).isInstanceOf(
        com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
        .hasMessageContaining("CUSTOM_UNKNOWN_TYPE");
  }

  @Test
  @DisplayName("Should map NONE security type explicitly")
  void shouldMapNoneSecurityTypeExplicitly() {
    var request = new InboundWebhookCreateDtoIn("my-connector", "No Auth",
        "Webhook without authentication", true,
        List.of(new InboundWebhookMappingDtoIn("deployment", "true",
            new InboundWebhookEntityMappingDtoIn(".id", ".name", Map.of(), Map.of()))),
        new InboundWebhookSecurityContractDtoIn("NONE", Map.of()));

    var domain = mapper.toDomain(request);

    assertThat(domain.security().type()).isEqualTo(WebhookSecurityType.NONE);
    assertThat(domain.security().config()).isEmpty();
  }

  @Test
  @DisplayName("Should default to NONE when security section is missing")
  void shouldDefaultToNoneWhenSecurityIsMissing() {
    var request = new InboundWebhookCreateDtoIn("my-connector", "No Auth",
        "Webhook without authentication", true, List.of(new InboundWebhookMappingDtoIn("deployment",
            "true", new InboundWebhookEntityMappingDtoIn(".id", ".name", Map.of(), Map.of()))),
        null);

    var domain = mapper.toDomain(request);

    assertThat(domain.security().type()).isEqualTo(WebhookSecurityType.NONE);
    assertThat(domain.security().config()).isEmpty();
  }
}
