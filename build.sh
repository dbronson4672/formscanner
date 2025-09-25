#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}" >/dev/null

M2_REPO="${HOME}/.m2/repository"
QUAQUA_COORD_PATH="ch/randelshofer/quaqua/8.0/quaqua-8.0.jar"
QUAQUA_JAR="formscanner-commons/src/main/resources/lib/quaqua-8.0.jar"

if [[ ! -f "${M2_REPO}/${QUAQUA_COORD_PATH}" ]]; then
  echo "Installing Quaqua dependency into local Maven repository..."
  mvn --batch-mode install:install-file \
    -Dfile="${QUAQUA_JAR}" \
    -DgroupId=ch.randelshofer \
    -DartifactId=quaqua \
    -Dversion=8.0 \
    -Dpackaging=jar
else
  echo "Quaqua dependency already installed. Skipping local install."
fi

echo "Building FormScanner distribution..."
mvn --batch-mode -pl formscanner-distribution -am \
  -DskipTests \
  -Dmaven.javadoc.skip=true \
  -Dgpg.skip=true \
  clean package

echo "Build complete. Distribution artifacts are under formscanner-distribution/target"
