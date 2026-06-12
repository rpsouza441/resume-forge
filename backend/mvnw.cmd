@REM Maven Wrapper startup script for Windows
@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip"

if exist "%WRAPPER_JAR%" goto execute

echo Downloading Maven Wrapper...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper-dist.zip' -UseBasicParsing"

echo Extracting Maven...
powershell -Command "Expand-Archive -Path '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper-dist.zip' -DestinationPath '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\temp' -Force; Move-Item -Path '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\temp\apache-maven-3.9.6\*' -Destination '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven' -Force; Remove-Item -Path '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper-dist.zip' -Force; Remove-Item -Path '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\temp' -Recurse -Force"

set "MAVEN_HOME=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

:execute
"%MAVEN_HOME%\bin\mvn.cmd" %*