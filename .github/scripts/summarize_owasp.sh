#!/usr/bin/env bash
set -euo pipefail

SUMMARY_FILE="${GITHUB_STEP_SUMMARY:-/dev/stdout}"

echo "### OWASP Dependency Check Summary" >> "$SUMMARY_FILE"

SARIF_REPORT=""
if [ -f "$PWD/target/security-reports/dependency-check-report.sarif" ]; then
  SARIF_REPORT="$PWD/target/security-reports/dependency-check-report.sarif"
elif [ -f "$PWD/target/dependency-check-report.sarif" ]; then
  SARIF_REPORT="$PWD/target/dependency-check-report.sarif"
fi

if [ -n "$SARIF_REPORT" ]; then
  echo "| Vulnerability | CVSS | Package |" >> "$SUMMARY_FILE"
  echo "|:--|:--:|:--|" >> "$SUMMARY_FILE"

  if command -v jq >/dev/null 2>&1; then
    VULN_COUNT=$(jq '.runs[].results | length' "$SARIF_REPORT" | awk '{s+=$1} END {print s}')
    if [ "$VULN_COUNT" -gt 0 ]; then
      jq -r '.runs[].results[] | [.ruleId, (.properties.cvssScore // "N/A"), (.message.text | split("\n")[0])] | @tsv' "$SARIF_REPORT" |
      while IFS=$'\t' read -r ruleId cvss message; do
        printf "| %s | %s | %s |\n" "$ruleId" "$cvss" "$message" >> "$SUMMARY_FILE"
      done
      echo "| **Total** | **$VULN_COUNT** |  |" >> "$SUMMARY_FILE"
      echo "::error:: OWASP Dependency Check found $VULN_COUNT vulnerabilities"
      exit 1
    else
      echo "| OK | 0 | No vulnerabilities found |" >> "$SUMMARY_FILE"
    fi
  else
    echo "::warning:: jq not found — unable to parse SARIF details"
  fi
else
  echo "::warning:: OWASP report missing"
fi
