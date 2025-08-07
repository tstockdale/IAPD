import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unified processing context that contains both configuration and runtime state
 * for the IAPD processing application. Thread-safe for concurrent access.
 */
public class ProcessingContext {
    
    // Configuration (immutable after creation)
    private final int indexLimit;
    private final boolean verbose;
    private final String outputFormat;
    private final int retryCount;
    private final boolean skipBrochureDownload;
    private final String configSource;
    private final LocalDateTime createdAt;
    
    // Runtime State (mutable, thread-safe)
    private final AtomicInteger processedFirms = new AtomicInteger(0);
    private final AtomicInteger successfulDownloads = new AtomicInteger(0);
    private final AtomicInteger failedDownloads = new AtomicInteger(0);
    private final AtomicInteger brochuresProcessed = new AtomicInteger(0);
    private final AtomicLong processingStartTime = new AtomicLong(0);
    private volatile String currentProcessingFile;
    private volatile ProcessingPhase currentPhase = ProcessingPhase.INITIALIZATION;
    private volatile String lastError;
    
    // Private constructor - use Builder
    private ProcessingContext(Builder builder) {
        this.indexLimit = builder.indexLimit;
        this.verbose = builder.verbose;
        this.outputFormat = builder.outputFormat;
        this.retryCount = builder.retryCount;
        this.skipBrochureDownload = builder.skipBrochureDownload;
        this.configSource = builder.configSource;
        this.createdAt = LocalDateTime.now();
        this.processingStartTime.set(System.currentTimeMillis());
    }
    
    // Configuration getters
    public int getIndexLimit() { return indexLimit; }
    public boolean isVerbose() { return verbose; }
    public String getOutputFormat() { return outputFormat; }
    public int getRetryCount() { return retryCount; }
    public boolean isSkipBrochureDownload() { return skipBrochureDownload; }
    public String getConfigSource() { return configSource; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // Runtime state getters
    public int getProcessedFirms() { return processedFirms.get(); }
    public int getSuccessfulDownloads() { return successfulDownloads.get(); }
    public int getFailedDownloads() { return failedDownloads.get(); }
    public int getBrochuresProcessed() { return brochuresProcessed.get(); }
    public String getCurrentProcessingFile() { return currentProcessingFile; }
    public ProcessingPhase getCurrentPhase() { return currentPhase; }
    public String getLastError() { return lastError; }
    public long getProcessingStartTime() { return processingStartTime.get(); }
    
    // Runtime state setters (thread-safe)
    public void incrementProcessedFirms() { processedFirms.incrementAndGet(); }
    public void incrementSuccessfulDownloads() { successfulDownloads.incrementAndGet(); }
    public void incrementFailedDownloads() { failedDownloads.incrementAndGet(); }
    public void incrementBrochuresProcessed() { brochuresProcessed.incrementAndGet(); }
    public void setCurrentProcessingFile(String filename) { this.currentProcessingFile = filename; }
    public void setCurrentPhase(ProcessingPhase phase) { this.currentPhase = phase; }
    public void setLastError(String error) { this.lastError = error; }
    
    // Utility methods
    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - processingStartTime.get();
    }
    
    public double getProcessingRate() {
        long elapsed = getElapsedTimeMs();
        if (elapsed == 0) return 0.0;
        return (double) processedFirms.get() / (elapsed / 1000.0); // firms per second
    }
    
    public boolean hasReachedIndexLimit() {
        return processedFirms.get() >= indexLimit;
    }
    
    public void logCurrentState() {
        if (verbose) {
            System.out.println("=== Processing Context State ===");
            System.out.println("Phase: " + currentPhase);
            System.out.println("Current File: " + (currentProcessingFile != null ? currentProcessingFile : "None"));
            System.out.println("Processed Firms: " + processedFirms.get() + "/" + (indexLimit == Integer.MAX_VALUE ? "unlimited" : indexLimit));
            System.out.println("Successful Downloads: " + successfulDownloads.get());
            System.out.println("Failed Downloads: " + failedDownloads.get());
            System.out.println("Brochures Processed: " + brochuresProcessed.get());
            System.out.println("Elapsed Time: " + (getElapsedTimeMs() / 1000.0) + " seconds");
            System.out.println("Processing Rate: " + String.format("%.2f", getProcessingRate()) + " firms/sec");
            if (lastError != null) {
                System.out.println("Last Error: " + lastError);
            }
            System.out.println("================================");
        }
    }
    
    @Override
    public String toString() {
        return "ProcessingContext{" +
                "indexLimit=" + indexLimit +
                ", verbose=" + verbose +
                ", outputFormat='" + outputFormat + '\'' +
                ", retryCount=" + retryCount +
                ", skipBrochureDownload=" + skipBrochureDownload +
                ", configSource='" + configSource + '\'' +
                ", currentPhase=" + currentPhase +
                ", processedFirms=" + processedFirms.get() +
                ", successfulDownloads=" + successfulDownloads.get() +
                ", failedDownloads=" + failedDownloads.get() +
                '}';
    }
    
    /**
     * Builder pattern for flexible ProcessingContext construction
     */
    public static class Builder {
        private int indexLimit = Integer.MAX_VALUE;
        private boolean verbose = false;
        private String outputFormat = "CSV";
        private int retryCount = 3;
        private boolean skipBrochureDownload = false;
        private String configSource = "default";
        
        public Builder indexLimit(int indexLimit) {
            if (indexLimit <= 0) {
                throw new IllegalArgumentException("Index limit must be positive");
            }
            this.indexLimit = indexLimit;
            return this;
        }
        
        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }
        
        public Builder outputFormat(String outputFormat) {
            if (outputFormat == null || outputFormat.trim().isEmpty()) {
                throw new IllegalArgumentException("Output format cannot be null or empty");
            }
            this.outputFormat = outputFormat;
            return this;
        }
        
        public Builder retryCount(int retryCount) {
            if (retryCount < 0) {
                throw new IllegalArgumentException("Retry count cannot be negative");
            }
            this.retryCount = retryCount;
            return this;
        }
        
        public Builder skipBrochureDownload(boolean skipBrochureDownload) {
            this.skipBrochureDownload = skipBrochureDownload;
            return this;
        }
        
        public Builder configSource(String configSource) {
            this.configSource = configSource != null ? configSource : "unknown";
            return this;
        }
        
        public ProcessingContext build() {
            return new ProcessingContext(this);
        }
    }
    
    /**
     * Creates a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a ProcessingContext from CommandLineOptions
     */
    public static ProcessingContext fromCommandLineOptions(CommandLineOptions options) {
        return builder()
                .indexLimit(options.getIndexLimit())
                .verbose(options.isVerbose())
                .configSource("command-line")
                .build();
    }
}
