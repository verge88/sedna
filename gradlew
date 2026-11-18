#!/bin/sh
# Lightweight bootstrap: fetch the official Gradle 8.13 wrapper JAR on first use.
# This avoids checking a binary into the repository; GitHub Actions uses setup-gradle.
set -eu
APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
URL="https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
    mkdir -p "$APP_HOME/gradle/wrapper"
    TEMP="$JAR.download"
    trap 'rm -f "$TEMP"' EXIT HUP INT TERM
    if command -v curl >/dev/null 2>&1; then
        curl -fL --retry 2 --connect-timeout 15 "$URL" -o "$TEMP"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$TEMP" "$URL"
    else
        echo "curl or wget is required for the initial Gradle wrapper download" >&2
        exit 1
    fi
    if command -v git >/dev/null 2>&1; then
        ACTUAL="$(git hash-object "$TEMP")"
        EXPECTED="9bbc975c742b298b441bfb90dbc124400a3751b9"
        if [ "$ACTUAL" != "$EXPECTED" ]; then
            echo "Gradle wrapper JAR checksum verification failed" >&2
            exit 1
        fi
    fi
    mv "$TEMP" "$JAR"
    trap - EXIT HUP INT TERM
fi
exec java -cp "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
