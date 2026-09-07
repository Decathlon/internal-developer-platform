package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.decoder;

import java.io.IOException;

/// Functional interface defining a payload decoder contract for specific content encodings.
@FunctionalInterface
public interface PayloadDecoder {

  /// Decodes a raw payload byte array.
  ///
  /// @param payload the raw, possibly compressed/encoded payload bytes
  /// @return the decoded payload as a byte array
  /// @throws IOException if decoding or decompression fails
  byte[] decode(byte[] payload) throws IOException;
}
