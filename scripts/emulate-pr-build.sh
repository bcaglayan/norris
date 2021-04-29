#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";
cd "${DIR%/*}"

./gradlew clean --no-daemon
./gradlew ktlintCheck --no-daemon --stacktrace
./gradlew detekt --no-daemon --stacktrace
./gradlew jacocoTestReport jacocoTestReportDebug --no-daemon --stacktrace
./gradlew app:connectedAndroidTest --no-daemon --stacktrace -PtestMode=true