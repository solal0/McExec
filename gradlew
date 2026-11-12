#!/bin/sh
# McExec lightweight, source-included Gradle bootstrap.
set -eu
APP_HOME=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
cd "$APP_HOME"
if [ -n "${JAVA_HOME:-}" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi
exec "$JAVA" -cp "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
