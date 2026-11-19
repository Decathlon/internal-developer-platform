package com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature;

/// Verification outcomes translated to the HTTP message signature authentication error contract.
public enum HttpMessageSignatureResult {
  VALID(""), MISSING("The HTTP message signature is missing"), EXPIRED(
      "The HTTP message signature is expired"), INVALID(
          "The HTTP message signature is invalid"), MALFORMED(
              "Error parsing signature: malformed entry");

  private final String description;

  HttpMessageSignatureResult(String description) {
    this.description = description;
  }

  /// Returns the public rejection description without cryptographic details.
  public String description() {
    return description;
  }
}
