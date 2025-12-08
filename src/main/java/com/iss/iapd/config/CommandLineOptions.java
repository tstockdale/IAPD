package com.iss.iapd.config;

/**
 * Command line options parser for the IAPD Parser application
 * Provides a centralized way to handle command line arguments with extensibility for future options
 */
public class CommandLineOptions {
    
    private int indexLimit = Integer.MAX_VALUE;
    private boolean verbose = false;
    private boolean showHelp = false;
    private boolean forceRestart = false;
    private boolean incrementalUpdates = false;
    private boolean incrementalDownloads = false;
    private boolean incrementalProcessing = false;
    private String baselineFilePath = null;

    // Optional overrides for rate limits (null means not provided on CLI)
    private Integer urlRatePerSecond = null;
    private Integer downloadRatePerSecond = null;


    /**
     * Parses command line arguments and returns a CommandLineOptions object
     * @param args command line arguments
     * @return parsed CommandLineOptions
     * @throws IllegalArgumentException if invalid arguments are provided
     */
    public static CommandLineOptions parseArgs(String[] args) throws IllegalArgumentException {
        CommandLineOptions options = new CommandLineOptions();
        
        // Handle null arguments gracefully
        if (args == null) {
            return options;
        }
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            // Handle arguments with equals sign (e.g., --index-limit=250)
            if (arg.contains("=")) {
                String[] parts = arg.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0];
                    String value = parts[1].trim(); // Handle extra spaces
                    
                    if ("--index-limit".equals(key) || "-l".equals(key)) {
                        try {
                            options.indexLimit = Integer.parseInt(value);
                            if (options.indexLimit < 0) {
                                throw new IllegalArgumentException("Index limit must be a positive integer");
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid index limit value: " + value);
                        }
                        continue;
                    } else if ("--baseline-file".equals(key)) {
                        options.baselineFilePath = value;
                        continue;
                    } else if ("--url-rate".equals(key)) {
                        try {
                            int rate = Integer.parseInt(value);
                            if (rate <= 0) throw new IllegalArgumentException("--url-rate must be a positive integer");
                            options.urlRatePerSecond = rate;
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid --url-rate value: " + value);
                        }
                        continue;
                    } else if ("--download-rate".equals(key)) {
                        try {
                            int rate = Integer.parseInt(value);
                            if (rate <= 0) throw new IllegalArgumentException("--download-rate must be a positive integer");
                            options.downloadRatePerSecond = rate;
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid --download-rate value: " + value);
                        }
                        continue;
                    } else {
                        throw new IllegalArgumentException("Unknown argument: " + arg);
                    }
                }
            }
            
            switch (arg) {
                case "--index-limit":
                case "-l":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    try {
                        String value = args[++i].trim(); // Handle extra spaces
                        options.indexLimit = Integer.parseInt(value);
                        if (options.indexLimit < 0) {
                            throw new IllegalArgumentException("Index limit must be a positive integer");
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid index limit value: " + args[i].trim());
                    }
                    break;
                    
                case "--verbose":
                case "-v":
                    options.verbose = true;
                    break;
                    
                case "--help":
                case "-h":
                    options.showHelp = true;
                    break;
                    
                case "--force-restart":
                    options.forceRestart = true;
                    break;
                    
                case "--incremental":
                case "-i":
                    options.incrementalUpdates = true;
                    options.incrementalDownloads = true;
                    options.incrementalProcessing = true;
                    break;
                case "--baseline-file":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    options.baselineFilePath = args[++i];
                    break;
                
                case "--url-rate":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    try {
                        String value = args[++i].trim();
                        int rate = Integer.parseInt(value);
                        if (rate <= 0) throw new IllegalArgumentException("--url-rate must be a positive integer");
                        options.urlRatePerSecond = rate;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid --url-rate value: " + args[i].trim());
                    }
                    break;
                
                case "--download-rate":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    try {
                        String value = args[++i].trim();
                        int rate = Integer.parseInt(value);
                        if (rate <= 0) throw new IllegalArgumentException("--download-rate must be a positive integer");
                        options.downloadRatePerSecond = rate;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid --download-rate value: " + args[i].trim());
                    }
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
        
        return options;
    }
    
    /**
     * Prints usage information to the console
     */
    public static void printUsage() {
        System.out.println("IAPD Parser - Investment Adviser Public Disclosure Parser");
        System.out.println();
        System.out.println("Usage: java IAFirmSECParserRefactored [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -l, --index-limit <number>    Set the maximum number of firms to process (for testing)");
        System.out.println("                                (default: " + Integer.MAX_VALUE + " - no limit)");
        System.out.println("  -v, --verbose                 Enable verbose logging");
        System.out.println("      --force-restart           Ignore existing files and start fresh");
        System.out.println("  -i, --incremental             Enable incremental updates for both downloads and processing");
        System.out.println("      --baseline-file <path>    Specify baseline IAPD_Data.csv file for incremental comparison");
        System.out.println("      --url-rate <n>            Limit brochure urls info api calls per second (overrides config)");
        System.out.println("      --download-rate <n>       Limit brochure downloads per second (overrides config)");
        System.out.println("  -h, --help                    Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java IAFirmSECParserRefactored");
        System.out.println("  java IAFirmSECParserRefactored --index-limit 1000");
        System.out.println("  java IAFirmSECParserRefactored -l 500 --verbose");
        System.out.println("  java IAFirmSECParserRefactored --verbose--incremental");
    System.out.println("  java IAFirmSECParserRefactored --url-rate 2 --download-rate 5");
    }
    
    // Getters
    public int getIndexLimit() {
        return indexLimit;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public boolean isShowHelp() {
        return showHelp;
    }
    
    public boolean isForceRestart() {
        return forceRestart;
    }
    
    public boolean isIncrementalProcessing() {
        return incrementalProcessing;
    }

    public boolean isIncrementalUpdates() {
        return incrementalUpdates;
    }
    public boolean isIncrementalDownloads() {
        return incrementalDownloads;
    }
    
    public String getBaselineFilePath() {
        return baselineFilePath;
    }
    
    public Integer getURLRatePerSecond() {
        return urlRatePerSecond;
    }
    
    public Integer getDownloadRatePerSecond() {
        return downloadRatePerSecond;
    }
    
    // Setters for testing or programmatic configuration
    public void setIndexLimit(int indexLimit) {
        this.indexLimit = indexLimit;
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    public void setForceRestart(boolean forceRestart) {
        this.forceRestart = forceRestart;
    }
    
    public void setIncrementalProcessing(boolean incrementalProcessing) {
        this.incrementalProcessing = incrementalProcessing;
        this.incrementalUpdates = incrementalProcessing;
        this.incrementalDownloads = incrementalProcessing;
    }
    
    public void setBaselineFilePath(String baselineFilePath) {
        this.baselineFilePath = baselineFilePath;
    }
    
    
    @Override
    public String toString() {
        return "CommandLineOptions{" +
                "indexLimit=" + indexLimit +
                ", verbose=" + verbose +
                ", showHelp=" + showHelp +
                ", forceRestart=" + forceRestart +
                ", incrementalProcessing=" + incrementalProcessing +
                ", incrementalUpdates=" + incrementalUpdates +
                ", incrementalDownloads=" + incrementalDownloads +
                ", baselineFilePath='" + baselineFilePath + '\'' +
                ", urlRatePerSecond=" + urlRatePerSecond +
                ", downloadRatePerSecond=" + downloadRatePerSecond +
                '}';
    }
}
