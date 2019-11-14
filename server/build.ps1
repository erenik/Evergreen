# Powershell
$javaVersion = 8

Write-Host "Cleaning "
rm -r ./java/
Write-Host "Copying"
cp -r ../evergreen-game/src/main/java/ ./java/
Write-Host "Ripping out Android-specific files and code."
rm -r ./java/erenik/evergreen/android/
Write-Host "Building for Java SE version $javaVersion"
javac -cp ./java/ ./java/erenik/evergreen/server/EGTCPServer.java `
    --source $javaVersion `
    --target $javaVersion `
    -verbose

# ./run.sh



