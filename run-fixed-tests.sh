#!/bin/bash

echo "================================================================================"
echo "IAPD PROJECT - FIXED TESTS ONLY (Linux/Mac)"
echo "================================================================================"
echo

# Quick script to run only the fixed tests without full compilation
# Assumes code is already compiled

# Set up runtime classpath
RUNTIME_CP="lib/*:test-lib/*:src:src/test/java"

echo "Runtime classpath: $RUNTIME_CP"
echo

echo "Running FixedTestRunner (Core Architecture Tests)..."
echo "================================================================================"
java -Dfile.encoding=UTF-8 -cp "$RUNTIME_CP" FixedTestRunner

echo
echo "Fixed tests execution completed."
