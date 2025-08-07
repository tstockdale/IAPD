/**
 * Command line options parser for the IAPD Parser application
 * Provides a centralized way to handle command line arguments with extensibility for future options
 */
public class CommandLineOptions {
    
    private int indexLimit = Integer.MAX_VALUE;
    private boolean verbose = false;
    private boolean showHelp = false;
    
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
        System.out.println("  -h, --help                    Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java IAFirmSECParserRefactored");
        System.out.println("  java IAFirmSECParserRefactored --index-limit 1000");
        System.out.println("  java IAFirmSECParserRefactored -l 500 --verbose");
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
    
    // Setters for testing or programmatic configuration
    public void setIndexLimit(int indexLimit) {
        this.indexLimit = indexLimit;
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public String toString() {
        return "CommandLineOptions{" +
                "indexLimit=" + indexLimit +
                ", verbose=" + verbose +
                ", showHelp=" + showHelp +
                '}';
    }
}
