#!/bin/bash
# Runs on the HOST before container creation (initializeCommand).
# On Linux hosts with corporate SSL inspection, copies host CA certificates
# into .devcontainer/local-certs/ so they can be bind-mounted into the container.
# On macOS/Windows or hosts without those paths, this is a no-op.

CERT_DIR="$(cd "$(dirname "$0")" && pwd)/local-certs"
mkdir -p "$CERT_DIR"

if [ -f "/etc/ssl/certs/ca-certificates.crt" ]; then
    cp "/etc/ssl/certs/ca-certificates.crt" "$CERT_DIR/"
fi

if [ -d "/usr/local/share/ca-certificates" ]; then
    find /usr/local/share/ca-certificates -maxdepth 2 -name "*.crt" \
        -exec cp {} "$CERT_DIR/" \; 2>/dev/null || true
fi
