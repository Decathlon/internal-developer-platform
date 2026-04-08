#!/bin/bash
# Check for 'none' placeholders in Zensical navigation

set -e

SITE_DIR="${1:-docs/site}"
SRC_DIR="${2:-docs/src}"
EXIT_CODE=0
declare -A broken_refs

echo "🔍 Checking for broken navigation links in ${SITE_DIR}..."

# Find files with 'none' placeholders in navigation
while IFS= read -r file; do
    if [ -n "$file" ]; then
        # Get line numbers where 'none' appears in md-ellipsis context
        line_nums=$(grep -n "^\s*none\s*$" "$file" | cut -d: -f1 2>/dev/null || true)

        for line_num in $line_nums; do
            # Check if this 'none' is within md-ellipsis span (check nearby lines)
            # Ensure we don't use negative line numbers
            start_line=$((line_num > 3 ? line_num - 3 : 1))
            end_line=$((line_num + 3))

            if sed -n "${start_line},${end_line}p" "$file" | grep -q "md-ellipsis"; then
                # Extract the href to find the source markdown file
                # Look up to 10 lines before, but not before line 1
                href_start=$((line_num > 10 ? line_num - 10 : 1))
                href=$(sed -n "${href_start},${line_num}p" "$file" | grep -oP 'href="\K[^"]+' | tail -1 2>/dev/null || true)

                if [[ $href == *.md ]]; then
                    # Store the reference
                    broken_refs["$href"]=1
                fi

                EXIT_CODE=1
            fi
        done
    fi
done < <(find "$SITE_DIR" -name "*.html" -type f)

if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ No broken navigation links found"
else
    echo ""
    echo "📝 Markdown files referenced in broken navigation links:"
    echo ""

    # Show unique markdown file references
    for ref in $(printf '%s\n' "${!broken_refs[@]}" | sort -u); do
        # Extract just the filename for clarity
        filename=$(basename "$ref")

        # Try to find it in the source directory
        found_files=$(find "$SRC_DIR" -name "$filename" 2>/dev/null || true)

        if [ -n "$found_files" ]; then
            echo "$found_files" | while read -r found; do
                echo "   ❌ $found"
            done
        else
            echo "   ⚠️  $filename (referenced as: $ref, not found in $SRC_DIR)"
        fi
    done | sort -u

    echo ""
    echo "💡 Fix: Add 'title: Your Title' in YAML frontmatter"
    echo "        (at the top of the file between --- markers)"
fi

exit $EXIT_CODE
