#!/system/bin/sh
TAG="AmazMod ExecCommand"
SYSTYPE=$(getprop | grep display.id)
BUSYBOX="busybox"
{
echo "#### AmazMod ADB script Date: $(date)"
echo "#### System: $SYSTYPE"
echo "#### PATH: $PATH"
echo "#### PWD: $PWD"
echo "#### starting, arg1 = ($1)"
if [[ ! -s /system/bin/adb ]]; then
     echo "#### adb not found, quitting!"
     exit 1
fi
BUSYBOXOK=$(${BUSYBOX} printf "BusyBox")
echo "#### busybox: $(${BUSYBOX} | ${BUSYBOX} head -1)"
if [[ ! "$BUSYBOXOK" == "BusyBox" ]]; then
    echo "#### busybox not found!"
fi
echo "#START#">&2
echo "#### killing adb server $(adb kill-server)"
if [[ "$1" != "" ]]; then
   echo "#### executing command: $1"
   $1 2>&1
fi
echo "#### killing server $(adb kill-server)"
#echo "## $(am kill-all)"
echo "#### Script finished"
echo "#END#">&2
exit 0
} | while read line; do
   log -p d -t "$TAG" "$line"
done
