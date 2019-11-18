#!/bin/sh
# Also known as the primary 24-hour/1-day server
java -cp ./java/ evergreen.server.EGTCPServer -ais 0 -maxActivePlayers 100 -printStatusInterval 300 -secondsPerDay 86400


