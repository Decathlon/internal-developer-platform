#!/usr/bin/env bats

# Test for check_flyway_migration_order.sh script

setup() {
    # Create a temporary git repository acting as the checked out pull request
    TEST_DIR="$(mktemp -d)"
    REPO_DIR="$TEST_DIR/repo"
    REPORT_FILE="$TEST_DIR/report.md"
    MIGRATION_DIR="src/main/resources/db/migration"

    SCRIPT_PATH="${BATS_TEST_DIRNAME}/../scripts/check_flyway_migration_order.sh"

    git init -q -b main "$REPO_DIR"
    git -C "$REPO_DIR" config user.email "ci@example.com"
    git -C "$REPO_DIR" config user.name "CI"
    mkdir -p "$REPO_DIR/$MIGRATION_DIR"

    write_migration "V5_1__create_first_table.sql"
    write_migration "V5_2__create_second_table.sql"
    write_migration "V6_1__create_third_table.sql"
    commit_all "chore: initial migrations"

    # The pull request branch starts from the base branch
    git -C "$REPO_DIR" checkout -q -b feature
}

teardown() {
    rm -rf "$TEST_DIR"
}

# Create or overwrite a migration file in the working tree
write_migration() {
    printf -- '-- %s\n' "$1" > "$REPO_DIR/$MIGRATION_DIR/$1"
}

commit_all() {
    git -C "$REPO_DIR" add -A
    git -C "$REPO_DIR" commit -q -m "$1"
}

# Add a migration on the base branch and bring it back into the pull request branch
merge_migration_from_main() {
    git -C "$REPO_DIR" checkout -q main
    write_migration "$1"
    commit_all "chore: add $1 on main"
    git -C "$REPO_DIR" checkout -q feature
    git -C "$REPO_DIR" merge -q --no-edit main
}

run_check() {
    run bash -c "cd '$REPO_DIR' && REPORT_FILE='$REPORT_FILE' bash '$SCRIPT_PATH' '$MIGRATION_DIR' main"
}

@test "exits with 0 when no migration is changed" {
    printf -- 'readme\n' > "$REPO_DIR/README.md"
    commit_all "docs: add readme"

    run_check
    [ "$status" -eq 0 ]
    [[ "$output" =~ "No migration change detected" ]]
}

@test "exits with 0 when the added migration is above the highest merged version" {
    write_migration "V6_2__create_fourth_table.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 0 ]
    [[ "$output" =~ "No Flyway migration ordering issue detected" ]]
}

@test "exits with 0 when the added migration uses a new major version" {
    write_migration "V7_1__create_new_domain_table.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 0 ]
    [[ "$output" =~ "Added migration V7_1__create_new_domain_table.sql (version 7.1)" ]]
}

@test "exits with 0 when several ordered migrations are added" {
    write_migration "V6_2__create_fourth_table.sql"
    write_migration "V6_3__create_fifth_table.sql"
    commit_all "feat: add migrations"

    run_check
    [ "$status" -eq 0 ]
    [[ "$output" =~ "V6_2__create_fourth_table.sql" ]]
    [[ "$output" =~ "V6_3__create_fifth_table.sql" ]]
}

@test "fails when a migration is inserted below the highest merged version" {
    write_migration "V5_3__insert_in_the_middle.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 1 ]
    [[ "$output" =~ "V5_3__insert_in_the_middle.sql" ]]
    [[ "$output" =~ "is lower than or equal to" ]]
    [[ "$output" =~ "V6_1__create_third_table.sql" ]]
}

@test "fails when the added migration reuses a version already merged" {
    write_migration "V6_1__another_description.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 1 ]
    [[ "$output" =~ "is lower than or equal to" ]]
}

@test "fails when a migration already merged is modified" {
    printf -- '-- extra statement\n' >> "$REPO_DIR/$MIGRATION_DIR/V5_1__create_first_table.sql"
    commit_all "fix: change merged migration"

    run_check
    [ "$status" -eq 1 ]
    [[ "$output" =~ "V5_1__create_first_table.sql" ]]
    [[ "$output" =~ "is modified" ]]
}

@test "fails when a migration already merged is deleted" {
    git -C "$REPO_DIR" rm -q "$MIGRATION_DIR/V5_2__create_second_table.sql"
    commit_all "fix: remove merged migration"

    run_check
    [ "$status" -eq 1 ]
    [[ "$output" =~ "V5_2__create_second_table.sql" ]]
    [[ "$output" =~ "is deleted or renamed" ]]
}

