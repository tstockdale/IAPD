@echo off
echo ================================================================================
echo IAPD PROJECT - JUNIT TEST EXECUTION (Windows)
echo ================================================================================
echo.

REM Check if bin directory exists, create if not
if not exist "bin" (
    echo Creating bin directory...
    mkdir bin
)

REM Check if test-lib directory exists
if not exist "test-lib" (
    echo ERROR: test-lib directory not found!
    echo Please create test-lib directory and add JUnit 5 JAR files.
    echo See JUNIT_SETUP_GUIDE.md for detailed instructions.
    pause
    exit /b 1
)

REM Check if JUnit JARs exist
if not exist "test-lib\junit-jupiter-api-*.jar" (
    echo ERROR: JUnit JAR files not found in test-lib directory!
    echo Please download and add JUnit 5 JAR files to test-lib directory.
    echo See JUNIT_SETUP_GUIDE.md for download links and instructions.
    pause
    exit /b 1
)

REM Set up classpath variables for clarity
set PRODUCTION_CP=lib/*
set TEST_CP=lib/*;test-lib/*;bin
set RUNTIME_CP=lib/*;test-lib/*;bin

echo Production classpath: %PRODUCTION_CP%
echo Test classpath: %TEST_CP%
echo Runtime classpath: %RUNTIME_CP%
echo.

echo Compiling production code...
javac -cp "%PRODUCTION_CP%" -d bin src/*.java
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to compile production code!
    echo Check that all production dependencies are in lib/ directory
    pause
    exit /b 1
)

echo Compiling test code...
javac -cp "%TEST_CP%" -d bin src/test/java/*.java
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to compile test code!
    echo Make sure all JUnit 5 JAR files are in test-lib directory.
    echo Current test classpath: %TEST_CP%
    pause
    exit /b 1
)

echo.
echo Running JUnit tests...
echo Runtime classpath: %RUNTIME_CP%
echo ================================================================================
java -cp "%RUNTIME_CP%" TestRunner

echo.
echo Test execution completed.
pause
