Write-Host "Running server"
java -cp ./java/ erenik.evergreen.server.EGTCPServer `
    -ais 0 `
    -maxActivePlayers 100 `
    -printStatusInterval 60 `
    -verbose
