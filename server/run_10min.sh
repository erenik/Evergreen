#!/bin/sh
# Also known as the 5-minute server
java -cp ./java/ erenik.evergreen.server.EGTCPServer -ais 0 -maxActivePlayers 100 -printStatusInterval 120 -secondsPerDay 600


