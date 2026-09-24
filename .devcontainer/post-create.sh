#!/usr/bin/env bash
set -euo pipefail

sudo apt-get update
sudo apt-get install --yes --no-install-recommends postgresql-client
sudo rm -rf /var/lib/apt/lists/*

mkdir -p /home/vscode/.m2
sudo chown -R vscode:vscode /home/vscode/.m2

uv sync --frozen --directory docs
uv tool install pre-commit
pre-commit install

# Remove any stale target/ (e.g. from a prebuild on an older commit) so
# Maven's timestamp-based staleness check can't skip recompiling sources
# that were added/changed since it was generated.
./mvnw -B -ntp clean
./mvnw -B -ntp dependency:go-offline
