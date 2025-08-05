#!/bin/bash

echo "================================================================================"
echo "IAPD PROJECT - JUNIT TEST EXECUTION (Linux/Mac)"
echo "================================================================================"
echo

# Check if bin directory exists, create if not
if [ ! -d "bin" ]; then
    echo "Creating bin directory..."
    mkdir -p bin
fi

# Check if test-lib directory exists
if [ ! -d "test-lib" ]; then
    echo "ERROR: test-lib directory not found!"
    echo "Please create test-lib directory and add JUnit 5 JAR files."
    echo "See JUNIT_SETUP_GUIDE.md for detailed instructions."
    exit 1
fi

# Check if JUnit JARs exist
if ! ls test-lib/junit-jupiter-api-*.jar 1> /dev/null 2>&1; then
    echo "ERROR: JUnit JAR files not found in test-lib directory!"
    echo "Please download and add JUnit 5 JAR files to test-lib directory."
    echo "See JUNIT_SETUP_GUIDE.md for download links and instructions."
    exit 1
fi

# Set up classpath variables for clarity
PRODUCTION_CP="lib/*"
TEST_CP="lib/*:test-lib/*:bin"
RUNTIME_CP="lib/*:test-lib/*:bin"

echo "Production classpath: $PRODUCTION_CP"
echo "Test classpath: $TEST_CP"
echo "Runtime classpath: $RUNTIME_CP"
echo

echo "Compiling production code..."
javac -cp "$PRODUCTION_CP" -d bin src/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to compile production code!"
    echo "Check that all production dependencies are in lib/ directory"
    exit 1
fi

echo "Compiling test code..."
javac -cp "$TEST_CP" -d bin src/test/java/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to compile test code!"
    echo "Make sure all JUnit 5 JAR files are in test-lib directory."
    echo "Current test classpath: $TEST_CP"
    exit 1
fi

echo
echo "Running JUnit tests..."
echo "Runtime classpath: $RUNTIME_CP"
echo "================================================================================"
java -cp "$RUNTIME_CP" TestRunner

echo
echo "Test execution completed."
