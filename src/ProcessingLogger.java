import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.Level;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Log4j-based logging utility for processing operations with failure tracking
 * Configured programmatically with console and rolling file appenders (5MB rollover)
 * Also provides a separate provider match logger with 50MB rollover
 */
public class ProcessingLogger {
    
    private static final Logger logger;
    private static final Logger providerMatchLogger;
    
    // Static initializer to configure logging programmatically
    static {
        configureLogging();
        logger = LogManager.getLogger("ProcessingLogger");
        providerMatchLogger = LogManager.getLogger("ProviderMatchLogger");
    }
    
    /**
     * Configures Log4j programmatically instead of using XML configuration
     */
    private static void configureLogging() {
        try {
            // Disable XML configuration loading completely
            System.setProperty("log4j2.configurationFile", "");
            System.setProperty("log4j.configurationFile", "");
            System.setProperty("log4j2.disable.jmx", "true");
            
            // Get log path from Config and resolve it properly
            String logPath = Config.LOG_PATH.startsWith("./") ? Config.LOG_PATH.substring(2) : Config.LOG_PATH;
            String logDir = System.getProperty("user.dir") + File.separator + logPath;
            String logFile = logDir + File.separator + "processing.log";
            String logPattern = logDir + File.separator + "processing-%i.log.gz";
            
            // Ensure log directory exists
            File logDirectory = new File(logDir);
            if (!logDirectory.exists()) {
                logDirectory.mkdirs();
            }
            
            // Create a completely new default configuration without getting existing context
            DefaultConfiguration config = new DefaultConfiguration();
            config.setName("ProgrammaticConfig");
            
            // Create pattern layout
            PatternLayout layout = PatternLayout.newBuilder()
                    .withPattern("%d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n")
                    .withConfiguration(config)
                    .build();
            
            // Create console appender
            ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                    .setName("Console")
                    .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                    .setLayout(layout)
                    .setConfiguration(config)
                    .build();
            consoleAppender.start();
            config.addAppender(consoleAppender);
            
            // Create rolling file appender for processing logs
            SizeBasedTriggeringPolicy policy = SizeBasedTriggeringPolicy.createPolicy("5MB");
            DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                    .withMax("10")
                    .withConfig(config)
                    .build();
            
            RollingFileAppender fileAppender = RollingFileAppender.newBuilder()
                    .setName("RollingFile")
                    .withFileName(logFile)
                    .withFilePattern(logPattern)
                    .setLayout(layout)
                    .withPolicy(policy)
                    .withStrategy(strategy)
                    .setConfiguration(config)
                    .build();
            fileAppender.start();
            config.addAppender(fileAppender);
            
            // Create provider match logger appender with 50MB rollover
            String providerLogFile = logDir + File.separator + "provider_match_strings.log";
            String providerLogPattern = logDir + File.separator + "provider_match_strings-%i.log.gz";
            
            // Create simple pattern layout for provider matches (no logger name, just timestamp and message)
            PatternLayout providerLayout = PatternLayout.newBuilder()
                    .withPattern("%d{yyyy-MM-dd HH:mm:ss} - %msg%n")
                    .withConfiguration(config)
                    .build();
            
            SizeBasedTriggeringPolicy providerPolicy = SizeBasedTriggeringPolicy.createPolicy("50MB");
            DefaultRolloverStrategy providerStrategy = DefaultRolloverStrategy.newBuilder()
                    .withMax("10")
                    .withConfig(config)
                    .build();
            
            RollingFileAppender providerFileAppender = RollingFileAppender.newBuilder()
                    .setName("ProviderMatchFile")
                    .withFileName(providerLogFile)
                    .withFilePattern(providerLogPattern)
                    .setLayout(providerLayout)
                    .withPolicy(providerPolicy)
                    .withStrategy(providerStrategy)
                    .setConfiguration(config)
                    .build();
            providerFileAppender.start();
            config.addAppender(providerFileAppender);
            
            // Configure ProcessingLogger
            org.apache.logging.log4j.core.config.LoggerConfig processingLoggerConfig = 
                    org.apache.logging.log4j.core.config.LoggerConfig.newBuilder()
                            .withAdditivity(false)
                            .withLevel(Level.INFO)
                            .withLoggerName("ProcessingLogger")
                            .withIncludeLocation("true")
                            .withRefs(new org.apache.logging.log4j.core.config.AppenderRef[]{
                                    org.apache.logging.log4j.core.config.AppenderRef.createAppenderRef("Console", null, null),
                                    org.apache.logging.log4j.core.config.AppenderRef.createAppenderRef("RollingFile", null, null)
                            })
                            .withProperties(null)
                            .withConfig(config)
                            .build();
            processingLoggerConfig.addAppender(consoleAppender, null, null);
            processingLoggerConfig.addAppender(fileAppender, null, null);
            config.addLogger("ProcessingLogger", processingLoggerConfig);
            
            // Configure ProviderMatchLogger (file-only, no console output, no additivity)
            org.apache.logging.log4j.core.config.LoggerConfig providerLoggerConfig = 
                    org.apache.logging.log4j.core.config.LoggerConfig.newBuilder()
                            .withAdditivity(false)
                            .withLevel(Level.INFO)
                            .withLoggerName("ProviderMatchLogger")
                            .withIncludeLocation("false")
                            .withRefs(new org.apache.logging.log4j.core.config.AppenderRef[]{
                                    org.apache.logging.log4j.core.config.AppenderRef.createAppenderRef("ProviderMatchFile", null, null)
                            })
                            .withProperties(null)
                            .withConfig(config)
                            .build();
            providerLoggerConfig.addAppender(providerFileAppender, null, null);
            config.addLogger("ProviderMatchLogger", providerLoggerConfig);
            
            // Configure root logger
            org.apache.logging.log4j.core.config.LoggerConfig rootLoggerConfig = config.getRootLogger();
            rootLoggerConfig.addAppender(consoleAppender, null, null);
            rootLoggerConfig.addAppender(fileAppender, null, null);
            rootLoggerConfig.setLevel(Level.INFO);
            
            // Start the configuration first
            config.start();
            
            // Now get the context and set our configuration
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.setConfiguration(config);
            context.updateLoggers();
            
        } catch (Exception e) {
            System.err.println("Failed to configure logging programmatically: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Processing counters
    private static final AtomicInteger firmsWithoutBrochures = new AtomicInteger(0);
    private static final AtomicInteger firmsWithBrochures = new AtomicInteger(0);
    private static final AtomicInteger brochureDownloadFailures = new AtomicInteger(0);
    private static final AtomicInteger filenameParsingFailures = new AtomicInteger(0);
    private static final AtomicInteger totalFirmsProcessed = new AtomicInteger(0);
    
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
