#!/usr/bin/env bash
# 在已启动的模拟器上安装 APK、运行 UI 测试，并把截图和崩溃日志拉到 ui-test-output/。
set -u
OUT=ui-test-output
mkdir -p "$OUT"

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c

adb shell am instrument -w com.aihub.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$OUT/instrument.txt"

adb pull /sdcard/Android/data/com.aihub.app/files/screenshots "$OUT/" || true
adb logcat -d -b crash > "$OUT/crash.txt" || true
adb logcat -d '*:E' > "$OUT/errors.txt" || true

echo "---- crash buffer ----"
cat "$OUT/crash.txt"

grep -q "^OK (" "$OUT/instrument.txt"
