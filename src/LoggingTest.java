/**
 * Simple test to verify log4j logging functionality
 */
public class LoggingTest {
    
    public static void main(String[] args) {
        ProcessingLogger.logInfo("Testing ProcessingLogger with log4j...");
        
        // Test basic logging methods
        ProcessingLogger.logInfo("This is an info message - logging test started");
        ProcessingLogger.logWarning("This is a warning message - testing warning level");
        ProcessingLogger.logError("This is an error message without exception", null);
        
        // Test with exception
        try {
            throw new RuntimeException("Test exception for logging");
        } catch (Exception e) {
            ProcessingLogger.logError("This is an error message with exception", e);
        }
        
        // Test counter methods
        ProcessingLogger.incrementTotalFirmsProcessed();
        ProcessingLogger.incrementBrochureUrlFailures();
        ProcessingLogger.incrementBrochureDownloadFailures();
        ProcessingLogger.incrementFilenameParsingFailures();
        
        // Test summary
        ProcessingLogger.printProcessingSummary();
        
        // Test reset
        ProcessingLogger.resetCounters();
        ProcessingLogger.printProcessingSummary();
        
        System.out.println("Logging test completed. Check the log file at: " + System.getProperty("user.dir") + "/Data/Logs/processing.log");
    }
}
