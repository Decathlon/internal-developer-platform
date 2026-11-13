FROM mcr.microsoft.com/devcontainers/java:3-25-trixie@sha256:290d873937f3684458fc150a0ec968d6e54dc3d41c2374e3ea5a16c2689ce6d2

COPY certs/ /tmp/idp-certs/

# update-ca-certificates only imports "*.crt" files, so split every staged
# "*.crt"/"*.pem" file (including multi-certificate bundles) into one PEM
# certificate per "*.crt" file before running it.
RUN dir=/usr/local/share/ca-certificates/idp \
  && mkdir -p "$dir" \
  && i=0 \
  && for f in /tmp/idp-certs/*.crt /tmp/idp-certs/*.pem; do \
    [ -f "$f" ] || continue; \
    i=$((i + 1)); \
    awk -v prefix="$dir/idp-$i" ' \
      { sub(/\r$/, "") } \
      /-----BEGIN CERTIFICATE-----/ { n++; file = prefix "-" n ".crt" } \
      file { print > file } \
      /-----END CERTIFICATE-----/ { if (file) close(file); file = "" } \
    ' "$f"; \
  done \
  && rm -rf /tmp/idp-certs \
  && update-ca-certificates
