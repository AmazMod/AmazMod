#/system/bin/sh 
tag="AmazMod install_apk"
systype=$(getprop | grep display.id)
{
echo "Date: $(date)"
echo "System: $systype"
echo "PWD: $PWD"
if [ "$1" != "" && "$1" != "&" ]; then
   adb kill-server
   echo "installing: $1"
   adb shell pm install -r $1
fi 
echo "$1 installed"
sleep 3
adb shell am force-stop com.huami.watch.launcher
echo "launcher restarted"
sleep 3
adb shell dpm set-device-owner com.amazmod.service/.AdminReceiver
echo "device ownner set"
adb kill-server
echo "removing files from internal storage"
rm /sdcard/install_apk.sh
rm /sdcard/AmazMod-service-*.apk
echo "installation finished"
} | busybox tee /dev/tty | while read line; do
   log -p d -t "$tag" "$line"
done
