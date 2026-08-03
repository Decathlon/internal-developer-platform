package com.decathlon.idp_core.domain.model.enums;

/// Discriminator for the security validation strategy of a [WebhookConnector].
///
/// | Strategy     | headerName | secretAlias        | prefix   | username | jwksUri |
/// |--------------|------------|--------------------|----------|----------|---------|
/// | HMAC_SHA256  | Required   | Required (hash key)| Optional | —        | —       |
/// | JWT_BEARER   | —          | —                  | —        | —        | Required|
/// | STATIC_TOKEN | Required   | Required (target)  | —        | —        | —       |
/// | BASIC_AUTH   | —          | Required (password)| —        | Required | —       |
/// | NONE         | —          | —                  | —        | —        | —       |
///
/// `NONE` means the connector intentionally accepts unauthenticated requests.
public enum WebhookSecurityType {
  HMAC_SHA256, JWT_BEARER, STATIC_TOKEN, BASIC_AUTH, NONE
}
