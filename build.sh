#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="$ROOT_DIR/build.log"

exec > >(tee "$LOG_FILE") 2>&1

cd "$ROOT_DIR" || exit 1

on_exit() {
    status=$?
    if [ "$status" -ne 0 ]; then
        echo "[ERROR] Build failed."
    fi
}
trap on_exit EXIT

export MAVEN_OPTS="${MAVEN_OPTS:+$MAVEN_OPTS }-Dfile.encoding=UTF-8"

if [ -z "${JAVA_HOME:-}" ]; then
    for candidate in \
        "/d/java/jdk-25" \
        "/d/Java/jdk-25" \
        "/usr/lib/jvm/jdk-25" \
        "/usr/lib/jvm/java-25-openjdk" \
        "/opt/jdk-25"; do
        if [ -x "$candidate/bin/java" ]; then
            export JAVA_HOME="$candidate"
            break
        fi
    done
fi

if [ -n "${JAVA_HOME:-}" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "[INFO] Build log: $LOG_FILE"
echo "[INFO] Started at: $(date '+%Y-%m-%d %H:%M:%S %z')"
echo

if ! command -v mvn >/dev/null 2>&1; then
    echo "[ERROR] Maven not found. Please install Maven and add it to PATH."
    exit 1
fi

if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] JDK not found. Please install JDK 25 and add it to PATH or set JAVA_HOME."
    exit 1
fi

echo "[INFO] Detect JDK Version:"
java -version
echo

echo "[INFO] Cleaning previous build..."
mvn clean "$@" -T 8
echo

echo "[INFO] Building ExcellentEnchants..."
mvn package -DskipTests "$@" -T 8
echo

echo "[OK] Build complete!"
artifact_dirs=()
[ -d "$ROOT_DIR/target" ] && artifact_dirs+=("$ROOT_DIR/target")
[ -d "$ROOT_DIR/Core/target" ] && artifact_dirs+=("$ROOT_DIR/Core/target")

if [ "${#artifact_dirs[@]}" -gt 0 ]; then
    find "${artifact_dirs[@]}" -type f -name '*.jar' -print | while IFS= read -r artifact; do
        echo "[OK] Artifact: $artifact"
    done
fi

echo "[INFO] Finished at: $(date '+%Y-%m-%d %H:%M:%S %z')"