#!/bin/bash

# IAPD Kubernetes Deployment Script
# This script deploys the IAPD application to Kubernetes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
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

# Function to check if kubectl is available
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    print_success "kubectl is available"
}

# Function to check if minikube is running
check_minikube() {
    if ! minikube status &> /dev/null; then
        print_error "Minikube is not running. Please start minikube first:"
        echo "  minikube start --memory=4096 --cpus=2"
        exit 1
    fi
    print_success "Minikube is running"
}

# Function to build Docker image
build_docker_image() {
    print_status "Building Docker image..."
    
    # Build the image
    docker build -t iapd:latest .
    
    # Load the image into minikube
    print_status "Loading image into minikube..."
    minikube image load iapd:latest
    
    print_success "Docker image built and loaded into minikube"
}

# Function to deploy Kubernetes resources
deploy_k8s_resources() {
    print_status "Deploying Kubernetes resources..."
    
    # Deploy in order
    print_status "Creating namespace..."
    kubectl apply -f k8s/namespace.yaml
    
    print_status "Creating persistent volume and claim..."
    kubectl apply -f k8s/persistent-volume.yaml
    
    # Fix permissions on the persistent volume directory
    print_status "Setting up persistent volume permissions..."
    minikube ssh "sudo mkdir -p /tmp/iapd-data && sudo chown -R 1000:1000 /tmp/iapd-data && sudo chmod -R 755 /tmp/iapd-data" || {
        print_warning "Failed to set permissions on persistent volume directory"
        print_warning "You may need to run: minikube ssh 'sudo chown -R 1000:1000 /tmp/iapd-data'"
    }
    print_success "Persistent volume permissions configured"
    
    print_status "Creating configuration..."
    kubectl apply -f k8s/configmap.yaml
    
    print_status "Creating job template..."
    kubectl apply -f k8s/job.yaml --dry-run=client -o yaml > /tmp/iapd-job-template.yaml
    print_success "Job template created (not deployed yet)"
    
    print_status "Creating cronjob (suspended)..."
    kubectl apply -f k8s/cronjob.yaml
    
    print_success "Core IAPD resources deployed"
}

# Function to deploy monitoring
deploy_monitoring() {
    print_status "Deploying monitoring stack..."
    
    print_status "Deploying Prometheus..."
    kubectl apply -f k8s/monitoring/prometheus.yaml
    
    print_status "Deploying Grafana..."
    kubectl apply -f k8s/monitoring/grafana.yaml
    
    print_success "Monitoring stack deployed"
    
    print_status "Waiting for monitoring services to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/prometheus -n monitoring
    kubectl wait --for=condition=available --timeout=300s deployment/grafana -n monitoring
    
    print_success "Monitoring services are ready"
}

# Function to show access information
show_access_info() {
    print_success "Deployment completed successfully!"
    echo
    print_status "Access Information:"
    echo "===================="
    
    # Get minikube IP
    MINIKUBE_IP=$(minikube ip)
    
    echo "Minikube IP: $MINIKUBE_IP"
    echo
    echo "Services:"
    echo "  - Prometheus: http://$MINIKUBE_IP:30000"
    echo "  - Grafana: http://$MINIKUBE_IP:30001 (admin/admin123)"
    echo
    echo "To run a job:"
    echo "  kubectl create job iapd-manual-job --from=cronjob/iapd-scheduled-job -n iapd"
    echo
    echo "To check job status:"
    echo "  kubectl get jobs -n iapd"
    echo "  kubectl get pods -n iapd"
    echo
    echo "To view job logs:"
    echo "  kubectl logs -f job/iapd-manual-job -n iapd"
    echo
    echo "To enable scheduled jobs:"
    echo "  kubectl patch cronjob iapd-scheduled-job -n iapd -p '{\"spec\":{\"suspend\":false}}'"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo
    echo "Options:"
    echo "  --skip-build     Skip Docker image build"
    echo "  --skip-monitoring Skip monitoring deployment"
    echo "  --help           Show this help message"
    echo
    echo "Examples:"
    echo "  $0                    # Full deployment"
    echo "  $0 --skip-build       # Deploy without rebuilding image"
    echo "  $0 --skip-monitoring  # Deploy without monitoring"
}

# Main deployment function
main() {
    local skip_build=false
    local skip_monitoring=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-build)
                skip_build=true
                shift
                ;;
            --skip-monitoring)
                skip_monitoring=true
                shift
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    print_status "Starting IAPD Kubernetes deployment..."
    
    # Pre-flight checks
    check_kubectl
    check_minikube
    
    # Build Docker image if not skipped
    if [ "$skip_build" = false ]; then
        build_docker_image
    else
        print_warning "Skipping Docker image build"
    fi
    
    # Deploy Kubernetes resources
    deploy_k8s_resources
    
    # Deploy monitoring if not skipped
    if [ "$skip_monitoring" = false ]; then
        deploy_monitoring
    else
        print_warning "Skipping monitoring deployment"
    fi
    
    # Show access information
    show_access_info
}

# Run main function with all arguments
main "$@"
