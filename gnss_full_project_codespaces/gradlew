#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -x "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
else
  echo "gradle wrapper jar not found. Running system gradle if available..."
  gradle "$@"
fi
