package com.decathlon.idp_core.domain.service.entity_template;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.decathlon.idp_core.domain.exception.entity_template.PropertyDefinitionRulesConflictException;

@DisplayName("PropertyRegexValidationService Tests")
class PropertyRegexValidationServiceTest {

	private PropertyRegexValidationService propertyRegexValidationService;

	@BeforeEach
	void setUp() {
		propertyRegexValidationService = new PropertyRegexValidationService();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"^[a-z0-9]+@[a-z0-9]+\\.[a-z]{2,}$", // email-like pattern
			"^(foo|bar)$",                          // safe alternation, not quantified
			"a{1,999}",                             // safe repetition bound
			"^[a-zA-Z0-9_-]+$",                    // alphanumeric slug
			"^\\d{4}-\\d{2}-\\d{2}$"              // ISO date
	})
	@DisplayName("Happy path: safe regex patterns are accepted")
	void testSafeRegexPatternsAccepted(String safePattern) {
		assertDoesNotThrow(() -> propertyRegexValidationService.validateRegexPattern("field", safePattern));
	}

	@Test
	@DisplayName("Error: Regex pattern exceeds maximum length (1000 chars)")
	void testRegexPatternTooLong() {
		String longPattern = "a".repeat(1001);
		String propertyName = "field";

		PropertyDefinitionRulesConflictException ex = assertThrows(
				PropertyDefinitionRulesConflictException.class,
				() -> propertyRegexValidationService.validateRegexPattern(propertyName, longPattern)
		);
		assertTrue(ex.getMessage().contains("too long"));
		assertTrue(ex.getMessage().contains("1000"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"(a+)+",           // nested quantifiers with +
			"(a*)*",           // nested quantifiers with *
			"(a+)*",           // mixed nested quantifiers
			"(a|b)+",          // quantified alternation with +
			"(foo|bar)*",      // quantified alternation with *
			"a{1,5000}"        // excessive repetition bound
	})
	@DisplayName("Error: Regex patterns with ReDoS vulnerabilities")
	void testRegexWithDangerousPatterns(String dangerousPattern) {
		String propertyName = "field";

		PropertyDefinitionRulesConflictException ex = assertThrows(
				PropertyDefinitionRulesConflictException.class,
				() -> propertyRegexValidationService.validateRegexPattern(propertyName, dangerousPattern)
		);
		assertTrue(ex.getMessage().contains("unsafe"),
				"Expected 'unsafe' in error message for pattern: " + dangerousPattern);
	}

	@Test
	@DisplayName("Error: Regex with invalid syntax")
	void testRegexWithInvalidSyntax() {
		PropertyDefinitionRulesConflictException ex = assertThrows(
				PropertyDefinitionRulesConflictException.class,
				() -> propertyRegexValidationService.validateRegexPattern("field", "[unclosed-bracket")
		);
		assertTrue(ex.getMessage().contains("Invalid regex"));
	}

}
