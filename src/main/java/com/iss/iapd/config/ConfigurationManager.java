package com.iss.iapd.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.iss.iapd.core.ProcessingContext;

/**
 * Manages configuration from multiple sources with priority ordering:
 * 1. Command line arguments (highest priority)
 * 2. Configuration file settings
 * 3. Database configuration
 * 4. Default values (lowest priority)
 */
public class ConfigurationManager {
    
    private static final String DEFAULT_CONFIG_FILE = "iapd.properties";
    
    /**
     * Builds a ProcessingContext by merging configuration from all sources
     * @param args command line arguments
     * @return configured ProcessingContext
     */
    public ProcessingContext buildContext(String[] args) {
        try {
            // Start with default configuration
            ProcessingContext.Builder builder = ProcessingContext.builder()
                    .configSource("default");
            
            // Layer 1: Database configuration (if available)
            applyDatabaseConfiguration(builder);
            
            // Layer 2: Configuration file (if exists)
            applyFileConfiguration(builder);
            
            // Layer 3: Command line arguments (highest priority)
            applyCommandLineConfiguration(builder, args);
            
            return builder.build();
            
        } catch (Exception e) {
            System.err.println("Error building configuration: " + e.getMessage());
            // Fall back to command line only
            try {
                CommandLineOptions options = CommandLineOptions.parseArgs(args);
                return ProcessingContext.fromCommandLineOptions(options);
            } catch (IllegalArgumentException cmdError) {
                // Ultimate fallback to defaults
                return ProcessingContext.builder()
                        .configSource("fallback-default")
                        .build();
            }
        }
    }
    
    /**
     * Applies database configuration (placeholder for future implementation)
     */
    private void applyDatabaseConfiguration(ProcessingContext.Builder builder) {
        // TODO: Implement database configuration loading
        // This would connect to a database and load configuration values
        // For now, this is a placeholder that could be implemented later
        
        // Example implementation:
        // try {
        //     DatabaseConfig dbConfig = loadFromDatabase();
        //     if (dbConfig != null) {
        //         builder.indexLimit(dbConfig.getIndexLimit())
        //                .verbose(dbConfig.isVerbose())
        //                .retryCount(dbConfig.getRetryCount())
        //                .configSource("database");
        //     }
        // } catch (Exception e) {
        //     System.err.println("Warning: Could not load database configuration: " + e.getMessage());
        // }
    }
    
    /**
     * Applies configuration from properties file
     */
    private void applyFileConfiguration(ProcessingContext.Builder builder) {
        Properties props = loadPropertiesFile();
        if (props != null) {
            // Apply file-based configuration
            if (props.containsKey("index.limit")) {
                try {
                    int indexLimit = Integer.parseInt(props.getProperty("index.limit"));
                    builder.indexLimit(indexLimit);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid index.limit in config file: " + props.getProperty("index.limit"));
                }
            }
            
            if (props.containsKey("verbose")) {
                builder.verbose(Boolean.parseBoolean(props.getProperty("verbose")));
            }
            
            if (props.containsKey("output.format")) {
                builder.outputFormat(props.getProperty("output.format"));
            }
            
            if (props.containsKey("retry.count")) {
                try {
                    int retryCount = Integer.parseInt(props.getProperty("retry.count"));
                    builder.retryCount(retryCount);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid retry.count in config file: " + props.getProperty("retry.count"));
                }
            }
            
            if (props.containsKey("skip.brochure.download")) {
                builder.skipBrochureDownload(Boolean.parseBoolean(props.getProperty("skip.brochure.download")));
            }
            
            // Rate limiting properties
            if (props.containsKey("rate.limit.xml.per.second")) {
                try {
                    int rate = Integer.parseInt(props.getProperty("rate.limit.xml.per.second"));
                    builder.urlRatePerSecond(rate);
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Invalid rate.limit.xml.per.second in config file: " + props.getProperty("rate.limit.xml.per.second"));
                }
            }
            if (props.containsKey("rate.limit.download.per.second")) {
                try {
                    int rate = Integer.parseInt(props.getProperty("rate.limit.download.per.second"));
                    builder.downloadRatePerSecond(rate);
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Invalid rate.limit.download.per.second in config file: " + props.getProperty("rate.limit.download.per.second"));
                }
            }
            
            builder.configSource("file");
        }
    }
    
