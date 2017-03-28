#!/bin/sh
echo Cleaning 
rm -r -f ./java/
echo Copying
cp -r ../app/src/main/java/ ./java/
echo Ripping out Android-specific files and code.
rm -r ./java/erenik/evergreen/android/
echo Building
javac -cp ./java/ ./java/erenik/evergreen/server/EGTCPServer.java

./run.sh



