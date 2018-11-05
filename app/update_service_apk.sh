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
echo "removing files from internal storage"
rm /sdcard/AmazMod-service-*.apk
file="/sdcard/amazmod-command.sh"
[ -e $file ] && rm $file
echo "update finished"
rm /sdcard/update_service_apk.sh
} | busybox tee /dev/tty | while read line; do
   log -p d -t "$tag" "$line"
done
