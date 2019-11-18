#!/bin/sh
# Also known as the 5-minute server
java -cp ./java/ evergreen.server.EGTCPServer -ais 0 -maxActivePlayers 100 -printStatusInterval 60 -secondsPerDay 300


