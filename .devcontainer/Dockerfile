FROM mcr.microsoft.com/devcontainers/java:3-25-trixie@sha256:290d873937f3684458fc150a0ec968d6e54dc3d41c2374e3ea5a16c2689ce6d2

COPY certs/ /usr/local/share/ca-certificates/idp/

RUN update-ca-certificates
