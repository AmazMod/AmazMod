#!/system/bin/sh
TAG="AmazMod install_apk"
SYSTYPE=$(getprop | grep display.id)
BUSYBOX="$3/busybox"
BUSYBOXOK="FALSE"
#sleep 3
{
if [[ "$4" == "" ]]; then
    OLDPATH=$PATH
    echo "#### AmazMod install_apk Date: $(date)"
    echo "#### starting, arg1=($1) arg2=($2) arg3=($3) arg4=($4)"
    echo "#### system: $SYSTYPE"
    echo "#### PWD: $PWD"
    echo "#### PATH:$PATH OLDPATH:$OLDPATH"
else
    echo "#### continuing, arg1=($1) arg2=($2) arg3=($3) arg4=($4)"
    OLDPATH=$4
    echo "#### PATH:$PATH OLDPATH:$OLDPATH"
fi
if [[ ! -s /system/bin/adb ]] && [[ "$4" == "" ]]; then
     echo "#### adb not found, quitting!"
     exit 1
fi
if [[ ! -s $1 ]] && [[ "$4" == "" ]]; then
     echo "#### APK file not found, quitting!"
     exit 1
fi
if [[ -s ${BUSYBOX}  ]] && [[ "$4" == "" ]]; then
    BUSYBOXOK=$(${BUSYBOX} printf "BusyBox")
    echo "#### busybox: $(${BUSYBOX} | ${BUSYBOX} head -1)"
fi
if [[ ! "$BUSYBOXOK" == "BusyBox" ]] && [[ "$4" == "" ]]; then
    echo "#INSTALL_APK#">&2
    echo "#### busybox is not working! Installing APK and quitting!"
    adb install -r $1&
    exit 0
fi
if [[ "$4" == "" ]]; then
   PATH=$3:${OLDPATH}
   echo "#### restarting in the background"
   #sleep 3
   echo $(busybox nohup sh $0 "$1" $2 $3 ${OLDPATH} &)
   PATH=${OLDPATH}
   exit 0
else
    echo "#START#">&2
fi
echo "#### killing adb server $(adb kill-server)"
sleep 1
if [[ "$1" != "" ]]; then
   echo "#### installing: $1"
   run_cmd=$(adb install -r "$1" 2>&1)
   echo "$run_cmd"
fi
if [[ "$2" == "DEL" ]]; then
   run_cmd=$(rm "$1" 2>&1)
   echo "#### deleting file: $1 $run_cmd"
fi
#sleep 3
#$LOG "restore screen timeout $(adb shell settings put system screen_off_timeout 14000)"
#$LOG "killing background processes"
echo "#### killing adb server $(adb kill-server)"
#$LOG "kill-all $(am kill-all)"
PATH=${OLDPATH}
echo "#### Installation finished"
echo "#END#">&2
exit 0
} | while read line; do
   log -p d -t "$TAG" "$line"
done