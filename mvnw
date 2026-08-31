#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
MAVEN_HOME="$DIR/.tools/apache-maven-3.9.9"

if [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
    echo "[mvnw] Maven not found in .tools/apache-maven-3.9.9"
    exit 1
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
