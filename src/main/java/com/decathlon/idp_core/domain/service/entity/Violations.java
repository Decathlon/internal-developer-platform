package com.decathlon.idp_core.domain.service.entity;
import java.util.ArrayList;
import java.util.List;

/// Mutable accumulator of validation violation messages.
///
/// Centralises message formatting and indexed-prefix handling so domain
/// validators stay focused on the rule they enforce rather than on string
/// concatenation. Not thread-safe; intended for short-lived per-request use.
final class Violations {
    private final List<String> messages = new ArrayList<>();
    void add(String message) {
        messages.add(message);
    }
    void add(String template, Object... args) {
        messages.add(template.formatted(args));
    }
    void addIfBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            messages.add(message);
        }
    }

    /// Adds a violation prefixed with the indexed collection name, e.g.
    /// `Property[2]: Property name is mandatory`.
    void addIndexed(String collection, int index, String message) {
        messages.add("%s[%d]: %s".formatted(collection, index, message));
    }
    boolean isEmpty() {
        return messages.isEmpty();
    }
    List<String> asList() {
        return List.copyOf(messages);
    }
}
