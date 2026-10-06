#!/usr/bin/env bash
# R2.6: inventory of the legacy id:data block pairs used by the building templates.
# Usage: tools/scan-templates.sh [--write|--check]     (default: --check)
#   --write   regenerate tools/template-blocks-baseline.txt
#   --check   exit 1 when a template uses a pair that is not in the baseline, or any pair is malformed
# Needs a JDK and civcraft/lib/spigot-api-1.12.2-R0.1-SNAPSHOT.jar. Scans ~47 million lines, about 15-30 s.
set -eu
cd "$(dirname "$0")/.."

MODE=${1:---check}
API=civcraft/lib/spigot-api-1.12.2-R0.1-SNAPSHOT.jar
OUT=${TMPDIR:-/tmp}/scan-templates-classes

mkdir -p "$OUT"
javac -nowarn -encoding UTF-8 -cp "$API" -d "$OUT" tools/ScanTemplates.java

# Windows (Git Bash): java.exe needs ';' as the classpath separator and a Windows path for /tmp.
case "$(uname -s)" in
	MINGW*|MSYS*|CYGWIN*) SEP=';'; JOUT=$(cygpath -w "$OUT") ;;
	*) SEP=':'; JOUT=$OUT ;;
esac
java -cp "$JOUT$SEP$API" ScanTemplates civcraft_data/templates "$MODE" tools/template-blocks-baseline.txt
