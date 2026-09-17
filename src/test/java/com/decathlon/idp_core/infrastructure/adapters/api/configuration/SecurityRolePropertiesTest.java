package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// Unit tests for SecurityRoleProperties verifying correct handling of baseline role configuration.
/// when a real role managing should be put in place, this test must fail
class SecurityRolePropertiesTest {

  @Test
  void shouldKeepValidRole() {
    SecurityRoleProperties props = new SecurityRoleProperties("ROLE_EDITOR");
    assertEquals("ROLE_EDITOR", props.baselineRole());
  }

  @Test
  void shouldDefaultToReaderWhenNull() {
    SecurityRoleProperties props = new SecurityRoleProperties(null);
    assertEquals("ROLE_READER", props.baselineRole());
  }

  @Test
  void shouldDefaultToReaderWhenEmpty() {
    SecurityRoleProperties props = new SecurityRoleProperties("");
    assertEquals("ROLE_READER", props.baselineRole());
  }

  @Test
  void shouldDefaultToReaderWhenBlank() {
    SecurityRoleProperties props = new SecurityRoleProperties("   ");
    assertEquals("ROLE_READER", props.baselineRole());
  }
}