@test "fails when two added migrations share the same version" {
    write_migration "V7_1__first_change.sql"
    write_migration "V7_1__second_change.sql"
    commit_all "feat: add migrations"

    run_check
    [ "$status" -eq 1 ]
    [[ "$output" =~ "uses version" ]]
    [[ "$output" =~ "Flyway versions must be unique" ]]
}

@test "fails when the added migration does not follow the naming convention" {
    write_migration "create_table.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 1 ]
    [[ "$output" =~ "does not follow the Flyway naming convention" ]]
}

@test "compares versions numerically and not alphabetically" {
    merge_migration_from_main "V10_1__create_tenth_table.sql"

    write_migration "V9_1__create_ninth_table.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 1 ]
    [[ "$output" =~ "V10_1__create_tenth_table.sql" ]]
    [[ "$output" =~ "is lower than or equal to" ]]
}

@test "exits with 0 when the added migration is above a two digit major version" {
    merge_migration_from_main "V10_1__create_tenth_table.sql"

    write_migration "V10_2__create_eleventh_table.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 0 ]
    [[ "$output" =~ "Added migration V10_2__create_eleventh_table.sql (version 10.2)" ]]
}

@test "compares minor versions numerically" {
    merge_migration_from_main "V6_10__create_tenth_minor_table.sql"

    write_migration "V6_9__create_ninth_minor_table.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 1 ]
    [[ "$output" =~ "is lower than or equal to" ]]
}

@test "emits a GitHub Actions error annotation on failure" {
    write_migration "V5_3__insert_in_the_middle.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 1 ]
    [[ "$output" =~ "::error::" ]]
}

@test "writes the markdown report listing the added migrations" {
    write_migration "V6_2__create_fourth_table.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 0 ]
    [ -f "$REPORT_FILE" ]
    grep -q '| `V6_2__create_fourth_table.sql` | `6.2` |' "$REPORT_FILE"
}

@test "writes the markdown report listing the ordering errors" {
    write_migration "V5_3__insert_in_the_middle.sql"
    commit_all "feat: add migration"

    run_check
    [ "$status" -eq 1 ]
    [ -f "$REPORT_FILE" ]
    grep -q "The following Flyway migration issues must be fixed" "$REPORT_FILE"
    grep -q "V5_3__insert_in_the_middle.sql" "$REPORT_FILE"
}

@test "fails when the base ref does not exist" {
    run bash -c "cd '$REPO_DIR' && REPORT_FILE='$REPORT_FILE' bash '$SCRIPT_PATH' '$MIGRATION_DIR' origin/unknown"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "is not available" ]]
}

@test "accepts any migration when the base branch has no migration" {
    EMPTY_REPO="$TEST_DIR/empty"
    git init -q -b main "$EMPTY_REPO"
    git -C "$EMPTY_REPO" config user.email "ci@example.com"
    git -C "$EMPTY_REPO" config user.name "CI"
    mkdir -p "$EMPTY_REPO/$MIGRATION_DIR"
    printf -- 'readme\n' > "$EMPTY_REPO/README.md"
    git -C "$EMPTY_REPO" add -A
    git -C "$EMPTY_REPO" commit -q -m "chore: init"
    git -C "$EMPTY_REPO" checkout -q -b feature
    printf -- '-- first\n' > "$EMPTY_REPO/$MIGRATION_DIR/V1_1__create_first_table.sql"
    git -C "$EMPTY_REPO" add -A
    git -C "$EMPTY_REPO" commit -q -m "feat: add migration"

    run bash -c "cd '$EMPTY_REPO' && REPORT_FILE='$REPORT_FILE' bash '$SCRIPT_PATH' '$MIGRATION_DIR' main"
    [ "$status" -eq 0 ]
    [[ "$output" =~ "No migration found on main" ]]
}

@test "reads the migration directory and base ref from environment variables" {
    write_migration "V5_3__insert_in_the_middle.sql"
    commit_all "feat: add migration"

    run bash -c "cd '$REPO_DIR' && MIGRATION_DIR='$MIGRATION_DIR' BASE_REF=main REPORT_FILE='$REPORT_FILE' bash '$SCRIPT_PATH'"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "V5_3__insert_in_the_middle.sql" ]]
}

@test "ignores non SQL files added in the migration directory" {
    printf -- 'notes\n' > "$REPO_DIR/$MIGRATION_DIR/README.md"
    commit_all "docs: add migration notes"

    run_check
    [ "$status" -eq 0 ]
    [[ "$output" =~ "No migration change detected" ]]
}
