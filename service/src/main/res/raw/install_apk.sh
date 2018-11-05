#/system/bin/sh
tag="AmazMod $0"
systype=$(getprop | grep display.id)
{
echo "***** starting, arg1 = ($1) // arg2 = ($2) // arg3 = ($3)"
echo "Date: $(date)"
echo "System: $systype"
echo "PWD: $PWD"
if [ "$2" == "" ]; then
   echo "restarting in the background"
   cd /sdcard/
   adb kill-server
   sleep 3
   busybox nohup sh $0 $1 OK > /dev/null &
   exit 0
fi
if [ "$1" != "" ]; then
   echo "installing: $1"
   [[ -s $1 ]] && adb install -r $1 || exit 1
fi
sleep 3
adb kill-server
echo "Installation finished"
} | busybox tee /dev/tty | while read line; do
   log -p d -t "$tag" "$line"
done
