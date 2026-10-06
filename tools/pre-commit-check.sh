#!/usr/bin/env bash
# R2.7: checks that should pass before a commit.
# Usage: tools/pre-commit-check.sh [--all]
#   (default) look at the staged files and run only the checks they affect
#   --all     run every check regardless of what is staged
#
# Checks:
#   tools/check-legacy-api.sh     pre-1.13 API ratchet; runs when Java sources or its baseline are staged (seconds)
#   tools/check-scheduler.sh      Bukkit scheduler only inside threading/; runs when Java sources are staged (seconds)
#   tools/scan-templates.sh       legacy block pairs in templates; runs when templates, its tool or its baseline are
#                                 staged (about a minute, so it is skipped for everything else)
set -u
cd "$(dirname "$0")/.." || exit 2

ALL=0
[ "${1:-}" = "--all" ] && ALL=1

staged=$(git diff --cached --name-only --diff-filter=ACMR)
touches() { printf '%s\n' "$staged" | grep -Eq "$1"; }

status=0

if [ "$ALL" = 1 ] || touches '^civcraft/src/.*\.java$|^tools/check-legacy-api\.sh$|^tools/legacy-api-baseline\.txt$'; then
	echo "[pre-commit] legacy API ratchet"
	bash tools/check-legacy-api.sh || status=1
fi

if [ "$ALL" = 1 ] || touches '^civcraft/src/.*\.java$|^tools/check-scheduler\.sh$'; then
	echo "[pre-commit] scheduler boundary"
	bash tools/check-scheduler.sh || status=1
fi

if [ "$ALL" = 1 ] || touches '^civcraft_data/templates/|^tools/ScanTemplates\.java$|^tools/scan-templates\.sh$|^tools/template-blocks-baseline\.txt$'; then
	echo "[pre-commit] template block inventory (about a minute)"
	bash tools/scan-templates.sh --check || status=1
fi

if [ "$status" != 0 ]; then
	echo "[pre-commit] failed. Fix the reported problem, or if the change is intended update the baseline:" >&2
	echo "  tools/check-legacy-api.sh --write-baseline    /    tools/scan-templates.sh --write" >&2
fi
exit $status
