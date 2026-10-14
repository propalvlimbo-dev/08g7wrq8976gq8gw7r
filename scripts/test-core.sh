#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
mkdir -p build/core-test
"$JAVAC" --release 21 -d build/core-test \
  src/main/java/dev/scoreboardtweaks/core/ColorMath.java \
  src/main/java/dev/scoreboardtweaks/core/Layout.java \
  src/main/java/dev/scoreboardtweaks/core/DragGesture.java \
  src/test/java/dev/scoreboardtweaks/core/CoreSelfTest.java
"$JAVA" -cp build/core-test dev.scoreboardtweaks.core.CoreSelfTest
