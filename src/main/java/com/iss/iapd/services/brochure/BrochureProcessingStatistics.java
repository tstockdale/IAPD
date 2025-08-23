package com.iss.iapd.services.brochure;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.iss.iapd.config.ProcessingLogger;

/**
 * Comprehensive statistics collector for brochure processing operations
 * Provides detailed metrics and summary reporting capabilities
 */
public class BrochureProcessingStatistics {
    
    // Processing metrics
    private final AtomicInteger totalBrochuresAttempted = new AtomicInteger(0);
    private final AtomicInteger totalBrochuresSuccessful = new AtomicInteger(0);
    private final AtomicInteger totalBrochuresFailed = new AtomicInteger(0);
    private final AtomicInteger totalPdfParsingFailures = new AtomicInteger(0);
    private final AtomicInteger totalTextExtractionFailures = new AtomicInteger(0);
    
    // Content analysis metrics
    private final AtomicInteger brochuresWithProxyProviders = new AtomicInteger(0);
    private final AtomicInteger brochuresWithClassActionProviders = new AtomicInteger(0);
    private final AtomicInteger brochuresWithESGProviders = new AtomicInteger(0);
    private final AtomicInteger brochuresWithESGLanguage = new AtomicInteger(0);
    private final AtomicInteger brochuresWithEmailAddresses = new AtomicInteger(0);
    private final AtomicInteger brochuresWithNoVoteLanguage = new AtomicInteger(0);
    
    // Email extraction metrics
    private final AtomicInteger totalEmailAddressesFound = new AtomicInteger(0);
    private final AtomicInteger brochuresWithComplianceEmails = new AtomicInteger(0);
    private final AtomicInteger brochuresWithProxyEmails = new AtomicInteger(0);
    private final AtomicInteger brochuresWithBrochureEmails = new AtomicInteger(0);
    
    // Provider combination tracking
    private final Map<String, AtomicInteger> providerCombinations = new ConcurrentHashMap<>();
    
    // Timing metrics
    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicLong endTime = new AtomicLong(0);
    
    // File processing metrics
    private final AtomicInteger filesProcessed = new AtomicInteger(0);
    private final AtomicInteger filesSkipped = new AtomicInteger(0);
    private final AtomicInteger resumedProcessingCount = new AtomicInteger(0);
    
    public BrochureProcessingStatistics() {
        this.startTime.set(System.currentTimeMillis());
    }
    
    /**
     * Records the start of processing session
     */
    public void startProcessing() {
        startTime.set(System.currentTimeMillis());
        ProcessingLogger.logInfo("Brochure processing statistics collection started");
    }
    
    /**
     * Records the end of processing session
     */
    public void endProcessing() {
        endTime.set(System.currentTimeMillis());
        totalProcessingTimeMs.set(endTime.get() - startTime.get());
    }
    
    /**
     * Records a successful brochure analysis
     */
    public void recordSuccessfulAnalysis(BrochureAnalysis analysis) {
        totalBrochuresAttempted.incrementAndGet();
        totalBrochuresSuccessful.incrementAndGet();
        
        // Analyze content for statistics
        analyzeContentStatistics(analysis);
    }
    
    /**
     * Records a failed brochure processing attempt
     */
    public void recordFailedAnalysis(String reason) {
        totalBrochuresAttempted.incrementAndGet();
        totalBrochuresFailed.incrementAndGet();
        
        // Categorize failure types
        if (reason != null) {
            if (reason.toLowerCase().contains("pdf")) {
                totalPdfParsingFailures.incrementAndGet();
            } else if (reason.toLowerCase().contains("text") || reason.toLowerCase().contains("extract")) {
                totalTextExtractionFailures.incrementAndGet();
            }
        }
    }
    
    /**
     * Records file processing metrics
     */
    public void recordFileProcessed() {
        filesProcessed.incrementAndGet();
    }
    
    public void recordFileSkipped() {
        filesSkipped.incrementAndGet();
    }
    
    public void recordResumedProcessing() {
        resumedProcessingCount.incrementAndGet();
    }
    
    /**
     * Analyzes brochure content for statistical purposes
     */
    private void analyzeContentStatistics(BrochureAnalysis analysis) {
        // Provider analysis
        if (analysis.getProxyProvider().length() > 0) {
            brochuresWithProxyProviders.incrementAndGet();
        }
        
        if (analysis.getClassActionProvider().length() > 0) {
            brochuresWithClassActionProviders.incrementAndGet();
        }
        
        if (analysis.getEsgProvider().length() > 0) {
            brochuresWithESGProviders.incrementAndGet();
        }
        
        if (analysis.getEsgInvestmentLanguage().length() > 0) {
            brochuresWithESGLanguage.incrementAndGet();
        }
        
        if (analysis.getNoVoteString().length() > 0) {
            brochuresWithNoVoteLanguage.incrementAndGet();
        }
        
        // Email analysis
        if (!analysis.getEmailSet().isEmpty()) {
            brochuresWithEmailAddresses.incrementAndGet();
            totalEmailAddressesFound.addAndGet(analysis.getEmailSet().size());
        }
        
        if (analysis.getEmailComplianceSentence().length() > 0) {
            brochuresWithComplianceEmails.incrementAndGet();
        }
        
        if (analysis.getEmailProxySentence().length() > 0) {
            brochuresWithProxyEmails.incrementAndGet();
        }
        
        if (analysis.getEmailBrochureSentence().length() > 0) {
            brochuresWithBrochureEmails.incrementAndGet();
        }
        
        // Track provider combinations
        recordProviderCombination(analysis);
    }
    
