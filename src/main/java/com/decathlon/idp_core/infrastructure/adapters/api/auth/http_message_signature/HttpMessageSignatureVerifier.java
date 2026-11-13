package com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureProperties;

/// Verifies HTTP message signatures without consuming request content or changing authentication.
public final class HttpMessageSignatureVerifier {

  private final HttpMessageSignatureProperties properties;
  private final Clock clock;
  private final HttpMessageSignatureParser parser = new HttpMessageSignatureParser();

  /// Creates a verifier with restart-bound policy and an injectable time source.
  public HttpMessageSignatureVerifier(HttpMessageSignatureProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  /// Verifies an unambiguous signature header against the client-generated ID.
  public HttpMessageSignatureResult verify(String header, String requestId) {
    var parsed = parser.parse(header);
    if (parsed.isEmpty()) {
      return HttpMessageSignatureResult.MALFORMED;
    }
    var signature = parsed.orElseThrow();
    String idHeader = properties.identifierHeaderName().toLowerCase(Locale.ROOT);
    if (!properties.algorithm().wireName().equals(signature.algorithm())
        || signature.components().size() != 3
        || !Set.copyOf(signature.components()).equals(Set.of(idHeader, "(created)", "(expires)"))
        || requestId == null || requestId.isBlank() || requestId.contains("\r")
        || requestId.contains("\n")) {
      return HttpMessageSignatureResult.INVALID;
    }
    byte[] supplied;
    try {
      supplied = Base64.getDecoder().decode(signature.signature());
    } catch (IllegalArgumentException _) {
      return HttpMessageSignatureResult.MALFORMED;
    }
    if (supplied.length != properties.algorithm().digestLength()) {
      return HttpMessageSignatureResult.INVALID;
    }
    String canonical = signature.components().stream().map(component -> switch (component) {
      case "(created)" -> "(created): " + signature.createdText();
      case "(expires)" -> "(expires): " + signature.expiresText();
      default -> idHeader + ": " + requestId;
    }).collect(Collectors.joining("\n"));
    if (!MessageDigest.isEqual(compute(canonical), supplied)) {
      return HttpMessageSignatureResult.INVALID;
    }
    var lifetime = signature.expires().subtract(signature.created());
    if (lifetime.signum() <= 0
        || lifetime.compareTo(BigDecimal.valueOf(properties.validityDurationSeconds())) > 0) {
      return HttpMessageSignatureResult.INVALID;
    }
    var now = clock.instant();
    var nowSeconds = BigDecimal.valueOf(now.getEpochSecond())
        .add(BigDecimal.valueOf(now.getNano(), 9));
    var skew = BigDecimal.valueOf(properties.clockSkewSeconds());
    if (signature.created().compareTo(nowSeconds.add(skew)) > 0) {
      return HttpMessageSignatureResult.INVALID;
    }
    if (signature.expires().compareTo(nowSeconds.subtract(skew)) <= 0) {
      return HttpMessageSignatureResult.EXPIRED;
    }
    return HttpMessageSignatureResult.VALID;
  }

  private byte[] compute(String canonical) {
    try {
      var mac = Mac.getInstance(properties.algorithm().jcaName());
      mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8),
          properties.algorithm().jcaName()));
      return mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("HTTP message signature cryptography is unavailable",
          exception);
    }
  }
}
