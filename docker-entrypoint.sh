#!/bin/bash

# Docker entrypoint script for IAPD (Investment Adviser Public Disclosure Parser)
# This script handles command line arguments and Java execution

set -e

# Function to print usage information
print_usage() {
    echo "IAPD Docker Container - Investment Adviser Public Disclosure Parser"
    echo ""
    echo "Usage: docker run [docker-options] iapd:latest [IAPD-options]"
    echo ""
    echo "Environment Variables:"
    echo "  JAVA_OPTS          Java JVM options (default: -Xmx2g -Xms512m -XX:+UseG1GC -XX:+UseStringDeduplication)"
    echo "  LOG_LEVEL          Logging level (default: INFO)"
    echo ""
    echo "Volume Mounts:"
    echo "  /app/Data          Mount your ~/Work/IAPD directory here"
    echo ""
    echo "Examples:"
    echo "  # Run with default settings"
    echo "  docker run -v ~/Work/IAPD:/app/Data iapd:latest"
    echo ""
    echo "  # Run with custom memory settings"
    echo "  docker run -e JAVA_OPTS='-Xmx4g -Xms1g' -v ~/Work/IAPD:/app/Data iapd:latest"
    echo ""
    echo "  # Run with command line arguments"
    echo "  docker run -v ~/Work/IAPD:/app/Data iapd:latest --index-limit 1000 --verbose"
    echo ""
    echo "  # Run in incremental mode"
    echo "  docker run -v ~/Work/IAPD:/app/Data iapd:latest --incremental --baseline-file /app/Data/Output/IAPD_Data.csv"
    echo ""
    echo "For IAPD-specific options, run:"
    echo "  docker run -v ~/Work/IAPD:/app/Data iapd:latest --help"
}

# Function to check if Data directory is properly mounted
check_data_mount() {
    if [ ! -d "/app/Data" ]; then
        echo "ERROR: Data directory not found at /app/Data"
        echo "Please mount your data directory using: -v ~/Work/IAPD:/app/Data"
        exit 1
    fi
    
    # Create subdirectories if they don't exist
    mkdir -p /app/Data/Downloads
    mkdir -p /app/Data/Output
    mkdir -p /app/Data/Input
    mkdir -p /app/Data/FirmFiles
    mkdir -p /app/Data/Logs
    
    # Check if the mount is writable
    if [ ! -w "/app/Data" ]; then
        echo "WARNING: Data directory /app/Data is not writable"
        echo "This may cause issues with file creation and logging"
    fi
}

# Function to setup logging
setup_logging() {
    # Set log path system property for the application
    export LOG_PATH="/app/Data/Logs"
    
    # Ensure log directory exists and is writable
    mkdir -p "$LOG_PATH"
    
    echo "Log directory: $LOG_PATH"
    echo "Java options: $JAVA_OPTS"
}

# Function to handle signals for graceful shutdown
cleanup() {
    echo ""
    echo "Received shutdown signal. Cleaning up..."
    # Kill the Java process if it's running
    if [ ! -z "$JAVA_PID" ]; then
        echo "Stopping IAPD process (PID: $JAVA_PID)..."
        kill -TERM "$JAVA_PID" 2>/dev/null || true
        wait "$JAVA_PID" 2>/dev/null || true
    fi
    echo "Cleanup completed."
    exit 0
}

# Set up signal handlers
trap cleanup SIGTERM SIGINT

# Main execution
main() {
    echo "Starting IAPD Docker Container..."
    echo "Working directory: $(pwd)"
    echo "User: $(whoami)"
    echo "Java version:"
    java -version
    echo ""
    
    # Check for help flags
    for arg in "$@"; do
        case $arg in
            --help|-h|help)
                print_usage
                echo ""
                echo "=== IAPD Application Help ==="
                java $JAVA_OPTS -jar iapd.jar --help
                exit 0
                ;;
        esac
    done
    
    # Check if Data directory is properly mounted
    check_data_mount
    
    # Setup logging
    setup_logging
    
    # Print startup information
    echo "=== IAPD Container Configuration ==="
    echo "Container working directory: $(pwd)"
    echo "Data directory: /app/Data"
    echo "Data directory contents:"
    ls -la /app/Data/ || echo "Data directory is empty or not accessible"
    echo ""
    echo "Java options: $JAVA_OPTS"
    echo "Command line arguments: $@"
    echo ""
    
    # Change to the Data directory for execution (matches the application's expectation)
    cd /app/Data
    
    echo "=== Starting IAPD Application ==="
    echo "Executing: java $JAVA_OPTS -jar /app/iapd.jar $@"
    echo ""
    
    # Execute the Java application with all passed arguments
    java $JAVA_OPTS -jar /app/iapd.jar "$@" &
    JAVA_PID=$!
    
    # Wait for the Java process to complete
    wait $JAVA_PID
    JAVA_EXIT_CODE=$?
    
    echo ""
    echo "=== IAPD Application Completed ==="
    echo "Exit code: $JAVA_EXIT_CODE"
    
    # Return the same exit code as the Java application
    exit $JAVA_EXIT_CODE
}

# Execute main function with all arguments
main "$@"
