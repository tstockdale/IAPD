#!/bin/bash

# Script to copy IAPD data from Kubernetes to local machine
# This script helps you access the processed data from the IAPD jobs

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "IAPD Data Copy Script"
    echo "===================="
    echo
    echo "This script helps you copy processed data from Kubernetes to your local machine."
    echo
    echo "Usage: $0 [output-directory]"
    echo
    echo "Arguments:"
    echo "  output-directory    Local directory to copy data to (default: ~/IAPD-Output)"
    echo
    echo "Examples:"
    echo "  $0                           # Copy to ~/IAPD-Output"
    echo "  $0 /path/to/my/data         # Copy to custom directory"
}

# Function to copy data
copy_data() {
    local output_dir="${1:-$HOME/IAPD-Output}"
    
    print_status "Copying IAPD data from Kubernetes to: $output_dir"
    
    # Create output directory
    mkdir -p "$output_dir"
    
    # Check if minikube is running
    if ! minikube status &> /dev/null; then
        print_error "Minikube is not running"
        exit 1
    fi
    
    print_status "Checking available data files..."
    
    # List available files
    echo "Available data files in Kubernetes:"
    minikube ssh "find /tmp/iapd-data -name '*.csv' -o -name '*.pdf' -o -name '*.xml' | sort"
    echo
    
    # Copy CSV files (these are the main output files)
    print_status "Copying CSV files..."
    minikube ssh "find /tmp/iapd-data -name '*.csv'" | while read -r file; do
        if [ -n "$file" ]; then
            local filename=$(basename "$file")
            print_status "Copying $filename..."
            minikube ssh "cat '$file'" > "$output_dir/$filename" 2>/dev/null || {
                print_error "Failed to copy $filename"
            }
        fi
    done
    
    # Copy XML files
    print_status "Copying XML files..."
    minikube ssh "find /tmp/iapd-data -name '*.xml'" | while read -r file; do
        if [ -n "$file" ]; then
            local filename=$(basename "$file")
            print_status "Copying $filename..."
            minikube ssh "cat '$file'" > "$output_dir/$filename" 2>/dev/null || {
                print_error "Failed to copy $filename"
            }
        fi
    done
    
    # Show what was copied
    print_success "Data copy completed!"
    echo
    print_status "Files copied to $output_dir:"
    ls -la "$output_dir" 2>/dev/null || echo "No files found"
    
    echo
    print_status "Data Location Summary:"
    echo "======================"
    echo "Container path: /app/Data"
    echo "Minikube path:  /tmp/iapd-data"
    echo "Local path:     $output_dir"
    echo
    echo "To access data directly in minikube:"
    echo "  minikube ssh"
    echo "  ls -la /tmp/iapd-data/Data/"
}

# Main function
main() {
    case "${1:-}" in
        --help|-h|help)
            show_usage
            exit 0
            ;;
        *)
            copy_data "$1"
            ;;
    esac
}

# Run main function
main "$@"
