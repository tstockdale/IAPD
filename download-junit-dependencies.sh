#!/bin/bash

echo "================================================================================"
echo "DOWNLOADING JUNIT 5 DEPENDENCIES (Linux/Mac)"
echo "================================================================================"
echo

# Create test-lib directory if it doesn't exist
if [ ! -d "test-lib" ]; then
    echo "Creating test-lib directory..."
    mkdir -p test-lib
fi

echo "Downloading JUnit 5 JAR files..."
echo "This may take a few minutes depending on your internet connection."
echo

# Function to download with error handling
download_jar() {
    local url=$1
    local filename=$2
    local description=$3
    
    echo "[$4/8] Downloading $filename..."
    if curl -L -o "test-lib/$filename" "$url" --silent --show-error; then
        echo "SUCCESS: $description downloaded"
    else
        echo "ERROR: Failed to download $description"
        return 1
    fi
}

# Download all JUnit 5 dependencies
download_jar "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.1/junit-jupiter-api-5.10.1.jar" \
    "junit-jupiter-api-5.10.1.jar" "junit-jupiter-api" "1"

download_jar "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.10.1/junit-jupiter-engine-5.10.1.jar" \
    "junit-jupiter-engine-5.10.1.jar" "junit-jupiter-engine" "2"

download_jar "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-params/5.10.1/junit-jupiter-params-5.10.1.jar" \
    "junit-jupiter-params-5.10.1.jar" "junit-jupiter-params" "3"

download_jar "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-launcher/1.10.1/junit-platform-launcher-1.10.1.jar" \
    "junit-platform-launcher-1.10.1.jar" "junit-platform-launcher" "4"

download_jar "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-engine/1.10.1/junit-platform-engine-1.10.1.jar" \
    "junit-platform-engine-1.10.1.jar" "junit-platform-engine" "5"

download_jar "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-commons/1.10.1/junit-platform-commons-1.10.1.jar" \
    "junit-platform-commons-1.10.1.jar" "junit-platform-commons" "6"

download_jar "https://repo1.maven.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar" \
    "apiguardian-api-1.1.2.jar" "apiguardian-api" "7"

download_jar "https://repo1.maven.org/maven2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar" \
    "opentest4j-1.3.0.jar" "opentest4j" "8"

echo
echo "================================================================================"
echo "DOWNLOAD SUMMARY"
echo "================================================================================"

# Check which files were downloaded successfully
echo "Checking downloaded files:"
check_file() {
    if [ -f "test-lib/$1" ]; then
        echo "✓ $1"
    else
        echo "✗ $1 - MISSING"
    fi
}

check_file "junit-jupiter-api-5.10.1.jar"
check_file "junit-jupiter-engine-5.10.1.jar"
check_file "junit-jupiter-params-5.10.1.jar"
check_file "junit-platform-launcher-1.10.1.jar"
check_file "junit-platform-engine-1.10.1.jar"
check_file "junit-platform-commons-1.10.1.jar"
check_file "apiguardian-api-1.1.2.jar"
check_file "opentest4j-1.3.0.jar"

echo
echo "All JUnit 5 dependencies have been downloaded to the test-lib directory."
echo "You can now run the tests using: ./run-tests.sh"
echo
echo "================================================================================"
