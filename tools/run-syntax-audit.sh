#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p tools/.audit-classes
javac --release 17 -d tools/.audit-classes tools/Java17SyntaxAudit.java
java -cp tools/.audit-classes Java17SyntaxAudit src/main/java
