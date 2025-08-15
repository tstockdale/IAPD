@echo off
echo Running IAPD...
echo.

REM Check if JAR exists
if not exist "target\iapd-1.0.0-SNAPSHOT-all.jar" (
    echo JAR file not found. Please build first using build.bat
    echo.
    pause
    exit /b 1
)

REM Run the application with all arguments passed through
java -jar "target\iapd-1.0.0-SNAPSHOT-all.jar" %*
