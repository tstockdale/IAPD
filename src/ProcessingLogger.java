import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.Level;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Log4j-based logging utility for processing operations with failure tracking
 * Configured programmatically with console and rolling file appenders (5MB rollover)
 */
public class ProcessingLogger {
    
    private static final Logger logger;
    
    // Static initializer to configure logging programmatically
    static {
        configureLogging();
        logger = LogManager.getLogger("ProcessingLogger");
    }
    
    /**
     * Configures Log4j programmatically instead of using XML configuration
     */
    private static void configureLogging() {
        try {
            // Disable XML configuration loading completely
            System.setProperty("log4j2.configurationFile", "");
            
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
            
            // Create rolling file appender
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
            
            // Configure ProcessingLogger
            org.apache.logging.log4j.core.config.LoggerConfig processingLoggerConfig = 
                    org.apache.logging.log4j.core.config.LoggerConfig.createLogger(
                            false, Level.INFO, "ProcessingLogger", "true", 
                            new org.apache.logging.log4j.core.config.AppenderRef[]{
                                    org.apache.logging.log4j.core.config.AppenderRef.createAppenderRef("Console", null, null),
                                    org.apache.logging.log4j.core.config.AppenderRef.createAppenderRef("RollingFile", null, null)
                            }, null, config, null);
            processingLoggerConfig.addAppender(consoleAppender, null, null);
            processingLoggerConfig.addAppender(fileAppender, null, null);
            config.addLogger("ProcessingLogger", processingLoggerConfig);
            
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
    
    // Failure counters
    private static final AtomicInteger firmsWithoutBrochures = new AtomicInteger(0);
    private static final AtomicInteger brochureDownloadFailures = new AtomicInteger(0);
    private static final AtomicInteger filenameParsingFailures = new AtomicInteger(0);
    private static final AtomicInteger totalFirmsProcessed = new AtomicInteger(0);
    
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
        logger.info("Brochure URL failures: {}", firmsWithoutBrochures.get());
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
        brochureDownloadFailures.set(0);
        filenameParsingFailures.set(0);
        totalFirmsProcessed.set(0);
        logger.info("All processing counters have been reset to 0");
    }
    
    // Getter methods for accessing counter values if needed
    public static int getFirmsWithoutBrochures() {
        return firmsWithoutBrochures.get();
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
}
