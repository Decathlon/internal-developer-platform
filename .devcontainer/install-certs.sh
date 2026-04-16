#!/bin/bash
# Runs inside the container during onCreateCommand.
# Installs host CA certificates (populated by init-certs.sh) into the
# container's system trust store. If no host certs were provided, this is a no-op.

CERT_SOURCE="/tmp/host-certs"

if [ -z "$(ls -A "$CERT_SOURCE" 2>/dev/null)" ]; then
    echo "No host certificates found in $CERT_SOURCE, skipping."
    exit 0
fi

echo "Installing host CA certificates into container trust store..."
sudo cp -rp "$CERT_SOURCE"/. /usr/local/share/ca-certificates/
sudo update-ca-certificates
