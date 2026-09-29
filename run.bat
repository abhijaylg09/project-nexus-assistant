@echo off
setlocal enabledelayedexpansion
title PROJECT N.E.X.U.S - Personal Real-Time AI Assistant

echo ================================================================
echo       PROJECT N.E.X.U.S (Neural EXecutive User System)
echo    Centralized Java Orchestrator - Team STI25CS Assistant
echo ================================================================

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
set "MAVEN_HOME=C:\Users\abhij\.gemini\antigravity-ide\scratch\tools\apache-maven-3.9.6"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

if exist "target\nexus-ai-assistant-1.0.0.jar" (
    echo [N.E.X.U.S] Launching packaged executable JAR...
    java -jar "target\nexus-ai-assistant-1.0.0.jar"
) else (
    echo [N.E.X.U.S] Standalone JAR not found. Launching via Maven JavaFX runner...
    call mvn javafx:run
)

if %errorlevel% neq 0 (
    echo.
    echo Launch terminated with exit code %errorlevel%.
    pause
)
