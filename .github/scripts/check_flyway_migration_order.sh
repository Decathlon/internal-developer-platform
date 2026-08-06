#!/bin/bash
# Check that Flyway migrations added in a pull request keep a strictly increasing
# version order compared to the base branch (usually 'main').
#
# A migration inserted "in the middle" of the already merged ones (for example adding
# V5_3 when main already contains V6_1) is a breaking change: environments where V6_1
# has already been applied would either skip the new script or fail Flyway validation.
#
# The script also detects modified or deleted migrations already present on the base
# branch, since their checksum is stored in the Flyway schema history table.
#
# Usage: check_flyway_migration_order.sh [migration_dir] [base_ref]

set -euo pipefail

MIGRATION_DIR="${1:-${MIGRATION_DIR:-src/main/resources/db/migration}}"
BASE_REF="${2:-${BASE_REF:-origin/main}}"
REPORT_FILE="${REPORT_FILE:-flyway-migration-order-report.md}"
NAME_PATTERN='^V[0-9]+(_[0-9]+)*__[A-Za-z0-9_]+\.sql$'

EXIT_CODE=0
ERRORS=()

# Convert a migration file name into a sortable numeric key (V5_2 -> 5.000002)
version_key() {
    local version
    version=$(basename "$1" | sed -E 's/^V([0-9_]+)__.*/\1/')
    awk -F'_' '{
        key = $1
        for (i = 2; i <= NF; i++) {
            key = key + $i / (10 ^ (6 * (i - 1)))
        }
        printf "%.12f", key
    }' <<< "$version"
}

version_label() {
    basename "$1" | sed -E 's/^V([0-9_]+)__.*/\1/' | tr '_' '.'
}

printf -- "Checking Flyway migration order in '%s' against '%s'\n" "$MIGRATION_DIR" "$BASE_REF"

if ! git rev-parse --verify --quiet "$BASE_REF" > /dev/null; then
    printf -- "::error:: Base ref '%s' is not available. Fetch it before running this check.\n" "$BASE_REF"
    exit 1
fi

# Migrations already merged on the base branch
BASE_FILES=$(git ls-tree -r --name-only "$BASE_REF" -- "$MIGRATION_DIR" | grep -E '\.sql$' | sort || true)
# Migrations present in the pull request working tree
PR_FILES=$(git ls-tree -r --name-only HEAD -- "$MIGRATION_DIR" | grep -E '\.sql$' | sort || true)

ADDED_FILES=$(comm -13 <(printf -- '%s\n' "$BASE_FILES") <(printf -- '%s\n' "$PR_FILES") | grep -E '\.sql$' || true)
DELETED_FILES=$(comm -23 <(printf -- '%s\n' "$BASE_FILES") <(printf -- '%s\n' "$PR_FILES") | grep -E '\.sql$' || true)
MODIFIED_FILES=$(git diff --name-only --diff-filter=M "$BASE_REF" HEAD -- "$MIGRATION_DIR" | grep -E '\.sql$' || true)

# Highest version already merged on the base branch
BASE_MAX_FILE=""
BASE_MAX_KEY="0"
while IFS= read -r file; do
    [ -z "$file" ] && continue
    if [[ ! $(basename "$file") =~ $NAME_PATTERN ]]; then
        continue
    fi
    key=$(version_key "$file")
    if awk -v a="$key" -v b="$BASE_MAX_KEY" 'BEGIN { exit !(a > b) }'; then
        BASE_MAX_KEY="$key"
        BASE_MAX_FILE="$file"
    fi
done <<< "$BASE_FILES"

if [ -n "$BASE_MAX_FILE" ]; then
    printf -- "Highest migration on %s: %s (version %s)\n" "$BASE_REF" "$(basename "$BASE_MAX_FILE")" "$(version_label "$BASE_MAX_FILE")"
else
    printf -- "No migration found on %s\n" "$BASE_REF"
fi

if [ -z "$ADDED_FILES" ] && [ -z "$DELETED_FILES" ] && [ -z "$MODIFIED_FILES" ]; then
    printf -- "No migration change detected in this pull request.\n"
    : > "$REPORT_FILE"
    exit 0
fi

# Rule 1: an already merged migration must never be modified or deleted
while IFS= read -r file; do
    [ -z "$file" ] && continue
    ERRORS+=("\`$file\` is modified. A migration already merged on \`$BASE_REF\` has been applied on running environments: its checksum is frozen. Create a new migration instead.")
    EXIT_CODE=1
done <<< "$MODIFIED_FILES"

while IFS= read -r file; do
    [ -z "$file" ] && continue
    ERRORS+=("\`$file\` is deleted or renamed. A migration already merged on \`$BASE_REF\` must be kept as is. Create a new migration instead.")
    EXIT_CODE=1
done <<< "$DELETED_FILES"

# Rule 2: added migrations must be well named and strictly above the highest merged version
SEEN_KEYS=()
while IFS= read -r file; do
    [ -z "$file" ] && continue
    name=$(basename "$file")

    if [[ ! $name =~ $NAME_PATTERN ]]; then
        ERRORS+=("\`$file\` does not follow the Flyway naming convention \`V<major>_<minor>__<description>.sql\`.")
        EXIT_CODE=1
        continue
    fi

    key=$(version_key "$file")
    label=$(version_label "$file")

    if [ -n "$BASE_MAX_FILE" ] && awk -v a="$key" -v b="$BASE_MAX_KEY" 'BEGIN { exit !(a <= b) }'; then
        ERRORS+=("\`$name\` (version \`$label\`) is lower than or equal to \`$(basename "$BASE_MAX_FILE")\` (version \`$(version_label "$BASE_MAX_FILE")\`) already merged on \`$BASE_REF\`. Renumber it above \`$(version_label "$BASE_MAX_FILE")\` so Flyway applies it on every environment.")
        EXIT_CODE=1
        continue
    fi

    for seen in ${SEEN_KEYS[@]+"${SEEN_KEYS[@]}"}; do
        if [ "${seen%%:*}" = "$key" ]; then
            ERRORS+=("\`$name\` uses version \`$label\` already used by \`${seen#*:}\` in this pull request. Flyway versions must be unique.")
            EXIT_CODE=1
        fi
    done
    SEEN_KEYS+=("$key:$name")

    printf -- "Added migration %s (version %s)\n" "$name" "$label"
done <<< "$ADDED_FILES"

# Build the markdown report
{
    if [ "$EXIT_CODE" -eq 0 ]; then
        printf -- "No Flyway migration ordering issue detected.\n\n"
        printf -- "| Added migration | Version |\n|---|---|\n"
        while IFS= read -r file; do
            [ -z "$file" ] && continue
            printf -- "| \`%s\` | \`%s\` |\n" "$(basename "$file")" "$(version_label "$file")"
        done <<< "$ADDED_FILES"
    else
        printf -- "The following Flyway migration issues must be fixed:\n\n"
        for error in "${ERRORS[@]}"; do
            printf -- "- %s\n" "$error"
        done
        printf -- "\nHighest migration currently on \`%s\`: " "$BASE_REF"
        if [ -n "$BASE_MAX_FILE" ]; then
            printf -- "\`%s\`\n" "$(basename "$BASE_MAX_FILE")"
        else
            printf -- "none\n"
        fi
    fi
} > "$REPORT_FILE"

cat "$REPORT_FILE"

if [ "$EXIT_CODE" -ne 0 ]; then
    for error in "${ERRORS[@]}"; do
        printf -- "::error:: %s\n" "$(printf -- '%s' "$error" | tr -d '`')"
    done
fi

exit "$EXIT_CODE"
