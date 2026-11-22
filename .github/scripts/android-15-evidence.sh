#!/usr/bin/env bash
set -eu

chmod +x gradlew
mkdir -p build/visual-evidence/before build/visual-evidence/after

baseline="${RUNNER_TEMP}/erthi-baseline/app-debug.apk"
if [[ -s "$baseline" ]] && adb install "$baseline"; then
    adb shell am start -W -n com.erthi.amanah.debug/com.erthi.amanah.MainActivity
    baseline_ready=false
    for _ in 1 2 3 4 5; do
        if adb shell uiautomator dump /sdcard/erthi-baseline.xml >/dev/null &&
            adb shell cat /sdcard/erthi-baseline.xml | grep -q 'ابدأ الآن'; then
            baseline_ready=true
            break
        fi
        sleep 1
    done
    if [[ "$baseline_ready" == true ]]; then
        adb exec-out screencap -p > build/visual-evidence/before/01-onboarding.png
        adb shell cat /sdcard/erthi-baseline.xml > build/erthi-baseline.xml
        if coordinates=$(python3 - <<'PY'
import re
import xml.etree.ElementTree as ET

root = ET.parse("build/erthi-baseline.xml")
for node in root.iter("node"):
    if node.get("text") == "ابدأ الآن":
        l, t, r, b = map(int, re.findall(r"\d+", node.get("bounds", "")))
        print((l + r) // 2, (t + b) // 2)
        break
else:
    raise SystemExit(1)
PY
        ); then
            read -r x y <<< "$coordinates"
            adb shell input tap "$x" "$y"
            sleep 1
            adb exec-out screencap -p > build/visual-evidence/before/02-create-pin.png
        fi
    fi
    # Only the disposable emulator's synthetic baseline install is removed.
    adb uninstall com.erthi.amanah.debug
fi

status=0
./gradlew connectedDebugAndroidTest --no-daemon || status=$?
adb pull /sdcard/Android/data/com.erthi.amanah.debug/files/screenshots build/visual-evidence/after/ || true
exit "$status"
