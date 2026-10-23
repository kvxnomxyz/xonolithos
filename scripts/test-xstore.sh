#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
JSON_JAR=${JSON_TEST_JAR:-$ROOT/.cache/json-test.jar}
[[ -f $JSON_JAR ]] || { echo 'Set JSON_TEST_JAR to org.json:json:20240303 (test dependency only).' >&2; exit 1; }
OUT=$(mktemp -d)
trap 'rm -rf -- "$OUT"' EXIT
javac -cp "$JSON_JAR" -d "$OUT" \
    "$ROOT/apps/Xstore/src/org/xonolithos/xstore/Catalog.java" "$ROOT/tests/CatalogTest.java"
java -cp "$OUT:$JSON_JAR" org.xonolithos.xstore.CatalogTest
