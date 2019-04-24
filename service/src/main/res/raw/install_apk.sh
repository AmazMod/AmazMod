#!/system/bin/sh
TAG="AmazMod install_apk"
SYSTYPE=$(getprop | grep display.id)
BUSYBOX="$3/busybox"
BUSYBOXOK="FALSE"
LOG="log -pd -t$TAG"
if [ "$4" == "" ]; then
    OLDPATH=$PATH
    $LOG "#### AmazMod install_apk Date: $(date)"
    $LOG "starting, arg1=($1) arg2=($2) arg3=($3) arg4=($4)"
    $LOG "system: $SYSTYPE"
    $LOG "PWD: $PWD"
    $LOG "PATH:$PATH OLDPATH:$OLDPATH"
else
    $LOG "continuing, arg1=($1) arg2=($2) arg3=($3) arg4=($4)"
    OLDPATH=$4
    $LOG "PATH:$PATH OLDPATH:$OLDPATH"
fi
if [ ! -s /system/bin/adb ] && [ "$4" == "" ]; then
     $LOG "adb not found, quitting!"
     exit 1
fi
if [ ! -s $1 ] && [ "$4" == "" ]; then
     $LOG "APK file not found, quitting!"
     exit 1
fi
if [ -s $BUSYBOX  ] && [ "$4" == "" ]; then
    BUSYBOXOK=$($BUSYBOX printf "BusyBox")
    $LOG "busybox: $($BUSYBOX | $BUSYBOX head -1)"
fi
if [ ! "$BUSYBOXOK" == "BusyBox" ] && [ "$4" == "" ]; then
    echo "#INSTALL_APK#">&2
    $LOG "busybox is not working! Installing APK and quitting!"
    adb install -r $1&
    exit 0
fi
if [ "$4" == "" ]; then
   PATH=$3:$OLDPATH
   $LOG "restarting in the background"
   sleep 3
   $LOG $(busybox nohup sh $0 "$1" $2 $3 $OLDPATH 2>&1 &)
   PATH=$OLDPATH
   exit 0
else
    echo "#START#">&2
    run_cmd=$(adb shell "echo APK_INSTALL > /sys/power/wake_lock")
    log -pi -tAmazMod "Enabling APK_INSTALL WAKELOCK: $run_cmd"
fi
$LOG "killing adb server $(adb kill-server)"
sleep 3
if [ "$1" != "" ]; then
   $LOG "installing: $1"
   run_cmd=$(adb install -r "$1" 2>&1)
   log -pi -tAmazMod "install_apk adb: $run_cmd"
fi
if [ "$2" == "DEL" ]; then
   $LOG "deleting file: $1 $(rm '$1')"
fi
$LOG "killing background processes"
$LOG "killing adb $(adb kill-server)"
$LOG "kill-all $(am kill-all)"
PATH=$OLDPATH
$LOG "Installation finished"
echo "#END#">&2
exit 0
