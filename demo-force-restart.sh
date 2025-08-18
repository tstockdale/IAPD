#!/bin/bash

echo "=== Force Restart Functionality Demo ==="
echo

# Create a sample Data directory with some content
echo "1. Creating sample Data directory with test content..."
mkdir -p Data/TestSubDir
echo "Sample content for testing" > Data/test.txt
echo "More test data" > Data/TestSubDir/nested.txt
ls -la Data/
echo

# Show the current timestamp
echo "2. Current time: $(date)"
echo

# Run the application with --force-restart (this will rename the Data directory)
echo "3. Running application with --force-restart option..."
echo "   Command: mvn exec:java -Dexec.mainClass=\"com.iss.iapd.core.IAFirmSECParserRefactored\" -Dexec.args=\"--force-restart --index-limit 1 --help\""
echo

# Note: We add --help to exit early after showing the force restart functionality
mvn exec:java -Dexec.mainClass="com.iss.iapd.core.IAFirmSECParserRefactored" -Dexec.args="--force-restart --index-limit 1 --help" -q 2>/dev/null

echo
echo "4. Checking directory structure after force restart..."
echo "   Looking for backup directories with timestamp..."
ls -la | grep "Data"
echo

# Clean up
echo "5. Cleaning up demo directories..."
rm -rf Data*
echo "Demo completed!"
