#!/usr/bin/env bash
set -e

APK=$(find apk -name '*.apk' | head -n 1)
echo "Found APK: ${APK:-NONE}"
test -n "$APK" || { echo "ERROR: no APK found"; exit 1; }

echo "Installing $APK"
adb install -r -g "$APK" || adb install -r "$APK"

echo "Launching app"
adb shell am start -W -n com.exemu/com.swordfish.lemuroid.app.mobile.feature.main.MainActivity
sleep 8

PID=$(adb shell pidof com.exemu)
echo "App PID: ${PID:-NONE}"

if [ -n "$PID" ]; then
  echo "SMOKE-TEST PASS: app is running"
else
  echo "SMOKE-TEST FAIL: app crashed"
  adb logcat -d -t 200 | grep -E "FATAL|AndroidRuntime|lemuroid" || true
  exit 1
fi

adb exec-out screencap -p > smoke-screenshot.png
ls -la smoke-screenshot.png