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
  void shouldDefaultToStarWhenNull() {
    SecurityRoleProperties props = new SecurityRoleProperties(null);
    assertEquals("*", props.baselineRole());
  }

  @Test
  void shouldDefaultToStarWhenEmpty() {
    SecurityRoleProperties props = new SecurityRoleProperties("");
    assertEquals("*", props.baselineRole());
  }

  @Test
  void shouldDefaultToStarWhenBlank() {
    SecurityRoleProperties props = new SecurityRoleProperties("   ");
    assertEquals("*", props.baselineRole());
  }
}
