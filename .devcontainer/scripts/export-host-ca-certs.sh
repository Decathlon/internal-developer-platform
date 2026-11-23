#!/usr/bin/env bash
# Stages any corporate/proxy CA certificates trusted by the *host* machine into
# .devcontainer/certs/ so the container can trust the same TLS-inspecting
# proxy (Zscaler, Netskope, custom internal CA, ...) during the image build
# (docker-in-docker's github.com fetch, apt, etc.).
#
# Runs on the host via devcontainer.json's "initializeCommand", before every
# build. Never commits anything: .devcontainer/certs/* is gitignored, and this
# script wipes/repopulates it fresh on each run so it always reflects the
# current host, whoever/whatever machine that is.
set -uo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
certs_dir="$(cd "$script_dir/.." && pwd)/certs"

mkdir -p "$certs_dir"
find "$certs_dir" -maxdepth 1 -type f ! -name '.gitkeep' -delete 2>/dev/null

copy_source() {
  local src="$1"
  if [ -f "$src" ]; then
    cp "$src" "$certs_dir/" 2>/dev/null
  elif [ -d "$src" ]; then
    find "$src" -maxdepth 2 -type f \( -name '*.crt' -o -name '*.pem' \) 2>/dev/null |
      while IFS= read -r f; do cp "$f" "$certs_dir/" 2>/dev/null; done
  fi
}

# Explicit opt-in override: point at your own cert/bundle for anything the
# auto-detection below doesn't recognize.
if [ -n "${DEVCONTAINER_EXTRA_CA_CERTS:-}" ]; then
  copy_source "$DEVCONTAINER_EXTRA_CA_CERTS"
fi

# Best-effort auto-detection per host OS. Never fatal: an empty certs/ just
# means the build proceeds without extra trust, same as today.
case "$(uname -s 2>/dev/null)" in
Darwin)
  # Dump the System keychain; duplicates of certs already in the image are harmless.
  if command -v security >/dev/null 2>&1; then
    security find-certificate -a -p /Library/Keychains/System.keychain 2>/dev/null |
      awk -v dir="$certs_dir" '
        /-----BEGIN CERTIFICATE-----/ { n++; file = dir "/host-ca-" n ".pem" }
        file { print > file }
      '
  fi
  ;;
Linux | MINGW* | MSYS* | CYGWIN*)
  # Debian/Ubuntu and RHEL/Fedora both keep locally-added (i.e. corporate
  # proxy installer) trust anchors separate from the default OS bundle here.
  copy_source /usr/local/share/ca-certificates
  copy_source /etc/pki/ca-trust/source/anchors
  ;;
esac

count=$(find "$certs_dir" -maxdepth 1 -type f ! -name '.gitkeep' 2>/dev/null | wc -l | tr -d ' ')
echo "[devcontainer] host CA export: ${count} certificate(s) staged in .devcontainer/certs (gitignored, build-context only)"
exit 0
