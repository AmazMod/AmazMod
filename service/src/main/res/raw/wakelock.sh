#!/system/bin/sh
TAG="Disabling APK_INSTALL WAKELOCK"
SYSTYPE=$(getprop | grep display.id)
LOG="log -pd -t$TAG"
    run_cmd=$(adb shell "echo APK_INSTALL > /sys/power/wake_unlock")
    log -pi -tAmazMod "Disabling APK_INSTALL WAKELOCK: $run_cmd"
    run_cmd=$(adb shell input keyevent 26)
    log -pi -tAmazMod "Wake up Screen: $run_cmd"
exit 0
