#!/bin/sh

mkdir out
javac -cp json-20160810.jar ./erenik/evergreen/server/EGTCPServer.java -d out && (cd out && java erenik/evergreen/server/EGTCPServer)

