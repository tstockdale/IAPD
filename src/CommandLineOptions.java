/**
 * Command line options parser for the IAPD Parser application
 * Provides a centralized way to handle command line arguments with extensibility for future options
 */
public class CommandLineOptions {
    
    private int indexLimit = Integer.MAX_VALUE;
    private boolean verbose = false;
    private boolean showHelp = false;
    private boolean resumeEnabled = false;
    private boolean resumeDownloads = false;
    private boolean resumeProcessing = false;
    private boolean validatePdfs = true;
    private boolean forceRestart = false;
    private boolean incrementalUpdates = false;
    private boolean incrementalDownloads = false;
    private boolean incrementalProcessing = false;
    private String baselineFilePath = null;
    private String monthName = null;
    // Optional overrides for rate limits (null means not provided on CLI)
    private Integer xmlRatePerSecond = null;
    private Integer downloadRatePerSecond = null;
    
    // Valid month names for validation
    private static final String[] VALID_MONTHS = {
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december"
    };
    
    /**
     * Validates if the provided month name is valid
     * @param monthName the month name to validate (case insensitive)
     * @return true if valid, false otherwise
     */
    private static boolean isValidMonth(String monthName) {
        if (monthName == null || monthName.trim().isEmpty()) {
            return false;
        }
        
        String normalizedMonth = monthName.trim().toLowerCase();
        for (String validMonth : VALID_MONTHS) {
            if (validMonth.equals(normalizedMonth)) {
                return true;
            }
        }
        return false;
    }
    
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
                    } else if ("--month".equals(key)) {
                        if (!isValidMonth(value)) {
                            throw new IllegalArgumentException("Invalid month name: " + value + ". Valid months are: january, february, march, april, may, june, july, august, september, october, november, december");
                        }
                        options.monthName = value.trim().toLowerCase();
                        continue;
                    } else if ("--xml-rate".equals(key)) {
                        try {
                            int rate = Integer.parseInt(value);
                            if (rate <= 0) throw new IllegalArgumentException("--xml-rate must be a positive integer");
                            options.xmlRatePerSecond = rate;
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid --xml-rate value: " + value);
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
                    
                case "--resume":
                case "-r":
                    options.resumeEnabled = true;
                    options.resumeDownloads = true;
                    options.resumeProcessing = true;
                    break;
                    
                case "--resume-downloads":
                    options.resumeDownloads = true;
                    break;
                    
                case "--resume-processing":
                    options.resumeProcessing = true;
                    break;
                    
                case "--validate-pdfs":
                    options.validatePdfs = true;
                    break;
                    
                case "--no-validate-pdfs":
                    options.validatePdfs = false;
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
                    
                case "--incremental-downloads":
                    options.incrementalDownloads = true;
                    break;
                    
                case "--incremental-processing":
                    options.incrementalProcessing = true;
                    break;
                    
                case "--baseline-file":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    options.baselineFilePath = args[++i];
                    break;
                
                case "--xml-rate":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    try {
                        String value = args[++i].trim();
                        int rate = Integer.parseInt(value);
                        if (rate <= 0) throw new IllegalArgumentException("--xml-rate must be a positive integer");
                        options.xmlRatePerSecond = rate;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid --xml-rate value: " + args[i].trim());
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
                    
                case "--month":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    String monthValue = args[++i].trim();
                    if (!isValidMonth(monthValue)) {
                        throw new IllegalArgumentException("Invalid month name: " + monthValue + ". Valid months are: january, february, march, april, may, june, july, august, september, october, november, december");
                    }
                    options.monthName = monthValue.toLowerCase();
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
        
        // Validate that month option is only used with incremental mode
        if (options.monthName != null && !options.isIncrementalUpdates() && !options.isIncrementalDownloads() && !options.isIncrementalProcessing()) {
            throw new IllegalArgumentException("--month option can only be used with incremental mode (--incremental, --incremental-downloads, or --incremental-processing)");
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
        System.out.println("  -l, --index-limit <number>    Set the maximum number of firms to process");
        System.out.println("                                (default: " + Integer.MAX_VALUE + " - no limit)");
        System.out.println("  -v, --verbose                 Enable verbose logging");
        System.out.println("  -r, --resume                  Enable resume for both downloads and processing");
        System.out.println("      --resume-downloads        Enable resume for brochure downloads only");
        System.out.println("      --resume-processing       Enable resume for brochure processing only");
        System.out.println("      --validate-pdfs           Validate existing PDF files during resume (default)");
        System.out.println("      --no-validate-pdfs        Skip PDF validation during resume");
        System.out.println("      --force-restart           Ignore existing files and start fresh");
        System.out.println("  -i, --incremental             Enable incremental updates for both downloads and processing");
        System.out.println("      --incremental-downloads   Enable incremental updates for downloads only");
        System.out.println("      --incremental-processing  Enable incremental updates for processing only");
        System.out.println("      --baseline-file <path>    Specify baseline IAPD_Data.csv file for incremental comparison");
        System.out.println("      --month <month>           Specify month name for incremental updates (requires incremental mode)");
        System.out.println("                                Valid months: january, february, march, april, may, june,");
        System.out.println("                                july, august, september, october, november, december");
    System.out.println("      --xml-rate <n>            Limit firms parsed from XML per second (overrides config)");
    System.out.println("      --download-rate <n>       Limit brochure downloads per second (overrides config)");
        System.out.println("  -h, --help                    Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java IAFirmSECParserRefactored");
        System.out.println("  java IAFirmSECParserRefactored --index-limit 1000");
        System.out.println("  java IAFirmSECParserRefactored -l 500 --verbose");
        System.out.println("  java IAFirmSECParserRefactored --resume --index-limit 1000");
        System.out.println("  java IAFirmSECParserRefactored --resume-downloads --validate-pdfs");
        System.out.println("  java IAFirmSECParserRefactored --incremental --baseline-file ./Data/Output/IAPD_Data.csv");
        System.out.println("  java IAFirmSECParserRefactored --incremental-downloads --baseline-file ./Data/Output/IAPD_Data.csv");
        System.out.println("  java IAFirmSECParserRefactored --incremental --month january");
        System.out.println("  java IAFirmSECParserRefactored --incremental --baseline-file ./Data/Output/IAPD_Data.csv --month december");
    System.out.println("  java IAFirmSECParserRefactored --xml-rate 2 --download-rate 5");
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
    
    public boolean isResumeEnabled() {
        return resumeEnabled;
    }
    
    public boolean isResumeDownloads() {
        return resumeDownloads;
    }
    
    public boolean isResumeProcessing() {
        return resumeProcessing;
    }
    
    public boolean isValidatePdfs() {
        return validatePdfs;
    }
    
    public boolean isForceRestart() {
        return forceRestart;
    }
    
    public boolean isIncrementalUpdates() {
        return incrementalUpdates;
    }
    
    public boolean isIncrementalDownloads() {
        return incrementalDownloads;
    }
    
    public boolean isIncrementalProcessing() {
        return incrementalProcessing;
    }
    
    public String getBaselineFilePath() {
        return baselineFilePath;
    }
    
    public String getMonthName() {
        return monthName;
    }
    
    public Integer getXmlRatePerSecond() {
        return xmlRatePerSecond;
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
    
    public void setResumeEnabled(boolean resumeEnabled) {
        this.resumeEnabled = resumeEnabled;
    }
    
    public void setResumeDownloads(boolean resumeDownloads) {
        this.resumeDownloads = resumeDownloads;
    }
    
    public void setResumeProcessing(boolean resumeProcessing) {
        this.resumeProcessing = resumeProcessing;
    }
    
    public void setValidatePdfs(boolean validatePdfs) {
        this.validatePdfs = validatePdfs;
    }
    
    public void setForceRestart(boolean forceRestart) {
        this.forceRestart = forceRestart;
    }
    
    public void setIncrementalUpdates(boolean incrementalUpdates) {
        this.incrementalUpdates = incrementalUpdates;
    }
    
    public void setIncrementalDownloads(boolean incrementalDownloads) {
        this.incrementalDownloads = incrementalDownloads;
    }
    
    public void setIncrementalProcessing(boolean incrementalProcessing) {
        this.incrementalProcessing = incrementalProcessing;
    }
    
    public void setBaselineFilePath(String baselineFilePath) {
        this.baselineFilePath = baselineFilePath;
    }
    
    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }
    
    @Override
    public String toString() {
        return "CommandLineOptions{" +
                "indexLimit=" + indexLimit +
                ", verbose=" + verbose +
                ", showHelp=" + showHelp +
                ", resumeEnabled=" + resumeEnabled +
                ", resumeDownloads=" + resumeDownloads +
                ", resumeProcessing=" + resumeProcessing +
                ", validatePdfs=" + validatePdfs +
                ", forceRestart=" + forceRestart +
                ", incrementalUpdates=" + incrementalUpdates +
                ", incrementalDownloads=" + incrementalDownloads +
                ", incrementalProcessing=" + incrementalProcessing +
                ", baselineFilePath='" + baselineFilePath + '\'' +
                ", monthName='" + monthName + '\'' +
                ", xmlRatePerSecond=" + xmlRatePerSecond +
                ", downloadRatePerSecond=" + downloadRatePerSecond +
                '}';
    }
}
