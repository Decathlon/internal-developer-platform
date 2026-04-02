#!/bin/bash
set -euo pipefail

# Define environment variables (passed by GitHub Actions)
# These default to the values defined in the workflow's 'env' block
API_PORT=${API_PORT:-8080}
SPEC_URL_PATH=${SPEC_URL_PATH:-/v3/api-docs/internal}
MAIN_SWAGGER="$PWD/docs/src/static/swagger.yaml"

# Save the current branch name (the PR branch)
PR_BRANCH=$(git rev-parse --abbrev-ref HEAD)
# Fetch and checkout the main branch to get the reference
git fetch origin main
git checkout origin/main
# 1. Check if the static swagger file exists on the 'main' branch
if [ -f "$MAIN_SWAGGER" ]; then
    # If it exists, copy it as the reference spec
    cp "$MAIN_SWAGGER" "specs/static-swagger-main.yaml"
else
    printf --  "Static swagger missing in main, extracting from main JAR\n"
    # Build the project on the 'main' branch to get the JAR
    mvn clean package -B

    JAR_PATH=$(find target -maxdepth 1 -name "*.jar" | head -n 1)
    if [ -z "$JAR_PATH" ]; then
        printf --  "::error:: No JAR found in target/ folder.\n"
        exit 1
    fi

    # Start the application in the background (&)
    java -jar "$JAR_PATH" --server.port="$API_PORT" &
    MAIN_PID=$!

    # --- START OF RELIABLE POLLING LOGIC ---
    MAX_ATTEMPTS=20  # Maximum number of attempts
    SLEEP_TIME=3     # Wait time (in seconds) between attempts
    ATTEMPT=0        # Initial attempt counter
    API_URL="http://localhost:$API_PORT$SPEC_URL_PATH"

    printf -- "Waiting for API endpoint (%s) to be reachable...\n" "$API_URL"

    while [ "$ATTEMPT" -lt "$MAX_ATTEMPTS" ]; do
        # Use curl with --fail and --silent to check for a successful connection (HTTP 2xx response)
        if curl --fail --silent -o /dev/null "$API_URL"; then
            printf -- "API is up after %d seconds.\n" "$((ATTEMPT * SLEEP_TIME))"
            break # Exit the loop because the API is ready
        fi
        printf -- "Attempt %d/%d failed. Waiting %d seconds...\n" "$((ATTEMPT + 1))" "$MAX_ATTEMPTS" "$SLEEP_TIME"
        sleep "$SLEEP_TIME"
        ATTEMPT=$((ATTEMPT + 1))
    done
    # Check if the maximum number of attempts was reached (timeout)
    if [ "$ATTEMPT" -eq "$MAX_ATTEMPTS" ]; then
        printf --  "::error:: Application failed to start and expose Swagger endpoint after %d attempts.\n" "$MAX_ATTEMPTS"
        printf -- "Swagger endpoint unreachable. Dumping logs:\n"
        pgrep -a java || printf -- "No Java process found\n"
        kill "$MAIN_PID"
        exit 1 # Fail the job
    fi
    # --- END OF RELIABLE POLLING LOGIC ---

    printf -- "Downloading specification from running application...\n"
    if ! curl -f "$API_URL" > "specs/static-swagger-main.yaml"; then
        printf -- "::error:: Failed to download OpenAPI specification after successful startup check.\n"
        kill "$MAIN_PID"
        exit 1
    fi

    kill "$MAIN_PID"
    printf --  "Reference swagger successfully generated and saved as static-swagger-main.yaml\n"
fi

# Return to the original PR branch
git checkout "$PR_BRANCH"
