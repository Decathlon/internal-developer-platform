package com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/// Parses the restricted legacy HTTP Signature profile, without accepting ambiguous entries.
public final class HttpMessageSignatureParser {

  private static final int MAX_HEADER_LENGTH = 8192;
  private static final String ALGORITHM = "algorithm";
  private static final String HEADERS = "headers";
  private static final String CREATED = "created";
  private static final String EXPIRES = "expires";
  private static final String SIGNATURE = "signature";
  private static final Pattern ATTRIBUTE = Pattern.compile(
      "[ \\t]*([A-Za-z][A-Za-z\\d]*)[ \\t]*=[ \\t]*(?:\"([^\"\\\\\\r\\n]*)\"|([^\\s,\"\\\\]+))[ \\t]*(,|$)");
  private static final Set<String> REQUIRED = Set.of(ALGORITHM, HEADERS, CREATED, EXPIRES,
      SIGNATURE);
  private static final Set<String> ALLOWED = Set.of("keyid", ALGORITHM, HEADERS, CREATED, EXPIRES,
      SIGNATURE);
  private static final BigDecimal MAX_TIMESTAMP = BigDecimal.valueOf(Instant.MAX.getEpochSecond());

  /// Returns parsed attributes, or empty for malformed or ambiguous input.
  public Optional<ParsedSignature> parse(String header) {
    if (header == null || header.isBlank() || header.length() > MAX_HEADER_LENGTH) {
      return Optional.empty();
    }
    Map<String, String> attributes = new HashMap<>();
    var matcher = ATTRIBUTE.matcher(header);
    int position = 0;
    while (position < header.length()) {
      matcher.region(position, header.length());
      if (!matcher.lookingAt()) {
        return Optional.empty();
      }
      String name = matcher.group(1).toLowerCase(Locale.ROOT);
      String value = matcher.group(matcher.group(2) != null ? 2 : 3);
      if (!ALLOWED.contains(name) || value.isBlank()
          || attributes.putIfAbsent(name, value) != null) {
        return Optional.empty();
      }
      position = matcher.end();
      if (position == header.length() && ",".equals(matcher.group(4))) {
        return Optional.empty();
      }
    }
    return parseSignature(attributes);
  }

  private Optional<ParsedSignature> parseSignature(Map<String, String> attributes) {
    if (!attributes.keySet().containsAll(REQUIRED) || !attributes.get(CREATED).matches("\\d+")
        || !attributes.get(EXPIRES).matches("\\d+(?:\\.\\d{1,9})?")) {
      return Optional.empty();
    }
    var created = new BigDecimal(attributes.get(CREATED));
    var expires = new BigDecimal(attributes.get(EXPIRES));
    if (created.compareTo(MAX_TIMESTAMP) > 0 || expires.compareTo(MAX_TIMESTAMP) > 0) {
      return Optional.empty();
    }
    List<String> components = List
        .of(attributes.get(HEADERS).toLowerCase(Locale.ROOT).split(" ", -1));
    return Optional
        .of(new ParsedSignature(attributes.get(ALGORITHM), components, attributes.get(CREATED),
            attributes.get(EXPIRES), created, expires, attributes.get(SIGNATURE)));
  }

  /// Parsed values retain timestamp text for byte-exact canonicalization.
  public record ParsedSignature(String algorithm, List<String> components, String createdText,
      String expiresText, BigDecimal created, BigDecimal expires, String signature) {
    /// Copies signed component names so canonicalization cannot change after
    /// parsing.
    public ParsedSignature {
      components = List.copyOf(components);
    }

    @Override
    public String toString() {
      return "ParsedSignature[REDACTED]";
    }
  }
}
