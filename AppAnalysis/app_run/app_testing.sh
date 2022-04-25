#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "[Error] please provide apk path"
    exit 1
fi

keystore_path="./app.keystore"
apk_path=$1
apk_path=`realpath $apk_path`
#apk_path=`readlink -f -- $apk_path`

# check if adb device is connected
adb_out=`adb devices | awk 'NR>1 {print $1}'`
if test -z "$adb_out"
then
    echo "[Error] adb device is not connected"
    exit $?
fi

# get app package name
packagename=$(aapt dump badging $apk_path  | grep "package: " | awk -F"'" '{print $2}')
if [ -z "$packagename" ]
then
    apkbasename=$(basename $apk_path)
    packagename=${apkbasename%.apk}
fi

if [ -f "${apk_path}.logcat" ]
then
    echo "logcat file already exist, exiting..."
    exit 1
fi

pwdir=$(pwd)
tmp=$pwdir/tmp
signed=`jarsigner -verify -verbose -certs $apk_path | grep "jar is unsigned\|signatures missing or not parsable"`
if [[ -z $signed ]]
then
    echo "$apk_path signed"
else
    echo "$apk_path not signed, signing ... "
    rm -rf $tmp;unzip $apk_path -d $tmp;mv $apk_path ${apk_path}.original;cd $tmp;rm -rf ./META-INF/*;zip -r $apk_path ./*;cd $pwdir
    jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore $keystore_path -storepass anonymous $apk_path anonymous
fi

mainactivity=$(aapt dump badging $apk_path  | grep "launchable-activity" | awk -F"'" '{print $2}')
# it is used to handle the <activity-alias> tag in Android apps
if [ -z "$mainactivity" ]; then
    mainactivity=$(aapt dump xmltree $apk_path AndroidManifest.xml | grep "android:targetActivity\|LAUNCHER" | grep -B 1 LAUNCHER | grep -v LAUNCHER | awk -F"\"" '{print $2}')
fi

dev_adb="adb"
installed=`$dev_adb shell pm list package | grep $packagename`
if [[ -z $installed ]]
then
    echo "installing apk $apk_path to emulator"
    status=$($dev_adb install $apk_path)
    if [[ $status = *"FAILED"* ]]
    then
        echo "$status"
        echo "Failed to install $packagename"
        exit 1
    fi
else
   echo -n "$apk already there, test the original one"
fi

# kill all "adb logcat" processes
ps -ef | grep "$dev_adb logcat" | grep -v grep | awk '{print $2}' | xargs -I{} kill -9 {}

# clear logcat
$dev_adb logcat -c
$dev_adb logcat >> ${apk_path}.logcat &

TIMES=3
COUNTER=0
while [[ $COUNTER -lt $TIMES ]]
do
    let COUNTER=COUNTER+1
    echo "starting main activity $mainactivity in $apk_path"

    # kill running
    $dev_adb shell am force-stop $packagename
    $dev_adb shell "ps | grep $packagename | grep -v grep" | awk '{print $2}' | xargs -I{} $dev_adb shell kill -9 {}
    $dev_adb shell pm clear $packagename

    # restart task
    $dev_adb shell am start -n $packagename/$mainactivity
    residue=$(($COUNTER%5))
    sleep 2
done

# kill all "adb logcat" processes
ps -ef | grep "$dev_adb logcat" | grep -v grep | awk '{print $2}' | xargs -I{} kill -9 {}
# uninstall the testing app
$dev_adb shell pm uninstall $packagename
