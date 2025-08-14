package com.iss.iapd.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Log4j-based logging utility for processing operations with failure tracking
 * Uses log4j2.xml configuration file for logger setup
 * Provides two loggers:
 * - ProcessingLogger: console + rolling file (5MB rollover)
 * - ProviderMatchLogger: file-only (50MB rollover)
 */
public class ProcessingLogger {
    
    private static final Logger logger;
    private static final Logger providerMatchLogger;
    
    // Static initializer to get loggers and ensure log directory exists
    static {
        // Ensure log directory exists before logging starts
        ensureLogDirectoryExists();
        
        // Get loggers - configuration comes from log4j2.xml
        logger = LogManager.getLogger("ProcessingLogger");
        providerMatchLogger = LogManager.getLogger("ProviderMatchLogger");
    }
    
    /**
     * Public method to explicitly initialize the ProcessingLogger
     * This forces the static block to run and ensures proper initialization
     */
    public static void initialize() {
        // This method forces the static block to execute
        // The actual initialization happens in the static block above
        System.out.println("ProcessingLogger explicitly initialized");
    }
    
    /**
     * Ensures the log directory exists before logging starts
     */
    private static void ensureLogDirectoryExists() {
        try {
            // Get log path from Config and resolve it properly
            String logPath = Config.LOG_PATH.startsWith("./") ? Config.LOG_PATH.substring(2) : Config.LOG_PATH;
            String logDir = System.getProperty("user.dir") + File.separator + logPath;
            
            File logDirectory = new File(logDir);
            if (!logDirectory.exists()) {
                boolean created = logDirectory.mkdirs();
                if (created) {
                    System.out.println("Created log directory: " + logDir);
                }
            }
            
            // Set system property for log path so log4j2.xml can use it
            System.setProperty("log.path", logPath);
            System.out.println("Log path is " + System.getProperty("log.path"));
            
        } catch (Exception e) {
            System.err.println("Failed to ensure log directory exists: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Processing counters
    private static final AtomicInteger firmsWithoutBrochures = new AtomicInteger(0);
    private static final AtomicInteger firmsWithBrochures = new AtomicInteger(0);
    private static final AtomicInteger brochureDownloadFailures = new AtomicInteger(0);
    private static final AtomicInteger filenameParsingFailures = new AtomicInteger(0);
    private static final AtomicInteger totalFirmsProcessed = new AtomicInteger(0);
    private static final AtomicInteger brochuresDownloadedCount = new AtomicInteger(0);
    
    // Provider match counters
    private static final AtomicInteger glassLewisMatches = new AtomicInteger(0);
    private static final AtomicInteger broadridgeMatches = new AtomicInteger(0);
    private static final AtomicInteger proxyEdgeMatches = new AtomicInteger(0);
    private static final AtomicInteger eganJonesMatches = new AtomicInteger(0);
    private static final AtomicInteger issProxyMatches = new AtomicInteger(0);
    private static final AtomicInteger thirdPartyProxyMatches = new AtomicInteger(0);
    private static final AtomicInteger frtMatches = new AtomicInteger(0);
    private static final AtomicInteger issClassActionMatches = new AtomicInteger(0);
    private static final AtomicInteger batteaMatches = new AtomicInteger(0);
    private static final AtomicInteger cccMatches = new AtomicInteger(0);
    private static final AtomicInteger robbinsGellerMatches = new AtomicInteger(0);
    private static final AtomicInteger sustainalyticsMatches = new AtomicInteger(0);
    private static final AtomicInteger msciMatches = new AtomicInteger(0);
    private static final AtomicInteger totalProviderMatches = new AtomicInteger(0);
    
    public static void logInfo(String message) {
        logger.info(message);
    }
    
    public static void logWarning(String message) {
        logger.warn(message);
    }
    
    public static void logError(String message, Exception e) {
        if (e != null) {
            logger.error(message, e);
        } else {
            logger.error(message);
        }
    }
    
    public static void incrementFirmsWithoutBrochures() {
        int count = firmsWithoutBrochures.incrementAndGet();
        logger.warn("Firms without brochures count incremented to: {}", count);
    }
    
    public static void incrementFirmsWithBrochures() {
    	int count = firmsWithBrochures.incrementAndGet();
    	logger.info("Firms with brochures count incremented to: {}", count);
    }

    public static void incrementBrochuresDownloadedCount() {
    	int count = brochuresDownloadedCount.incrementAndGet();
    	logger.info("Brochures downloaded count incremented to: {}", count);
    }
    
    public static void incrementBrochureDownloadFailures() {
        int count = brochureDownloadFailures.incrementAndGet();
        logger.warn("Brochure download failure count incremented to: {}", count);
    }
    
    public static void incrementFilenameParsingFailures() {
        int count = filenameParsingFailures.incrementAndGet();
        logger.warn("Filename parsing failure count incremented to: {}", count);
    }
    
    public static void incrementTotalFirmsProcessed() {
        int count = totalFirmsProcessed.incrementAndGet();
        logger.info("Total firms processed count incremented to: {}", count);
    }
    
    public static void printProcessingSummary() {
        String separator = "==================================================";
        logger.info("\n{}", separator);
        logger.info("PROCESSING SUMMARY");
        logger.info(separator);
        logger.info("Total firms processed: {}", totalFirmsProcessed.get());
        logger.info("Firms without brochures: {}", firmsWithoutBrochures.get());
        logger.info("Brochures downloaded successfully: {}", firmsWithBrochures.get());
        logger.info("Brochure download failures: {}", brochureDownloadFailures.get());
        logger.info("Filename parsing failures: {}", filenameParsingFailures.get());
        
        if (totalFirmsProcessed.get() > 0) {
            double firmsWithBrochuresRate = ((double)(totalFirmsProcessed.get() - firmsWithoutBrochures.get()) / totalFirmsProcessed.get()) * 100;
            logger.info("Firms with brochures rate: {}%", String.format("%.2f", firmsWithBrochuresRate));
        }
        logger.info(separator);
    }
    
    public static void resetCounters() {
        firmsWithoutBrochures.set(0);
        firmsWithBrochures.set(0);
        brochureDownloadFailures.set(0);
        filenameParsingFailures.set(0);
        totalFirmsProcessed.set(0);
        logger.info("All processing counters have been reset to 0");
    }
    
    // Getter methods for accessing counter values if needed
    public static int getFirmsWithoutBrochures() {
        return firmsWithoutBrochures.get();
    }
    
    public static int getFirmsWithBrochures() {
    	return firmsWithBrochures.get();
    }

    public static int getBrochuresDownloadedCount() {
        return brochuresDownloadedCount.get();
    }
    
    public static int getBrochureDownloadFailures() {
        return brochureDownloadFailures.get();
    }
    
    public static int getFilenameParsingFailures() {
        return filenameParsingFailures.get();
    }
    
    public static int getTotalFirmsProcessed() {
        return totalFirmsProcessed.get();
    }
    
    /**
     * Gets the provider match logger for logging provider match strings
     * This logger writes to provider_match_strings.log with 50MB rollover
     */
    public static Logger getProviderMatchLogger() {
        return providerMatchLogger;
    }
    
    /**
     * Enhanced method to log provider match information with counting
     */
    public static void logProviderMatch(String providerName, String matchedText) {
        // Increment the appropriate counter based on provider name
        int count = incrementProviderMatchCounter(providerName);
        
        // Log the match with count information
        String logMessage = providerName + " (match #" + count + "):   " + matchedText;
        providerMatchLogger.info(logMessage);
        
        // Also increment total matches
        int totalCount = totalProviderMatches.incrementAndGet();
        if (totalCount % 10 == 0) { // Log milestone every 10 matches
            providerMatchLogger.info("--- Total provider matches reached: " + totalCount + " ---");
        }
    }
    
    /**
     * Simple convenience method for non-provider specific logging
     */
    public static void logProviderMatch(String message) {
        providerMatchLogger.info(message);
    }
    
    /**
     * Increments the counter for a specific provider and returns the new count
     */
    private static int incrementProviderMatchCounter(String providerName) {
        switch (providerName.toLowerCase()) {
            case "glass lewis":
                return glassLewisMatches.incrementAndGet();
            case "broadridge":
                return broadridgeMatches.incrementAndGet();
            case "proxyedge":
                return proxyEdgeMatches.incrementAndGet();
            case "egan-jones":
                return eganJonesMatches.incrementAndGet();
            case "iss":
                return issProxyMatches.incrementAndGet();
            case "third party":
                return thirdPartyProxyMatches.incrementAndGet();
            case "frt":
                return frtMatches.incrementAndGet();
            case "iss class action":
                return issClassActionMatches.incrementAndGet();
            case "battea":
                return batteaMatches.incrementAndGet();
            case "ccc":
                return cccMatches.incrementAndGet();
            case "robbins geller":
                return robbinsGellerMatches.incrementAndGet();
            case "sustainalytics":
                return sustainalyticsMatches.incrementAndGet();
            case "msci":
                return msciMatches.incrementAndGet();
            default:
                // For unknown providers, just return 1
                return 1;
        }
    }
    
    /**
     * Prints a comprehensive summary of all provider matches
     */
    public static void printProviderMatchSummary() {
        String separator = "==================================================";
        providerMatchLogger.info("");
        providerMatchLogger.info(separator);
        providerMatchLogger.info("PROVIDER MATCH SUMMARY");
        providerMatchLogger.info(separator);
        
        // Proxy Providers
        providerMatchLogger.info("PROXY PROVIDERS:");
        providerMatchLogger.info("  Glass Lewis: " + glassLewisMatches.get());
        providerMatchLogger.info("  BroadRidge: " + broadridgeMatches.get());
        providerMatchLogger.info("  ProxyEdge: " + proxyEdgeMatches.get());
        providerMatchLogger.info("  Egan-Jones: " + eganJonesMatches.get());
        providerMatchLogger.info("  ISS: " + issProxyMatches.get());
        providerMatchLogger.info("  Third Party: " + thirdPartyProxyMatches.get());
        
        // Class Action Providers
        providerMatchLogger.info("CLASS ACTION PROVIDERS:");
        providerMatchLogger.info("  FRT: " + frtMatches.get());
        providerMatchLogger.info("  ISS Class Action: " + issClassActionMatches.get());
        providerMatchLogger.info("  Battea: " + batteaMatches.get());
        providerMatchLogger.info("  CCC: " + cccMatches.get());
        providerMatchLogger.info("  Robbins Geller: " + robbinsGellerMatches.get());
        
        // ESG Providers
        providerMatchLogger.info("ESG PROVIDERS:");
        providerMatchLogger.info("  Sustainalytics: " + sustainalyticsMatches.get());
        providerMatchLogger.info("  MSCI: " + msciMatches.get());
        
        providerMatchLogger.info(separator);
        providerMatchLogger.info("TOTAL PROVIDER MATCHES: " + totalProviderMatches.get());
        providerMatchLogger.info(separator);
    }
    
    /**
     * Resets all provider match counters
     */
    public static void resetProviderMatchCounters() {
        glassLewisMatches.set(0);
        broadridgeMatches.set(0);
        proxyEdgeMatches.set(0);
        eganJonesMatches.set(0);
        issProxyMatches.set(0);
        thirdPartyProxyMatches.set(0);
        frtMatches.set(0);
        issClassActionMatches.set(0);
        batteaMatches.set(0);
        cccMatches.set(0);
        robbinsGellerMatches.set(0);
        sustainalyticsMatches.set(0);
        msciMatches.set(0);
        totalProviderMatches.set(0);
        providerMatchLogger.info("All provider match counters have been reset to 0");
    }
    
    /**
     * Enhanced reset method that includes provider match counters
     */
    public static void resetAllCounters() {
        resetCounters();
        resetProviderMatchCounters();
    }
    
    // Getter methods for provider match counters
    public static int getTotalProviderMatches() {
        return totalProviderMatches.get();
    }
    
    public static int getGlassLewisMatches() {
        return glassLewisMatches.get();
    }
    
    public static int getBroadridgeMatches() {
        return broadridgeMatches.get();
    }
    
    public static int getSustainalyticsMatches() {
        return sustainalyticsMatches.get();
    }
    
    public static int getMsciMatches() {
        return msciMatches.get();
    }
    
    public static int getFrtMatches() {
        return frtMatches.get();
    }
}
