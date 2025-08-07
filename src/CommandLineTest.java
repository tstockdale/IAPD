/**
 * Simple test class to demonstrate command line argument parsing
 */
public class CommandLineTest {
    
    public static void main(String[] args) {
        System.out.println("=== Command Line Options Test ===");
        
        try {
            // Parse command line arguments
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            // Show help if requested
            if (options.isShowHelp()) {
                CommandLineOptions.printUsage();
                return;
            }
            
            // Display parsed options
            System.out.println("Parsed options:");
            System.out.println("  Index Limit: " + (options.getIndexLimit() == Integer.MAX_VALUE ? "unlimited" : options.getIndexLimit()));
            System.out.println("  Verbose: " + options.isVerbose());
            System.out.println();
            
            // Simulate what would happen in XML processing
            System.out.println("Simulating XML processing with index limit: " + options.getIndexLimit());
            int processedCount = 0;
            for (int i = 1; i <= 10 && processedCount < options.getIndexLimit(); i++) {
                System.out.println("Processing firm " + i);
                processedCount++;
            }
            
            if (processedCount >= options.getIndexLimit() && options.getIndexLimit() != Integer.MAX_VALUE) {
                System.out.println("Reached index limit of " + options.getIndexLimit() + " firms. Processing stopped.");
            } else {
                System.out.println("Processed all available firms (" + processedCount + ").");
            }
            
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            CommandLineOptions.printUsage();
            System.exit(1);
        }
    }
}