    /**
     * Applies command line configuration (highest priority)
     */
    private void applyCommandLineConfiguration(ProcessingContext.Builder builder, String[] args) {
        try {
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            // Command line arguments override all other sources
            builder.indexLimit(options.getIndexLimit())
                   .verbose(options.isVerbose())
                   .resumeDownloads(options.isResumeDownloads())
                   .resumeProcessing(options.isResumeProcessing())
                   .validatePdfs(options.isValidatePdfs())
                   .forceRestart(options.isForceRestart())
                   .incrementalUpdates(options.isIncrementalUpdates())
                   .incrementalDownloads(options.isIncrementalDownloads())
                   .incrementalProcessing(options.isIncrementalProcessing())
                   .baselineFilePath(options.getBaselineFilePath())
                   .configSource("command-line");
            
            // Optional rate overrides
            if (options.getURLRatePerSecond() != null) {
                builder.urlRatePerSecond(options.getURLRatePerSecond());
            }
            if (options.getDownloadRatePerSecond() != null) {
                builder.downloadRatePerSecond(options.getDownloadRatePerSecond());
            }
                   
        } catch (IllegalArgumentException e) {
            // If command line parsing fails, let the exception bubble up
            throw e;
        }
    }
    
    /**
     * Loads properties from configuration file
     */
    private Properties loadPropertiesFile() {
        Properties props = new Properties();
        
        // Try to load from current directory first
        Path configPath = Paths.get(DEFAULT_CONFIG_FILE);
        if (Files.exists(configPath)) {
            try {
                props.load(Files.newInputStream(configPath));
                System.out.println("Loaded configuration from: " + DEFAULT_CONFIG_FILE);
                return props;
            } catch (IOException e) {
                System.err.println("Warning: Could not load configuration file: " + e.getMessage());
            }
        }
        
        // Try to load from classpath
        try {
            ClassLoader classLoader = ConfigurationManager.class.getClassLoader();
            if (classLoader.getResourceAsStream(DEFAULT_CONFIG_FILE) != null) {
                props.load(classLoader.getResourceAsStream(DEFAULT_CONFIG_FILE));
                System.out.println("Loaded configuration from classpath: " + DEFAULT_CONFIG_FILE);
                return props;
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load configuration file from classpath: " + e.getMessage());
        }
        
        return null; // No configuration file found
    }
    
    /**
     * Creates a sample configuration file for reference
     */
    public void createSampleConfigFile(String filename) {
        Properties props = new Properties();
        props.setProperty("# IAPD Parser Configuration File", "");
        props.setProperty("# Maximum number of firms to process (default: unlimited)", "");
        props.setProperty("index.limit", "1000");
        props.setProperty("", "");
        props.setProperty("# Enable verbose logging (default: false)", "");
        props.setProperty("verbose", "false");
        props.setProperty(" ", "");
        props.setProperty("# Output format (default: CSV)", "");
        props.setProperty("output.format", "CSV");
        props.setProperty("  ", "");
        props.setProperty("# Number of retry attempts (default: 3)", "");
        props.setProperty("retry.count", "3");
        props.setProperty("   ", "");
        props.setProperty("# Skip brochure download (default: false)", "");
        props.setProperty("skip.brochure.download", "false");
    props.setProperty("    ", "");
    props.setProperty("# Rate limits (operations per second)", "");
    props.setProperty("# How many firms to parse per second when reading XML", "");
    props.setProperty("rate.limit.xml.per.second", "1");
    props.setProperty("# How many brochure downloads per second", "");
    props.setProperty("rate.limit.download.per.second", "1");
        
        try {
            Path configPath = Paths.get(filename);
            props.store(Files.newOutputStream(configPath), "IAPD Parser Configuration");
            System.out.println("Sample configuration file created: " + filename);
        } catch (IOException e) {
            System.err.println("Error creating sample configuration file: " + e.getMessage());
        }
    }
    
    /**
     * Validates the final configuration
     */
    public boolean validateConfiguration(ProcessingContext context) {
        boolean valid = true;
        
        if (context.getIndexLimit() <= 0) {
            System.err.println("Error: Index limit must be positive");
            valid = false;
        }
        
        if (context.getRetryCount() < 0) {
            System.err.println("Error: Retry count cannot be negative");
            valid = false;
        }
        
        if (context.getOutputFormat() == null || context.getOutputFormat().trim().isEmpty()) {
            System.err.println("Error: Output format cannot be empty");
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Prints the effective configuration
     */
    public void printEffectiveConfiguration(ProcessingContext context) {
        if (context == null) {
            System.out.println("=== Effective Configuration ===");
            System.out.println("Error: No configuration context provided");
            System.out.println("===============================");
            return;
        }
        
        System.out.println("=== Effective Configuration ===");
        System.out.println("Config Source: " + context.getConfigSource());
        System.out.println("Index Limit: " + (context.getIndexLimit() == Integer.MAX_VALUE ? "unlimited" : context.getIndexLimit()));
        System.out.println("Verbose: " + context.isVerbose());
        System.out.println("Output Format: " + context.getOutputFormat());
        System.out.println("Retry Count: " + context.getRetryCount());
        System.out.println("Skip Brochure Download: " + context.isSkipBrochureDownload());
    System.out.println("XML Rate (ops/sec): " + context.getURLRatePerSecond());
    System.out.println("Download Rate (ops/sec): " + context.getDownloadRatePerSecond());
        System.out.println("Created At: " + context.getCreatedAt());
        System.out.println("===============================");
    }
}
