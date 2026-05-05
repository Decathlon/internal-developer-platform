package com.decathlon.idp_core.domain.service.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_BOOLEAN_NOT_ALLOWED;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_MAX_LENGTH_POSITIVE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_MIN_LENGTH_NON_NEGATIVE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_NUMERIC_RULE_NOT_ALLOWED;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.minMaxConstraintViolated;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ruleNotAllowed;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.rulesAreIncompatible;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.entity_template.PropertyDefinitionRulesConflictException;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

/// Domain service for validating property rule compatibility with property types.
///
/// **Business rules:**
/// - STRING: Allows format, enum_values, regex, max_length, min_length. Rejects numeric rules.
/// - NUMBER: Allows max_value, min_value. Rejects string and format rules.
/// - BOOLEAN: Rejects all rules; rules field must be null or empty.
///
@Service
public class PropertyDefinitionValidationService {

    // Static and thread-safe regex validator executor
    private static final ExecutorService VALIDATION_EXECUTOR = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            runnable -> {
                Thread thread = new Thread(runnable, "regex-validator-thread");
                thread.setDaemon(true);
                return thread;
            }
    );
    // Validation ReDoS probe string designed to trigger backtracking in vulnerable patterns
    private static final String STRESS_PROBE = "a".repeat(50) + "!";

    // Rule name constants
    public static final String REGEX = "regex";
    public static final String LENGTH = "length";
    public static final String VALUE = "value";
    public static final String FORMAT = "format";
    public static final String ENUM_VALUES = "enum_values";
    public static final String MAX_LENGTH = "max_length";
    public static final String MIN_LENGTH = "min_length";
    public static final String MAX_VALUE = "max_value";
    public static final String MIN_VALUE = "min_value";

    /// Validates property rules are compatible with the property's data type.
    ///
    /// **Contract:** Performs comprehensive validation including:
    /// - Rule type compatibility with property type
    /// - Numeric constraint ordering (min ≤ max)
    /// - Boolean properties reject all rules
    ///
    /// @param propertyDefinition the property definition containing type and rules
    /// @throws PropertyDefinitionRulesConflictException when rules violate business invariants
    public void validatePropertyDefinitionRules(PropertyDefinition propertyDefinition) {
        if (propertyDefinition.rules() == null) {
            return;
        }

        PropertyRules rules = propertyDefinition.rules();
        PropertyType type = propertyDefinition.type();

        switch (type) {
            case STRING:
                validateStringPropertyRules(propertyDefinition.name(), rules);
                break;
            case NUMBER:
                validateNumberPropertyRules(propertyDefinition.name(), rules);
                break;
            case BOOLEAN:
                validateBooleanPropertyRules(propertyDefinition.name(), rules);
                break;
            default:
                throw new IllegalArgumentException("Unknown property type: " + type);
        }
    }

    /// Validates rules for STRING property type.
    ///
    /// **Allowed rules:** format, enum_values, regex, max_length, min_length
    /// **Rejected rules:** max_value, min_value (numeric)
    /// **Conflicting rules:** format, regex, and enum_values are mutually exclusive;
    ///   enum_values is also mutually exclusive with max_length and min_length
    /// **Constraints:** 0 ≤ min_length ≤ max_length, regex must be valid
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyDefinitionRulesConflictException when rules defined violate any of the above constraints
    private void validateStringPropertyRules(String propertyName, PropertyRules rules) {
        validateStringIncompatibleRules(propertyName, rules);
        validateStringConstraints(propertyName, rules);

        // Validate regex pattern is valid
        if (rules.regex() != null && !rules.regex().isBlank()) {
            validateRegexPattern(propertyName, rules.regex());
        }
    }

    /// Validates numeric constraints for STRING property rules.
    ///
    /// **Constraints enforced:**
    /// - min_length must be non-negative (≥ 0)
    /// - max_length must be positive (> 0)
    /// - min_length must be less than or equal to max_length
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyDefinitionRulesConflictException when any constraint is violated
    private void validateStringConstraints(String propertyName, PropertyRules rules) {
        // Validate min_length is non-negative
        if (rules.minLength() != null && rules.minLength() < 0) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_MIN_LENGTH_NON_NEGATIVE
            );
        }
        // Validate max_length is not zero or negative
        if (rules.maxLength() != null && rules.maxLength() <= 0) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_MAX_LENGTH_POSITIVE
            );
        }
        // Validate min_length is below or equal to max_length
        if (rules.minLength() != null && rules.maxLength() != null && rules.minLength() > rules.maxLength()) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    minMaxConstraintViolated(LENGTH)
            );
        }
    }

    /// Validates rule compatibility and mutual exclusivity for STRING property rules.
    ///
    /// **Incompatibility rules enforced:**
    /// - Numeric rules (max_value, min_value) are not allowed for STRING type
    /// - format, regex, and enum_values are mutually exclusive
    /// - enum_values and length constraints (max_length, min_length) are mutually exclusive
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyDefinitionRulesConflictException when incompatible rules are both present
    private void validateStringIncompatibleRules(String propertyName, PropertyRules rules){
        // Reject numeric rules for STRING type
        if (rules.maxValue() != null || rules.minValue() != null) {
            String ruleName = rules.maxValue() != null ? MAX_VALUE : MIN_VALUE;
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_NUMERIC_RULE_NOT_ALLOWED.replace("{rule}", ruleName)
            );
        }

        // format, regex, and enum_values are incompatible with each other
        if (rules.format() != null && rules.enumValues() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(FORMAT, ENUM_VALUES)
            );
        }
        if (rules.format() != null && rules.regex() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(FORMAT, REGEX)
            );
        }
        if (rules.regex() != null && rules.enumValues() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(REGEX, ENUM_VALUES)
            );
        }

        // enum_values and length constraints are incompatible with each other
        if (rules.enumValues() != null && rules.maxLength() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(ENUM_VALUES, MAX_LENGTH)
            );
        }
        if (rules.enumValues() != null && rules.minLength() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(ENUM_VALUES, MIN_LENGTH)
            );
        }

    }

    /// Validates rules for NUMBER property type.
    ///
    /// **Allowed rules:** max_value, min_value
    /// **Rejected rules:** format, enum_values, regex, max_length, min_length (string)
    /// **Constraints:** min_value ≤ max_value
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyDefinitionRulesConflictException when string rules are present
    ///         or min/max value constraints are violated
    private void validateNumberPropertyRules(String propertyName, PropertyRules rules) {
        if (rules.format() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(FORMAT, PropertyType.NUMBER.name())
            );
        }

        if (rules.enumValues() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(ENUM_VALUES, PropertyType.NUMBER.name())
            );
        }

        if (rules.regex() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(REGEX, PropertyType.NUMBER.name())
            );
        }

        if (rules.minLength() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(MIN_LENGTH, PropertyType.NUMBER.name())
            );
        }

        if (rules.maxLength() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(MAX_LENGTH, PropertyType.NUMBER.name())
            );
        }

        if (rules.minValue() != null && rules.maxValue() != null && rules.minValue() > rules.maxValue()) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    minMaxConstraintViolated(VALUE)
            );
        }
    }

    /// Validates rules for BOOLEAN property type.
    ///
    /// **Allowed rules:** None
    /// **Rejected rules:** All rules must be null or empty
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyDefinitionRulesConflictException when any rule is set for BOOLEAN
    private void validateBooleanPropertyRules(String propertyName, PropertyRules rules) {
        if (rules.format() != null ||
                rules.enumValues() != null ||
                rules.regex() != null ||
                rules.maxLength() != null ||
                rules.minLength() != null ||
                rules.maxValue() != null ||
                rules.minValue() != null) {

            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.BOOLEAN,
                    PROPERTY_RULES_BOOLEAN_NOT_ALLOWED
            );
        }
    }

    /// Validates the user-provided regex pattern against ReDoS and injection risks.
    ///
    /// **Security checks:**
    /// 1. Rejects patterns exceeding 1,000 characters.
    /// 2. Rejects known dangerous regex patterns.
    /// 3. Ensures the pattern is valid Java regex.
    /// 4. Detects ReDoS by executing pattern matching within 100ms timeout.
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param regexPattern the regex pattern to validate
    /// @throws PropertyDefinitionRulesConflictException if any security check fails
    private void validateRegexPattern(String propertyName, String regexPattern) {
        if (regexPattern.length() > 1000) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName, PropertyType.STRING, "Regex pattern too long (max 1,000 characters)");
        }

        if (containsDangerousPatterns(regexPattern)) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName, PropertyType.STRING, "Regex pattern contains potentially unsafe constructs");
        }

        Pattern compiledRegexPattern;
        try {
            compiledRegexPattern = Pattern.compile(regexPattern);
        } catch (PatternSyntaxException e) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName, PropertyType.STRING, "Invalid regex pattern: " + e.getMessage());
        }

        validatePatternWithTimeout(propertyName, compiledRegexPattern);
    }

    /// Validates pattern matching with a timeout to detect ReDoS (Regular Expression Denial of Service) vulnerabilities.
    ///
    /// Executes a pattern match against a stress probe within a 100 ms timeout using a shared, bounded executor
    /// If the pattern takes longer than the timeout, it is rejected as potentially vulnerable to catastrophic backtracking.
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param pattern the compiled pattern to test
    /// @throws PropertyDefinitionRulesConflictException if the pattern times out or validation fails
    private void validatePatternWithTimeout(String propertyName, Pattern pattern) {
        Future<Boolean> future = VALIDATION_EXECUTOR.submit(() -> pattern.matcher(STRESS_PROBE).matches());
        try {
            future.get(10, TimeUnit.MILLISECONDS);
        } catch (TimeoutException _) {
            future.cancel(true);
            throw new PropertyDefinitionRulesConflictException(
                    propertyName, PropertyType.STRING, "Regex pattern rejected: execution time exceeded safety limits (ReDoS risk)");
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            throw new PropertyDefinitionRulesConflictException(
                    propertyName, PropertyType.STRING, "Regex pattern validation was interrupted");
        } catch (ExecutionException e) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName, PropertyType.STRING, "Regex validation failed: " + e.getCause().getMessage());
        }
    }

    /// Checks for known dangerous regex constructs using static string analysis.
    ///
    /// **Patterns detected:**
    /// - Nested quantifiers: `(a+)+`, `(a*)*`, `(a+)*`, etc.
    /// - Quantified alternation groups: `(a|b)+`, `(a|b)*`
    /// - Unbounded repetition upper bounds greater than 1,000 (e.g. `{5,9999}`)
    /// - Lookarounds with quantifiers: `(?=a+)`, `(?!a*)`
    ///
    /// **Implementation:** Uses static string analysis without regex matching
    /// to avoid ReDoS vulnerabilities in the validator itself.
    ///
    /// @param pattern the raw regex string to analyse
    /// @return `true` if potentially dangerous constructs are detected
    private boolean containsDangerousPatterns(String pattern) {
        return hasNestedQuantifiers(pattern) ||
               hasQuantifiedAlternation(pattern) ||
               hasLargeRepetitionBounds(pattern) ||
               hasLookaroundsWithQuantifiers(pattern);
    }

    /// Detects nested quantifiers like `(a+)+`, `(a*)*`, `(a+)*`, etc.
    /// Uses simple character-by-character analysis without regex.
    ///
    /// @param pattern the regex pattern string
    /// @return true if nested quantifiers are found
    private boolean hasNestedQuantifiers(String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '(' && matchesQuantifiedGroup(pattern, i, this::containsQuantifier)) {
                return true;
            }
        }
        return false;
    }

    /// Checks if a group starting at index i matches the quantified pattern criteria.
    /// The pattern must have a closing paren followed by a quantifier (+, *, ?, {),
    /// and the group content must match the provided test.
    ///
    /// @param pattern the regex pattern string
    /// @param groupStartIndex the index of the opening parenthesis
    /// @param test the test to apply to group content
    /// @return true if the group matches the criteria
    private boolean matchesQuantifiedGroup(String pattern, int groupStartIndex, Predicate<String> test) {
        int closeIdx = findMatchingCloseParenthesis(pattern, groupStartIndex);
        if (closeIdx == -1 || closeIdx + 1 >= pattern.length()) {
            return false;
        }

        char nextChar = pattern.charAt(closeIdx + 1);
        if (!isQuantifier(nextChar)) {
            return false;
        }

        String groupContent = pattern.substring(groupStartIndex + 1, closeIdx);
        return test.test(groupContent);
    }

    /// Detects quantified alternation groups like `(a|b)+` or `(a|b)*`.
    /// Uses simple character-by-character analysis without regex.
    ///
    /// @param pattern the regex pattern string
    /// @return true if quantified alternation is found
    private boolean hasQuantifiedAlternation(String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '(' && matchesQuantifiedGroup(pattern, i, groupContent -> groupContent.contains("|"))) {
                return true;
            }
        }
        return false;
    }

    /// Detects repetition bounds with excessively large upper limits like `{5,9999}`.
    /// Uses simple character-by-character analysis without regex.
    ///
    /// @param pattern the regex pattern string
    /// @return true if large repetition bounds are found
    private boolean hasLargeRepetitionBounds(String pattern) {
        int i = 0;
        while (i < pattern.length()) {
            if (pattern.charAt(i) == '{') {
                if (isLargeRepetitionBound(pattern, i)) {
                    return true;
                }
                int closeIdx = pattern.indexOf('}', i);
                i = closeIdx != -1 ? closeIdx + 1 : i + 1;
            } else {
                i++;
            }
        }
        return false;
    }

    /// Checks if the repetition bound starting at position i exceeds the safe limit.
    ///
    /// @param pattern the regex pattern string
    /// @param startIndex the index of the opening brace
    /// @return true if the upper bound is greater than 1000
    private boolean isLargeRepetitionBound(String pattern, int startIndex) {
        int closeIdx = pattern.indexOf('}', startIndex);
        if (closeIdx == -1) {
            return false;
        }

        String bounds = pattern.substring(startIndex + 1, closeIdx);
        return hasExcessiveUpperBound(bounds);
    }

    /// Parses a repetition bound string and checks if the upper limit exceeds 1000.
    ///
    /// @param bounds the bounds string (e.g., "5,9999" or "1,100")
    /// @return true if upper bound is greater than 1000
    private boolean hasExcessiveUpperBound(String bounds) {
        if (!bounds.contains(",")) {
            return false;
        }

        String[] parts = bounds.split(",");
        if (parts.length != 2 || parts[1].trim().isEmpty()) {
            return false;
        }

        try {
            int upper = Integer.parseInt(parts[1].trim());
            return upper > 1000;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    /// Detects lookarounds with quantifiers like `(?=a+)`, `(?!a*)`, etc.
    /// These can amplify backtracking behavior and pose ReDoS risks.
    /// Uses simple character-by-character analysis without regex.
    ///
    /// @param pattern the regex pattern string
    /// @return true if lookarounds with quantifiers are found
    private boolean hasLookaroundsWithQuantifiers(String pattern) {
        for (int i = 0; i < pattern.length() - 3; i++) {
            if (isLookaroundAt(pattern, i)) {
                int closeIdx = findMatchingCloseParenthesis(pattern, i);
                if (closeIdx != -1) {
                    String lookaroundContent = pattern.substring(i, closeIdx + 1);
                    if (containsQuantifier(lookaroundContent)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// Checks if position i in pattern is the start of a lookaround construct.
    /// Lookarounds are: `(?=...)`, `(?!...)`, `(?<=...)`, `(?<!...)`
    ///
    /// @param pattern the regex pattern string
    /// @param i the position to check
    /// @return true if a lookaround starts at position i
    private boolean isLookaroundAt(String pattern, int i) {
        if (pattern.charAt(i) != '(' || pattern.charAt(i + 1) != '?') {
            return false;
        }

        // Lookahead: (?= or (?!
        if (isLookaroundType(pattern, i + 2, '=', '!')) {
            return true;
        }

        // Lookbehind: (?<= or (?<!
        return i + 3 < pattern.length() && pattern.charAt(i + 2) == '<' &&
               isLookaroundType(pattern, i + 3, '=', '!');
    }

    /// Checks if the character at position i is one of the expected lookaround markers.
    ///
    /// @param pattern the regex pattern string
    /// @param i the position to check
    /// @param marker1 first expected marker
    /// @param marker2 second expected marker
    /// @return true if the character matches either marker
    private boolean isLookaroundType(String pattern, int i, char marker1, char marker2) {
        return pattern.charAt(i) == marker1 || pattern.charAt(i) == marker2;
    }

    /// Helper: Checks if a character is a regex quantifier (+, *, ?, {).
    ///
    /// @param c the character to check
    /// @return true if the character is a quantifier
    private boolean isQuantifier(char c) {
        return c == '+' || c == '*' || c == '?' || c == '{';
    }

    /// Helper: Checks if a string contains regex quantifiers (+, *, ?, {n,m}).
    ///
    /// @param str the string to check
    /// @return true if any quantifier is found
    private boolean containsQuantifier(String str) {
        for (char c : str.toCharArray()) {
            if (isQuantifier(c)) {
                return true;
            }
        }
        return false;
    }

    /// Helper: Finds the matching closing parenthesis for an opening paren at index startIdx.
    /// Handles nested parentheses correctly. Returns -1 if no matching close paren exists.
    ///
    /// @param pattern the pattern string
    /// @param startIndex the index of the opening parenthesis
    /// @return the index of the matching closing paren, or -1 if not found
    private int findMatchingCloseParenthesis(String pattern, int startIndex) {
        int depth = 0;
        for (int i = startIndex; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '(' && (i == 0 || pattern.charAt(i - 1) != '\\')) {
                depth++;
            } else if (pattern.charAt(i) == ')' && (i == 0 || pattern.charAt(i - 1) != '\\')) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }


}
