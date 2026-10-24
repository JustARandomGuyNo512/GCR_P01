#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v gradle >/dev/null 2>&1; then
  echo 'ERROR: Gradle 8.x unavailable. Install Gradle 8.8+ and put it on PATH.' >&2
  exit 2
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo 'ERROR: Set JAVA_HOME to a Java 17 JDK before starting ForgeGradle.' >&2
  exit 3
fi
if ! "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q 'version "17'; then
  echo 'ERROR: JAVA_HOME must point to Java 17.' >&2
  exit 4
fi
# All dependencies are declared in build.gradle, no manual libs/*.jar needed.
echo 'Building GCR Forge 1.20.1 with LDLib2 2.2.41 through Curse Maven...'
gradle --no-daemon --stacktrace clean build
find build/libs -maxdepth 1 -type f -name '*.jar' -size +10k -print
