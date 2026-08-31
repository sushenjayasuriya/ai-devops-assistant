@echo off
setlocal
set "DIR=%~dp0"
set "MAVEN_HOME=%DIR%.tools\apache-maven-3.9.9"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo [mvnw] Maven not found. Initializing toolchain...
    powershell -ExecutionPolicy Bypass -File "%DIR%setup_maven.ps1"
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
