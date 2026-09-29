@echo off
setlocal
echo =======================================================
echo   PROJECT N.E.X.U.S - BUILDING MAVEN CODEBASE
echo   Team STI25CS - Java Multimodal Personal AI Assistant
echo =======================================================

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
set "MAVEN_HOME=C:\Users\abhij\.gemini\antigravity-ide\scratch\tools\apache-maven-3.9.6"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo Checking Java compiler...
javac -version
if %errorlevel% neq 0 (
    echo Error: JDK 21 compiler not found!
    pause
    exit /b %errorlevel%
)

echo.
echo Running Maven compile...
call mvn clean compile -DskipTests
if %errorlevel% neq 0 (
    echo Build failed! Check compiler logs.
    pause
    exit /b %errorlevel%
)

echo.
echo =======================================================
echo   BUILD COMPLETED SUCCESSFULLY!
echo   To launch N.E.X.U.S, execute: run.bat
echo =======================================================
pause
