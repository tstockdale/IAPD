#!/bin/bash

# IAPD Docker Runner Script
# This script provides convenient commands to run the IAPD Docker container

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker Desktop and try again."
        exit 1
    fi
}

# Function to check if data directory exists
check_data_directory() {
    if [ ! -d "$HOME/Work/IAPD" ]; then
        print_warning "Data directory $HOME/Work/IAPD does not exist."
        read -p "Create it now? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            mkdir -p "$HOME/Work/IAPD"/{Downloads,Output,Input,FirmFiles,Logs}
            print_success "Created data directory structure at $HOME/Work/IAPD"
        else
            print_error "Data directory is required. Exiting."
            exit 1
        fi
    fi
}

# Function to build the Docker image
build_image() {
    print_info "Building IAPD Docker image..."
    docker build -t iapd:latest .
    print_success "Docker image built successfully!"
}

# Function to run IAPD with arguments
run_iapd() {
    check_docker
    check_data_directory
    
    print_info "Running IAPD container with arguments: $*"
    docker run --rm -v "$HOME/Work/IAPD:/app/Data" iapd:latest "$@"
}

# Function to run IAPD interactively
run_interactive() {
    check_docker
    check_data_directory
    
    print_info "Starting interactive IAPD container..."
    docker run -it --rm -v "$HOME/Work/IAPD:/app/Data" --entrypoint /bin/bash iapd:latest
}

# Function to show usage
show_usage() {
    echo "IAPD Docker Runner"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  build                 Build the Docker image"
    echo "  run [args...]         Run IAPD with specified arguments"
    echo "  interactive           Start an interactive shell in the container"
    echo "  help                  Show IAPD application help"
    echo "  examples              Show usage examples"
    echo ""
    echo "Examples:"
    echo "  $0 build"
    echo "  $0 run --index-limit 1000 --verbose"
    echo "  $0 run --incremental --baseline-file /app/Data/Output/IAPD_Data.csv"
    echo "  $0 interactive"
}

# Function to show examples
show_examples() {
    echo "IAPD Docker Usage Examples:"
    echo ""
    echo "1. Basic processing:"
    echo "   $0 run"
    echo ""
    echo "2. Limited processing with verbose output:"
    echo "   $0 run --index-limit 100 --verbose"
    echo ""
    echo "3. Resume downloads:"
    echo "   $0 run --resume-downloads"
    echo ""
    echo "4. Incremental processing:"
    echo "   $0 run --incremental --baseline-file /app/Data/Output/IAPD_Data.csv"
    echo ""
    echo "5. Monthly incremental updates:"
    echo "   $0 run --incremental --month january"
    echo ""
    echo "6. Custom memory settings:"
    echo "   docker run -e JAVA_OPTS='-Xmx8g -Xms2g' -v ~/Work/IAPD:/app/Data iapd:latest --index-limit 5000"
    echo ""
    echo "7. Rate limiting:"
    echo "   $0 run --url-rate 2 --download-rate 5"
}

# Main script logic
case "${1:-}" in
    build)
        build_image
        ;;
    run)
        shift
        run_iapd "$@"
        ;;
    interactive)
        run_interactive
        ;;
    help)
        run_iapd --help
        ;;
    examples)
        show_examples
        ;;
    *)
        show_usage
        ;;
esac
