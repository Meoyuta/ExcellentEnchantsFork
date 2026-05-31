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

if [ -n "${JAVA_HOME:-}" ]; then
    if [ ! -x "$JAVA_HOME/bin/java" ]; then
        echo "[ERROR] JAVA_HOME is set but java was not found: $JAVA_HOME"
        exit 1
    fi
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
    echo "[ERROR] Java not found. Set JAVA_HOME or add JDK 25 to PATH."
    exit 1
fi

if ! command -v javac >/dev/null 2>&1; then
    echo "[ERROR] javac not found. Set JAVA_HOME or add JDK 25 to PATH."
    exit 1
fi

echo "[INFO] Detect JDK Version:"
java -version
echo

echo "[INFO] Cleaning previous build..."
mvn clean "$@"
echo

echo "[INFO] Building ExcellentEnchants..."
mvn package -DskipTests "$@"
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
