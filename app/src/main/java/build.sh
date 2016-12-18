#!/bin/sh

mkdir out
javac ./erenik/evergreen/server/EGTCPServer.java -d out && (cd out && java erenik/evergreen/server/EGTCPServer)

