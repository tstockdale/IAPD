#!/bin/bash

# IAPD Job Management Script
# This script helps manage IAPD jobs in Kubernetes

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

# Function to show usage
show_usage() {
    echo "IAPD Job Management Script"
    echo "=========================="
    echo
    echo "Usage: $0 <command> [options]"
    echo
    echo "Commands:"
    echo "  run [job-name] [args...]     Run a new job with optional arguments"
    echo "  list                         List all jobs"
    echo "  status [job-name]            Show status of a specific job"
    echo "  logs [job-name]              Show logs of a job"
    echo "  delete [job-name]            Delete a job"
    echo "  cleanup                      Clean up completed jobs"
    echo "  enable-schedule              Enable scheduled jobs"
    echo "  disable-schedule             Disable scheduled jobs"
    echo "  schedule-status              Show schedule status"
    echo
    echo "Examples:"
    echo "  $0 run                                    # Run job with default args"
    echo "  $0 run my-job --index-limit 100          # Run job with custom args"
    echo "  $0 list                                   # List all jobs"
    echo "  $0 logs my-job                            # Show job logs"
    echo "  $0 cleanup                                # Clean up completed jobs"
}

# Function to run a new job
run_job() {
    local job_name="${1:-iapd-manual-$(date +%Y%m%d-%H%M%S)}"
    shift || true
    local args="$*"
    
    print_status "Creating job: $job_name"
    
    if [ -n "$args" ]; then
        print_status "Job arguments: $args"
        
        # Create a temporary file for the job YAML
        local temp_file=$(mktemp)
        
        # Build the args array for YAML
        local args_yaml=""
        for arg in $args; do
            args_yaml="${args_yaml}          - \"$arg\""$'\n'
        done
        args_yaml="${args_yaml%$'\n'}"  # Remove trailing newline
        
        # Create the job YAML directly with custom arguments
        cat > "$temp_file" << EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: $job_name
  namespace: iapd
  labels:
    app: investment-adviser-parser
    component: batch-processor
spec:
  ttlSecondsAfterFinished: 86400
  backoffLimit: 3
  template:
    metadata:
      labels:
        app: investment-adviser-parser
        component: batch-processor
    spec:
      restartPolicy: Never
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
      containers:
      - name: iapd-parser
        image: iapd:latest
        imagePullPolicy: Never
        envFrom:
        - configMapRef:
            name: iapd-config
        env:
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        volumeMounts:
        - name: iapd-data
          mountPath: /app/Data
        workingDir: /app
        args:
$args_yaml
        startupProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - "test -f /app/iapd.jar"
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 5
          failureThreshold: 3
      volumes:
      - name: iapd-data
        persistentVolumeClaim:
          claimName: iapd-data-pvc
EOF
        
        # Apply the job
        kubectl apply -f "$temp_file"
        
        # Clean up
        rm -f "$temp_file"
        
        print_success "Job '$job_name' created with custom arguments"
    else
        # Create job with default arguments
        kubectl create job "$job_name" --from=cronjob/iapd-scheduled-job -n iapd
        print_success "Job '$job_name' created with default arguments"
    fi
    
    print_status "To monitor the job:"
    echo "  kubectl get jobs -n iapd"
    echo "  kubectl logs -f job/$job_name -n iapd"
}

# Function to list jobs
list_jobs() {
    print_status "Listing all IAPD jobs:"
    echo
    kubectl get jobs -n iapd -o wide
    echo
    print_status "Job pods:"
    kubectl get pods -n iapd -l app=investment-adviser-parser
}

# Function to show job status
show_job_status() {
    local job_name="$1"
    
    if [ -z "$job_name" ]; then
        print_error "Job name is required"
        echo "Usage: $0 status <job-name>"
        exit 1
    fi
    
    print_status "Status for job: $job_name"
    echo
    kubectl describe job "$job_name" -n iapd
}

# Function to show job logs
show_job_logs() {
    local job_name="$1"
    
    if [ -z "$job_name" ]; then
        print_error "Job name is required"
        echo "Usage: $0 logs <job-name>"
        exit 1
    fi
    
    print_status "Logs for job: $job_name"
    echo
    kubectl logs -f job/"$job_name" -n iapd
}

# Function to delete a job
delete_job() {
    local job_name="$1"
    
    if [ -z "$job_name" ]; then
        print_error "Job name is required"
        echo "Usage: $0 delete <job-name>"
        exit 1
    fi
    
    print_status "Deleting job: $job_name"
    kubectl delete job "$job_name" -n iapd
    print_success "Job '$job_name' deleted"
}

# Function to cleanup completed jobs
cleanup_jobs() {
    print_status "Cleaning up completed jobs..."
    
    # Delete completed jobs (both successful and failed)
    kubectl delete jobs -n iapd --field-selector status.successful=1
    kubectl delete jobs -n iapd --field-selector status.failed=1
    
    print_success "Completed jobs cleaned up"
}

# Function to enable scheduled jobs
enable_schedule() {
    print_status "Enabling scheduled jobs..."
    kubectl patch cronjob iapd-scheduled-job -n iapd -p '{"spec":{"suspend":false}}'
    print_success "Scheduled jobs enabled"
    show_schedule_status
}

# Function to disable scheduled jobs
disable_schedule() {
    print_status "Disabling scheduled jobs..."
    kubectl patch cronjob iapd-scheduled-job -n iapd -p '{"spec":{"suspend":true}}'
    print_success "Scheduled jobs disabled"
    show_schedule_status
}

# Function to show schedule status
show_schedule_status() {
    print_status "Schedule status:"
    echo
    kubectl get cronjob iapd-scheduled-job -n iapd -o wide
}

# Main function
main() {
    if [ $# -eq 0 ]; then
        show_usage
        exit 1
    fi
    
    local command="$1"
    shift
    
    case "$command" in
        run)
            run_job "$@"
            ;;
        list)
            list_jobs
            ;;
        status)
            show_job_status "$1"
            ;;
        logs)
            show_job_logs "$1"
            ;;
        delete)
            delete_job "$1"
            ;;
        cleanup)
            cleanup_jobs
            ;;
        enable-schedule)
            enable_schedule
            ;;
        disable-schedule)
            disable_schedule
            ;;
        schedule-status)
            show_schedule_status
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            print_error "Unknown command: $command"
            echo
            show_usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
