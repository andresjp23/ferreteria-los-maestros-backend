#!/bin/sh
set -eu
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "Descargando Maven Wrapper oficial en la primera ejecucion..."
  if command -v curl >/dev/null 2>&1; then
    curl -fLSs https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar -o "$JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar -O "$JAR"
  else
    echo "ERROR: instalar curl o wget, o ejecutar Maven ya instalado." >&2
    exit 1
  fi
fi
exec java -Dmaven.multiModuleProjectDirectory="$BASE_DIR" -cp "$JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
