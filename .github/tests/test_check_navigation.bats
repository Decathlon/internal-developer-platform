#!/usr/bin/env bats

# Test for check_navigation.sh script

setup() {
    # Create temporary test directory
    TEST_DIR="$(mktemp -d)"
    SITE_DIR="$TEST_DIR/site"
    SRC_DIR="$TEST_DIR/src"
    mkdir -p "$SITE_DIR" "$SRC_DIR"

    SCRIPT_PATH="${BATS_TEST_DIRNAME}/../scripts/check_navigation.sh"
}

teardown() {
    # Clean up temporary directory
    rm -rf "$TEST_DIR"
}

@test "exits with 0 when no HTML files exist" {
    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 0 ]
    [[ "$output" =~ "✅ No broken navigation links found" ]]
}

@test "exits with 0 when HTML files have no 'none' placeholders" {
    cat > "$SITE_DIR/test.html" <<EOF
<html>
<body>
  <span class="md-ellipsis">
    Valid Title
  </span>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 0 ]
    [[ "$output" =~ "✅ No broken navigation links found" ]]
}

@test "detects 'none' placeholder in navigation" {
    cat > "$SITE_DIR/test.html" <<EOF
<html>
<body>
  <a href="broken.md" class="md-nav__link">
    <span class="md-ellipsis">
      none
    </span>
  </a>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "📝 Markdown files referenced in broken navigation links" ]]
}

@test "shows markdown filename in error output" {
    cat > "$SITE_DIR/page.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="missing-title.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "missing-title.md" ]]
}

@test "handles multiple broken links in same file" {
    cat > "$SITE_DIR/multi.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="first.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
  <li class="md-nav__item">
    <a href="second.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "first.md" ]]
    [[ "$output" =~ "second.md" ]]
}

@test "handles multiple broken links across multiple files" {
    cat > "$SITE_DIR/page1.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="broken1.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    cat > "$SITE_DIR/page2.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="broken2.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "broken1.md" ]]
    [[ "$output" =~ "broken2.md" ]]
}

@test "ignores 'none' text not in md-ellipsis context" {
    cat > "$SITE_DIR/test.html" <<EOF
<html>
<body>
  <p>This is none of my business</p>
  <div>none</div>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 0 ]
    [[ "$output" =~ "✅ No broken navigation links found" ]]
}

@test "finds existing markdown files in source directory" {
    mkdir -p "$SRC_DIR/concepts"
    touch "$SRC_DIR/concepts/test-file.md"

    cat > "$SITE_DIR/page.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="concepts/test-file.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "$SRC_DIR/concepts/test-file.md" ]]
}

@test "handles relative paths in hrefs" {
    cat > "$SITE_DIR/page.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="../concepts/relative.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "relative.md" ]]
}

@test "accepts custom site directory as first argument" {
    CUSTOM_SITE="$TEST_DIR/custom_site"
    mkdir -p "$CUSTOM_SITE"

    cat > "$CUSTOM_SITE/test.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="broken.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$CUSTOM_SITE" "$SRC_DIR"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "broken.md" ]]
}

@test "shows helpful fix message on failure" {
    cat > "$SITE_DIR/test.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="broken.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 1 ]
    [[ "$output" =~ "💡 Fix: Add 'title: Your Title' in YAML frontmatter" ]]
}

@test "deduplicates markdown file references" {
    cat > "$SITE_DIR/page1.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="same-file.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    cat > "$SITE_DIR/page2.html" <<EOF
<html>
<body>
  <li class="md-nav__item">
    <a href="same-file.md" class="md-nav__link">
      <span class="md-ellipsis">
        none
      </span>
    </a>
  </li>
</body>
</html>
EOF

    run bash "$SCRIPT_PATH" "$SITE_DIR" "$SRC_DIR"
    [ "$status" -eq 1 ]
    # Should only show same-file.md once
    count=$(echo "$output" | grep -c "same-file.md" || true)
    [ "$count" -eq 1 ]
}
