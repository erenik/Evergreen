#!/bin/sh
echo Cleaning 
rm -r -f ./java/
echo Copying
cp -r ../evergreen-game/src/main/java/ ./java/
echo Ripping out Android-specific files and code.
rm -r ./java/erenik/evergreen/android/
echo Building
javac -cp "./java/;D:\weka3-stable-3-8\weka/weka.jar" -sourcepath "./java/;D:/weka3-stable-3-8/weka/weka-src/src/main" ./java/erenik/weka/WekaManager.java 

# ./java/	
./wm_run.sh
# D:/weka3-stable-3-8/weka


