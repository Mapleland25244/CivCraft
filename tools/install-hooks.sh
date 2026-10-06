#!/usr/bin/env bash
# Point this clone at the versioned hooks in .githooks (a per-clone setting, so each developer runs it once).
# Undo with: git config --unset core.hooksPath
set -eu
cd "$(dirname "$0")/.."
git config core.hooksPath .githooks
echo "core.hooksPath = $(git config core.hooksPath)"
echo "pre-commit now runs tools/pre-commit-check.sh (skip once with: git commit --no-verify)"
