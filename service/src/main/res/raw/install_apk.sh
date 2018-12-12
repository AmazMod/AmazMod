#/system/bin/sh
TAG="AmazMod install_apk"
SYSTYPE=$(getprop | grep display.id)
BUSYBOX="$3/busybox"
BUSYBOXOK="FALSE"
if [ "$4" == "" ]; then
    OLDPATH=$PATH
    LOG="log -pi -t$TAG"
else
    OLDPATH=$4
    LOG="echo"
fi
$LOG "#### AmazMod install_apk Date: $(date)"
$LOG "starting, arg1=($1) arg2=($2) arg3=($3) arg4=($4)"
$LOG "system: $SYSTYPE"
$LOG "PWD: $PWD"
if [ ! -s /system/bin/adb ]; then
     $LOG "adb not found, quitting!"
     exit 1
fi
if [ ! -s $1 ]; then
     $LOG "APK file not found, quitting!"
     exit 1
fi
if [ -s $BUSYBOX  ] && [ "$4" == "" ]; then
    BUSYBOXOK=$($BUSYBOX printf "BusyBox")
    $LOG "busybox: $($BUSYBOX | $BUSYBOX head -1)"
    echo "install_apk #END#">&2
fi
if [ ! "$BUSYBOXOK" == "BusyBox" ] && [ "$4" == "" ]; then
    $LOG "busybox is not working! Installing APK and quitting!"
    adb install -r $1&
    exit 0
fi
{
if [ "$4" == "" ]; then
   PATH=$3:$OLDPATH
   echo "restarting in the background"
   sleep 3
   busybox nohup sh $0 $1 $2 $3 $OLDPATH 2>&1 &
   PATH=$OLDPATH
   exit 0
fi
echo "killing adb server"
adb kill-server
sleep 3
if [ "$1" != "" ]; then
   echo "installing: $1"
   adb install -r $1
fi
if [ "$2" == "DEL" ]; then
   echo "deleting file: $1"
   rm $1
fi
echo "killing background processes"
adb kill-server
am kill-all
PATH=$OLDPATH
echo "Installation finished"
exit 0
} | while read LINE; do
   log -p i -t "$TAG" "$LINE"
done
