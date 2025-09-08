#!/usr/bin/env sh
# Simple launcher: if gradle wrapper jar exists, use it; else fall back to local gradle
DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$WRAPPER_JAR" ]; then
  exec java -jar "$WRAPPER_JAR" "$@"
else
  echo "gradle-wrapper.jar missing; falling back to local 'gradle'"
  exec gradle "$@"
fi
