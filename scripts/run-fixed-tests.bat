@echo off
echo ================================================================================
echo IAPD PROJECT - FIXED TESTS ONLY (Windows)
echo ================================================================================
echo.

REM Quick script to run only the fixed tests without full compilation
REM Assumes code is already compiled

REM Set up Java 11 paths
set JAVA_HOME=C:\Users\stoctom\AppData\Local\Programs\Microsoft\jdk-11.0.26.4-hotspot
set JAVA_CMD=%JAVA_HOME%\bin\java.exe

REM Set up runtime classpath
set RUNTIME_CP=lib/*;test-lib/*;src;src/test/java

echo Using Java 11 from: %JAVA_HOME%
echo Runtime classpath: %RUNTIME_CP%
echo.

REM Check if Java exists
if not exist "%JAVA_CMD%" (
    echo ERROR: Java 11 not found at %JAVA_CMD%
    echo Please verify the Java 11 installation path.
    pause
    exit /b 1
)

echo Running FixedTestRunner (Core Architecture Tests)...
echo ================================================================================
"%JAVA_CMD%" -Dfile.encoding=UTF-8 -cp "%RUNTIME_CP%" FixedTestRunner

echo.
echo Fixed tests execution completed.
pause
