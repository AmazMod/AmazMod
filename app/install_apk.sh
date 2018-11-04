#/system/bin/sh 
tag="AmazMod install_apk"
systype=$(getprop | grep display.id)
{
echo "***** starting, arg1 = ($1) // arg2 = ($2) // arg3 = ($3)"
echo "Date: $(date)"
echo "System: $systype"
echo "PWD: $PWD"
cd /sdcard/
adb kill-server
sleep 3
adb shell am force-stop com.huami.watch.launcher
echo "launcher restarted"
sleep 3
adb shell dpm set-device-owner com.amazmod.service/.AdminReceiver
echo "device ownner set"
echo "removing files from internal storage"
rm /sdcard/install_apk.sh
rm /sdcard/AmazMod-service-*.apk
file="/sdcard/amazmod-command.sh"
[ -e $file ] && rm $file
echo "installation finished"
adb kill-server
} | busybox tee /dev/tty | while read line; do
   log -p d -t "$tag" "$line"
done
