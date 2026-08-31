$ErrorActionPreference = 'Stop'
$toolsDir = Join-Path $PSScriptRoot ".tools"
$mavenDir = Join-Path $toolsDir "apache-maven-3.9.9"
$zipFile = Join-Path $PSScriptRoot "maven.zip"

if (-not (Test-Path $mavenDir)) {
    if (-not (Test-Path $toolsDir)) {
        New-Item -ItemType Directory -Path $toolsDir | Out-Null
    }
    Write-Host "Downloading Apache Maven 3.9.9..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" -OutFile $zipFile
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $zipFile -DestinationPath $toolsDir -Force
    Remove-Item $zipFile -Force
}

$mvnCmd = Join-Path $mavenDir "bin\mvn.cmd"
Write-Host "Maven executable is ready at: $mvnCmd"
& $mvnCmd -version
