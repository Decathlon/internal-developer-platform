package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

/// Supported HTTP message HMAC algorithms, pinned by server configuration.
public enum HttpMessageSignatureAlgorithm {
  HMAC_SHA256("hmac-sha256", "HmacSHA256", 32), HMAC_SHA1("hmac-sha1", "HmacSHA1", 20), HMAC_SHA384(
      "hmac-sha384", "HmacSHA384", 48), HMAC_SHA512("hmac-sha512", "HmacSHA512", 64);

  private final String wireName;
  private final String jcaName;
  private final int digestLength;

  HttpMessageSignatureAlgorithm(String wireName, String jcaName, int digestLength) {
    this.wireName = wireName;
    this.jcaName = jcaName;
    this.digestLength = digestLength;
  }

  /// Returns the required HTTP Signature algorithm attribute.
  public String wireName() {
    return wireName;
  }

  /// Returns the JCA MAC algorithm name.
  public String jcaName() {
    return jcaName;
  }

  /// Returns the expected digest length in bytes.
  public int digestLength() {
    return digestLength;
  }
}
