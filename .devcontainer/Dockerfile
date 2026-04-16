FROM mcr.microsoft.com/devcontainers/java:dev-25

ARG MAVEN_VERSION=3.9.9
ARG MAVEN_BASE_URL=https://archive.apache.org/dist/maven/maven-3

# Use native TLS so uv respects the system CA bundle (required for corporate proxies)
ENV UV_NATIVE_TLS=1

# Copy corporate CA certificates from host for build-time HTTPS trust.
# These are populated by init-certs.sh (initializeCommand) on the host.
COPY local-certs/ /usr/local/share/ca-certificates/

# Install tools that were previously provided through Dev Container features:
# Docker-in-Docker dependencies, Docker CLI/Compose/Buildx, GitHub CLI,
# and Python tooling (uv + pre-commit).
RUN set -e; \
    \
    update-ca-certificates; \
    \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        apt-transport-https \
        ca-certificates \
        curl \
        dirmngr \
        git \
        gnupg2 \
        iptables \
        jq \
        lsb-release \
        pigz \
        python3-pip \
        python3-venv \
        sudo \
        wget; \
    \
    install -m 0755 -d /etc/apt/keyrings; \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg; \
    chmod a+r /etc/apt/keyrings/docker.gpg; \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian trixie stable" > /etc/apt/sources.list.d/docker.list; \
    \
    curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | gpg --dearmor -o /etc/apt/keyrings/githubcli-archive-keyring.gpg; \
    chmod a+r /etc/apt/keyrings/githubcli-archive-keyring.gpg; \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" > /etc/apt/sources.list.d/github-cli.list; \
    \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        containerd.io \
        docker-buildx-plugin \
        docker-ce \
        docker-ce-cli \
        docker-compose-plugin \
        gh; \
    mkdir -p /opt; \
    wget -q "${MAVEN_BASE_URL}/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -O /tmp/maven.tar.gz; \
    tar -xzf /tmp/maven.tar.gz -C /opt; \
    ln -sf "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn; \
    rm /tmp/maven.tar.gz; \
    mvn -version; \
    \
    python3 -m pip install --no-cache-dir --break-system-packages uv pre-commit; \
    uv python install 3.14; \
    usermod -aG docker vscode; \
    apt-mark hold docker-ce docker-ce-cli; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*
