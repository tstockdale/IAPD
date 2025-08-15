@echo off
echo Building IAPD with Maven...
echo.

REM Clean and build the project
mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful!
    echo JAR file created: target\iapd-1.0.0-SNAPSHOT-all.jar
    echo.
    echo To run: java -jar target\iapd-1.0.0-SNAPSHOT-all.jar
) else (
    echo.
    echo Build failed!
    exit /b 1
)

pause
