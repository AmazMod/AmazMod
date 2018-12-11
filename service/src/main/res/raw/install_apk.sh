#/system/bin/sh
tag="AmazMod $0"
SYSTYPE=$(getprop | grep display.id)
echo "starting, arg1 = ($1) // arg2 = ($2) // arg3 = ($3)"
echo "Date: $(date)"
echo "System: $SYSTYPE"
echo "PWD: $PWD"
#cd /sdcard/
if [ "$2" == "" ]; then
   echo "restarting in the background"
   sleep 1
   nohup sh $0 $1 OK > /dev/null &
   exit 0
fi
echo "killing adb server"
adb kill-server
sleep 3
if [ "$1" != "" ]; then
   echo "installing: $1"
   [[ -s $1 ]] && adb install -r $1 || exit 1
fi
if [ "$2" == "DEL" ]; then
   echo "deleting file: $1"
   rm $1
fi
echo "killing background processes"
adb kill-server
am kill-all
echo "Installation finished"
exit 0
