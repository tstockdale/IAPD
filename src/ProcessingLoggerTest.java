/**
 * Test class to verify ProcessingLogger programmatic configuration
 */
public class ProcessingLoggerTest {
    
    public static void main(String[] args) {
        System.out.println("Testing ProcessingLogger programmatic configuration...");
        
        // Test basic logging methods
        ProcessingLogger.logInfo("This is an info message - testing programmatic configuration");
        ProcessingLogger.logWarning("This is a warning message - testing programmatic configuration");
        ProcessingLogger.logError("This is an error message - testing programmatic configuration", null);
        
        // Test with exception
        try {
            throw new RuntimeException("Test exception for logging");
        } catch (Exception e) {
            ProcessingLogger.logError("Testing error logging with exception", e);
        }
        
        // Test counter methods
        ProcessingLogger.incrementTotalFirmsProcessed();
        ProcessingLogger.incrementFirmsWithoutBrochures();
        ProcessingLogger.incrementBrochureDownloadFailures();
        ProcessingLogger.incrementFilenameParsingFailures();
        
        // Print summary
        ProcessingLogger.printProcessingSummary();
        
        // Reset counters
        ProcessingLogger.resetCounters();
        
        System.out.println("ProcessingLogger test completed. Check console output and log file in " + Config.LOG_PATH + "/processing.log");
    }
}
