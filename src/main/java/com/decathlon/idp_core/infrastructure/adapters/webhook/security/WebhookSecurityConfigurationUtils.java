package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Common utilities for webhook security validation.
 *
 * Provides shared methods for extracting and validating configuration keys across all security strategies.
 * This eliminates duplication between creation-time and runtime validation logic.
 */
public final class WebhookSecurityConfigurationUtils {

    private WebhookSecurityConfigurationUtils() {
        // Utility class
    }

    /**
     * Retrieves a required configuration value, checking multiple key variants (snake_case and camelCase).
     *
     * @param config the configuration map
     * @param keys the keys to check in order (e.g., "secret_alias", "secretAlias")
     * @return the first non-blank value found
     * @throws WebhookSecurityConfigurationException if no value is found (at creation time)
     * @throws WebhookAuthenticationException if no value is found (at runtime)
     */
    public static String required(Map<String, String> config, String... keys) {
        return required(config, false, keys);
    }

    /**
     * Retrieves a required configuration value at runtime (throws WebhookAuthenticationException).
     *
     * @param config the configuration map
     * @param keys the keys to check in order
     * @return the first non-blank value found
     * @throws WebhookAuthenticationException if no value is found
     */
    public static String requiredAtRuntime(Map<String, String> config, String... keys) {
        return required(config, true, keys);
    }

    /**
     * Retrieves an optional configuration value, returning a default if not found.
     *
     * @param config the configuration map
     * @param key the key to look up
     * @param defaultValue the value to return if key is not found
     * @return the configuration value or the default
     */
    public static String optional(Map<String, String> config, String key, String defaultValue) {
        String value = config.get(key);
        return value == null ? defaultValue : value;
    }

    /**
     * Validates that a secret alias follows the UPPER_SNAKE_CASE convention.
     *
     * @param alias the alias to validate
     * @throws WebhookSecurityConfigurationException if the alias format is invalid (at creation time)
     */
    public static void validateSecretAliasFormat(String alias) {
        if (!alias.matches("^[A-Z0-9_]+$")) {
            throw new WebhookSecurityConfigurationException(
                    "Invalid 'secret_alias'. Use an environment variable alias (UPPER_SNAKE_CASE), not the raw secret value"
            );
        }
    }

    /**
     * Retrieves a secret from environment variables, throwing if not found or empty.
     *
     * @param alias the environment variable alias (key)
     * @return the secret value
     * @throws WebhookAuthenticationException if the secret is not found or empty
     */
    public static String getSecretFromEnvironment(String alias) {
        String secret = System.getenv(alias);
        if (secret == null || secret.isBlank()) {
            throw new WebhookAuthenticationException("Missing environment secret for alias: " + alias);
        }
        return secret;
    }

    private static String required(Map<String, String> config, boolean isRuntime, String... keys) {
        for (String key : keys) {
            String value = config.get(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }

        String keysStr = String.join(", ", keys);
        if (isRuntime) {
            throw new WebhookAuthenticationException("Missing security config key. Expected one of: " + keysStr);
        } else {
            throw new WebhookSecurityConfigurationException("Missing required security config key. Expected one of: " + keysStr);
        }
    }
}
