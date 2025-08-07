/**
 * Test class to demonstrate the ProcessingContext architecture
 */
public class ProcessingContextTest {
    
    public static void main(String[] args) {
        System.out.println("=== ProcessingContext Architecture Test ===");
        
        try {
            // Test 1: Create context from command line arguments
            System.out.println("\n1. Testing command line argument parsing:");
            ProcessingContext context1 = testCommandLineContext(new String[]{"--index-limit", "5", "--verbose"});
            context1.logCurrentState();
            
            // Test 2: Create context using builder pattern
            System.out.println("\n2. Testing builder pattern:");
            ProcessingContext context2 = ProcessingContext.builder()
                    .indexLimit(10)
                    .verbose(true)
                    .retryCount(5)
                    .skipBrochureDownload(true)
                    .configSource("test-builder")
                    .build();
            
            System.out.println("Built context: " + context2.toString());
            
            // Test 3: Test runtime state tracking
            System.out.println("\n3. Testing runtime state tracking:");
            simulateProcessing(context2);
            
            // Test 4: Test configuration manager
            System.out.println("\n4. Testing configuration manager:");
            ConfigurationManager configManager = new ConfigurationManager();
            ProcessingContext context3 = configManager.buildContext(new String[]{"-l", "3", "-v"});
            configManager.printEffectiveConfiguration(context3);
            
            // Test 5: Test utility methods
            System.out.println("\n5. Testing utility methods:");
            testUtilityMethods(context3);
            
        } catch (Exception e) {
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static ProcessingContext testCommandLineContext(String[] args) {
        try {
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            return ProcessingContext.fromCommandLineOptions(options);
        } catch (Exception e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            return ProcessingContext.builder().build();
        }
    }
    
    private static void simulateProcessing(ProcessingContext context) {
        context.setCurrentPhase(ProcessingPhase.PARSING_XML);
        context.setCurrentProcessingFile("test_file.xml");
        
        // Simulate processing some firms
        for (int i = 0; i < 7; i++) {
            context.incrementProcessedFirms();
            if (i % 2 == 0) {
                context.incrementSuccessfulDownloads();
            } else {
                context.incrementFailedDownloads();
            }
            
            if (i % 3 == 0) {
                context.incrementBrochuresProcessed();
            }
            
            // Simulate some processing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        context.setCurrentPhase(ProcessingPhase.COMPLETED);
        context.logCurrentState();
    }
    
    private static void testUtilityMethods(ProcessingContext context) {
        System.out.println("Has reached index limit: " + context.hasReachedIndexLimit());
        System.out.println("Processing rate: " + String.format("%.2f", context.getProcessingRate()) + " firms/sec");
        System.out.println("Elapsed time: " + (context.getElapsedTimeMs() / 1000.0) + " seconds");
        System.out.println("Index limit: " + (context.getIndexLimit() == Integer.MAX_VALUE ? "unlimited" : context.getIndexLimit()));
        System.out.println("Config source: " + context.getConfigSource());
        System.out.println("Created at: " + context.getCreatedAt());
    }
}
