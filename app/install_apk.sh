#/system/bin/sh
if ["$2" == ""]; then
   adb kill-server
   busybox nohup sh /sdcard/install_apk.sh $1 OK &
   adb shell am force-stop com.amazmod.service
fi  
tag="AmazMod install_apk"
systype=$(getprop | grep display.id)
{
adb kill-server
date
echo "System: $systype"
echo "PWD: $PWD"
echo "installing: $1"
adb shell pm install -r $1
echo "$1 installed"
sleep 3
adb kill-server
adb shell am force-stop com.huami.watch.launcher
echo "launcher restarted"
sleep 3
adb kill-server
adb shell dpm set-device-owner com.amazmod.service/.AdminReceiver
echo "device ownner set"
adb kill-server
rm /sdcard/install_apk.sh
rm $1
echo "installation finished"
} | busybox tee /dev/tty | while read line; do
   log -p d -t "$tag" "$line"
done
