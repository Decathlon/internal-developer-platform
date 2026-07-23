#!/usr/bin/env bash
#
# Blocks a commit when any staged file contains a term listed in the Vale
# "reject" vocabulary. Matching is case-insensitive and substring based:
# each entry is treated as a literal fragment, not a whole word, so partial
# names are caught anywhere they appear.
#
set -euo pipefail

REJECT_FILE=".vale/styles/config/vocabularies/IDP/reject.txt"

# Nothing to enforce if the vocabulary file is missing.
[[ -f "$REJECT_FILE" ]] || exit 0

# Build a patterns file without blank lines. A blank line in a grep patterns
# file matches every input line, so blank lines must be stripped out.
patterns_file="$(mktemp)"
trap 'rm -f "$patterns_file"' EXIT
sed '/^[[:space:]]*$/d' "$REJECT_FILE" >"$patterns_file"

# No usable patterns -> nothing to check.
[[ -s "$patterns_file" ]] || exit 0

status=0
for file in "$@"; do
  [[ -f "$file" ]] || continue

  # -i: case-insensitive, -F: literal (substring), -I: skip binary files,
  # -n: show line numbers. grep exits 0 when at least one match is found.
  if matches="$(grep -i -n -F -I -f "$patterns_file" -- "$file")"; then
    echo "Rejected term found in ${file}:"
    printf '%s\n' "$matches" | sed 's/^/  /'
    status=1
  else
    # grep exits 1 for "no matches" and >1 for runtime errors.
    grep_status=$?
    if [[ "$grep_status" -gt 1 ]]; then
      echo "Error: failed to scan ${file} for rejected terms." >&2
      exit "$grep_status"
    fi
  fi
done

if [[ "$status" -ne 0 ]]; then
  echo
  echo "Commit blocked: remove the rejected terms listed above."
  echo "Rejected terms are defined in ${REJECT_FILE}."
fi

exit "$status"
