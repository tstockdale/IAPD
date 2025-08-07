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
    
    /**
     * Parses command line arguments and returns a CommandLineOptions object
     * @param args command line arguments
     * @return parsed CommandLineOptions
     * @throws IllegalArgumentException if invalid arguments are provided
     */
    public static CommandLineOptions parseArgs(String[] args) throws IllegalArgumentException {
        CommandLineOptions options = new CommandLineOptions();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case "--index-limit":
                case "-l":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    try {
                        options.indexLimit = Integer.parseInt(args[++i]);
                        if (options.indexLimit <= 0) {
                            throw new IllegalArgumentException("Index limit must be a positive integer");
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid index limit value: " + args[i]);
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
                '}';
    }
}
