@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
echo =======================================================
echo   PROJECT N.E.X.U.S - BUILDING MAVEN CODEBASE (Windows)
echo   Team STI25CS - Java Multimodal Personal AI Assistant
echo =======================================================

if exist "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot" (
    set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
)
if exist "C:\Users\abhij\.gemini\antigravity-ide\scratch\tools\apache-maven-3.9.6" (
    set "MAVEN_HOME=C:\Users\abhij\.gemini\antigravity-ide\scratch\tools\apache-maven-3.9.6"
    set "PATH=!MAVEN_HOME!\bin;!PATH!"
)

echo Checking Java compiler...
javac -version
if %errorlevel% neq 0 (
    echo Error: JDK 21+ compiler ('javac') not found!
    echo Please install JDK 21 or set JAVA_HOME in your system environment.
    pause
    exit /b %errorlevel%
)

echo.
echo Installing Python deep learning dependencies...
python -m pip install -r requirements.txt 2>nul || py -m pip install -r requirements.txt 2>nul

echo.
echo Running Maven package...
call mvn clean package -DskipTests
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