    /**
     * Records provider combinations for analysis
     */
    private void recordProviderCombination(BrochureAnalysis analysis) {
        StringBuilder combination = new StringBuilder();
        
        if (analysis.getProxyProvider().length() > 0) {
            combination.append("Proxy:").append(analysis.getProxyProvider());
        }
        
        if (analysis.getClassActionProvider().length() > 0) {
            if (combination.length() > 0) combination.append("|");
            combination.append("ClassAction:").append(analysis.getClassActionProvider());
        }
        
        if (analysis.getEsgProvider().length() > 0) {
            if (combination.length() > 0) combination.append("|");
            combination.append("ESG:").append(analysis.getEsgProvider());
        }
        
        if (combination.length() > 0) {
            String combinationKey = combination.toString();
            providerCombinations.computeIfAbsent(combinationKey, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }
    
    /**
     * Calculates processing success rate as percentage
     */
    public double getSuccessRate() {
        int attempted = totalBrochuresAttempted.get();
        if (attempted == 0) return 0.0;
        return (double) totalBrochuresSuccessful.get() / attempted * 100.0;
    }
    
    /**
     * Calculates average processing time per brochure in milliseconds
     */
    public double getAverageProcessingTimeMs() {
        int successful = totalBrochuresSuccessful.get();
        if (successful == 0) return 0.0;
        return (double) totalProcessingTimeMs.get() / successful;
    }
    
    /**
     * Calculates processing throughput in brochures per minute
     */
    public double getThroughputPerMinute() {
        long elapsedMs = getElapsedTimeMs();
        if (elapsedMs == 0) return 0.0;
        return (double) totalBrochuresSuccessful.get() / (elapsedMs / 60000.0);
    }
    
    /**
     * Gets elapsed processing time in milliseconds
     */
    public long getElapsedTimeMs() {
        long end = endTime.get() > 0 ? endTime.get() : System.currentTimeMillis();
        return end - startTime.get();
    }
    
    /**
     * Calculates provider detection rates
     */
    public double getProxyProviderDetectionRate() {
        int successful = totalBrochuresSuccessful.get();
        if (successful == 0) return 0.0;
        return (double) brochuresWithProxyProviders.get() / successful * 100.0;
    }
    
    public double getClassActionProviderDetectionRate() {
        int successful = totalBrochuresSuccessful.get();
        if (successful == 0) return 0.0;
        return (double) brochuresWithClassActionProviders.get() / successful * 100.0;
    }
    
    public double getESGProviderDetectionRate() {
        int successful = totalBrochuresSuccessful.get();
        if (successful == 0) return 0.0;
        return (double) brochuresWithESGProviders.get() / successful * 100.0;
    }
    
    public double getEmailDetectionRate() {
        int successful = totalBrochuresSuccessful.get();
        if (successful == 0) return 0.0;
        return (double) brochuresWithEmailAddresses.get() / successful * 100.0;
    }
    
    /**
     * Prints comprehensive summary statistics
     */
    public void printComprehensiveSummary() {
        endProcessing(); // Ensure timing is captured
        
        String separator = "================================================================";
        ProcessingLogger.logInfo("");
        ProcessingLogger.logInfo(separator);
        ProcessingLogger.logInfo("COMPREHENSIVE BROCHURE PROCESSING SUMMARY");
        ProcessingLogger.logInfo("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        ProcessingLogger.logInfo(separator);
        
        // Processing Overview
        ProcessingLogger.logInfo("PROCESSING OVERVIEW:");
        ProcessingLogger.logInfo("  Total brochures attempted: " + totalBrochuresAttempted.get());
        ProcessingLogger.logInfo("  Successfully processed: " + totalBrochuresSuccessful.get());
        ProcessingLogger.logInfo("  Failed processing: " + totalBrochuresFailed.get());
        ProcessingLogger.logInfo("  Success rate: " + String.format("%.2f%%", getSuccessRate()));
        ProcessingLogger.logInfo("  Files processed: " + filesProcessed.get());
        ProcessingLogger.logInfo("  Files skipped: " + filesSkipped.get());
        if (resumedProcessingCount.get() > 0) {
            ProcessingLogger.logInfo("  Resumed processing count: " + resumedProcessingCount.get());
        }
        ProcessingLogger.logInfo("");
        
        // Performance Metrics
        ProcessingLogger.logInfo("PERFORMANCE METRICS:");
        ProcessingLogger.logInfo("  Total processing time: " + formatDuration(getElapsedTimeMs()));
        ProcessingLogger.logInfo("  Average time per brochure: " + String.format("%.2f ms", getAverageProcessingTimeMs()));
        ProcessingLogger.logInfo("  Processing throughput: " + String.format("%.2f brochures/minute", getThroughputPerMinute()));
        ProcessingLogger.logInfo("");
        
        // Content Analysis Results
        ProcessingLogger.logInfo("CONTENT ANALYSIS RESULTS:");
        ProcessingLogger.logInfo("  Brochures with proxy providers: " + brochuresWithProxyProviders.get() + 
                                " (" + String.format("%.2f%%", getProxyProviderDetectionRate()) + ")");
        ProcessingLogger.logInfo("  Brochures with class action providers: " + brochuresWithClassActionProviders.get() + 
                                " (" + String.format("%.2f%%", getClassActionProviderDetectionRate()) + ")");
        ProcessingLogger.logInfo("  Brochures with ESG providers: " + brochuresWithESGProviders.get() + 
                                " (" + String.format("%.2f%%", getESGProviderDetectionRate()) + ")");
        ProcessingLogger.logInfo("  Brochures with ESG language: " + brochuresWithESGLanguage.get());
        ProcessingLogger.logInfo("  Brochures with no-vote language: " + brochuresWithNoVoteLanguage.get());
        ProcessingLogger.logInfo("");
        
        // Email Analysis Results
        ProcessingLogger.logInfo("EMAIL ANALYSIS RESULTS:");
        ProcessingLogger.logInfo("  Brochures with email addresses: " + brochuresWithEmailAddresses.get() + 
                                " (" + String.format("%.2f%%", getEmailDetectionRate()) + ")");
        ProcessingLogger.logInfo("  Total email addresses found: " + totalEmailAddressesFound.get());
        if (brochuresWithEmailAddresses.get() > 0) {
            double avgEmails = (double) totalEmailAddressesFound.get() / brochuresWithEmailAddresses.get();
            ProcessingLogger.logInfo("  Average emails per brochure (with emails): " + String.format("%.2f", avgEmails));
        }
        ProcessingLogger.logInfo("  Brochures with compliance emails: " + brochuresWithComplianceEmails.get());
        ProcessingLogger.logInfo("  Brochures with proxy emails: " + brochuresWithProxyEmails.get());
        ProcessingLogger.logInfo("  Brochures with brochure emails: " + brochuresWithBrochureEmails.get());
        ProcessingLogger.logInfo("");
        
        // Error Analysis
        if (totalBrochuresFailed.get() > 0) {
            ProcessingLogger.logInfo("ERROR ANALYSIS:");
            ProcessingLogger.logInfo("  PDF parsing failures: " + totalPdfParsingFailures.get());
            ProcessingLogger.logInfo("  Text extraction failures: " + totalTextExtractionFailures.get());
            ProcessingLogger.logInfo("  Other failures: " + (totalBrochuresFailed.get() - totalPdfParsingFailures.get() - totalTextExtractionFailures.get()));
            ProcessingLogger.logInfo("");
        }
        
        // Top Provider Combinations
        if (!providerCombinations.isEmpty()) {
            ProcessingLogger.logInfo("TOP PROVIDER COMBINATIONS:");
            providerCombinations.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().get() - e1.getValue().get())
                .limit(10)
                .forEach(entry -> ProcessingLogger.logInfo("  " + entry.getKey() + ": " + entry.getValue().get()));
            ProcessingLogger.logInfo("");
        }
        
        ProcessingLogger.logInfo(separator);
        ProcessingLogger.logInfo("END OF COMPREHENSIVE SUMMARY");
        ProcessingLogger.logInfo(separator);
    }
    
    /**
     * Prints a brief progress summary during processing
     */
    public void printProgressSummary() {
        ProcessingLogger.logInfo("=== PROCESSING PROGRESS SUMMARY ===");
        ProcessingLogger.logInfo("Processed: " + totalBrochuresSuccessful.get() + 
                                " | Failed: " + totalBrochuresFailed.get() + 
                                " | Success Rate: " + String.format("%.2f%%", getSuccessRate()));
        ProcessingLogger.logInfo("Throughput: " + String.format("%.2f brochures/min", getThroughputPerMinute()) + 
                                " | Elapsed: " + formatDuration(getElapsedTimeMs()));
        ProcessingLogger.logInfo("===================================");
    }
    
    /**
     * Formats duration in milliseconds to human-readable format
     */
    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    // Getter methods for individual statistics
    public int getTotalBrochuresAttempted() { return totalBrochuresAttempted.get(); }
    public int getTotalBrochuresSuccessful() { return totalBrochuresSuccessful.get(); }
    public int getTotalBrochuresFailed() { return totalBrochuresFailed.get(); }
    public int getBrochuresWithProxyProviders() { return brochuresWithProxyProviders.get(); }
    public int getBrochuresWithClassActionProviders() { return brochuresWithClassActionProviders.get(); }
    public int getBrochuresWithESGProviders() { return brochuresWithESGProviders.get(); }
    public int getBrochuresWithEmailAddresses() { return brochuresWithEmailAddresses.get(); }
    public int getTotalEmailAddressesFound() { return totalEmailAddressesFound.get(); }
    public Map<String, AtomicInteger> getProviderCombinations() { return providerCombinations; }
}
