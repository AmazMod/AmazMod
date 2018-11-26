#/system/bin/sh
tag="AmazMod $0"
SYSTYPE=$(getprop | grep display.id)
{
echo "***** starting, arg1 = ($1) // arg2 = ($2) // arg3 = ($3)"
echo "Date: $(date)"
echo "System: $SYSTYPE"
echo "PWD: $PWD"
cd /sdcard/
if [ "$1" == "kill-all" ]; then
    echo "killing background processes"
    adb kill-server
    am kill-all
    echo "finished"
    exit 0
fi
exit 0
} | busybox tee /dev/tty | while read line; do
   log -p d -t "$tag" "$line"
done
