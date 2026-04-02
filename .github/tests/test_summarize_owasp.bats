#!/usr/bin/env bats

SCRIPT_PATH=".github/scripts/summarize_owasp.sh"
load test_helper/bats-support/load
load test_helper/bats-assert/load

setup() {
  mkdir -p target/security-reports
  rm -f target/security-reports/dependency-check-report.sarif
    # Sauvegarde du summary d'origine
    OLD_SUMMARY_FILE="${GITHUB_STEP_SUMMARY:-}"
    export OLD_SUMMARY_FILE

    # Rediriger vers un summary temporaire pour les tests
    export GITHUB_STEP_SUMMARY="target/summary_for_test.txt"
    rm -f "$GITHUB_STEP_SUMMARY"
}

teardown() {
  rm -rf target

  # Restaurer le summary d'origine s'il existait
  if [ -n "${OLD_SUMMARY_FILE:-}" ]; then
    export GITHUB_STEP_SUMMARY="$OLD_SUMMARY_FILE"
  else
    unset GITHUB_STEP_SUMMARY
  fi
}

@test "fails and exits 1 when vulnerabilities are present" {
  cat > target/security-reports/dependency-check-report.sarif <<'EOF'
{
  "runs": [{
    "results": [
      {
        "ruleId": "CVE-1234",
        "message": { "text": "Vulnerable library found" },
        "properties": { "cvssScore": "7.5" }
      }
    ]
  }]
}
EOF

  run "$SCRIPT_PATH"

  assert_failure

  assert_output "::error:: OWASP Dependency Check found 1 vulnerabilities"
}

@test "succeeds and exits 0 when no vulnerabilities found" {
  # Rapport SARIF vide
  cat > target/security-reports/dependency-check-report.sarif <<'EOF'
{
  "runs": [{
    "results": []
  }]
}
EOF

  run "$SCRIPT_PATH"

  assert_success

  assert_output --regexp '^$'
}

@test "succeeds and warns when no SARIF report is present" {
  # Suppression du rapport SARIF
  rm -f target/security-reports/dependency-check-report.sarif

  run "$SCRIPT_PATH"

  assert_success

  assert_output "::warning:: OWASP report missing"
}
