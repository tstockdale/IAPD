import java.util.function.Supplier;

/**
 * Utility class for implementing retry logic for transient failures
 */
public class RetryUtils {
    
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;
    
    /**
     * Executes an operation with retry logic
     * @param operation The operation to execute
     * @param maxRetries Maximum number of retry attempts
     * @param retryDelayMs Delay between retries in milliseconds
     * @param operationName Name of the operation for logging
     * @return The result of the operation, or null if all retries failed
     */
    public static <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long retryDelayMs, String operationName) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                T result = operation.get();
                if (attempt > 1) {
                    ProcessingLogger.logInfo("Operation '" + operationName + "' succeeded on attempt " + attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                
                if (attempt <= maxRetries) {
                    ProcessingLogger.logWarning("Operation '" + operationName + "' failed on attempt " + attempt + 
                                               ", retrying in " + retryDelayMs + "ms. Error: " + e.getMessage());
                    
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        ProcessingLogger.logError("Retry interrupted for operation: " + operationName, ie);
                        break;
                    }
                } else {
                    ProcessingLogger.logError("Operation '" + operationName + "' failed after " + (maxRetries + 1) + " attempts", e);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Executes an operation with default retry settings
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        return executeWithRetry(operation, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS, operationName);
    }
    
    /**
     * Checks if an exception is likely to be transient (network-related)
     */
    public static boolean isTransientException(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("timeout") ||
               lowerMessage.contains("connection") ||
               lowerMessage.contains("network") ||
               lowerMessage.contains("socket") ||
               lowerMessage.contains("unreachable") ||
               e instanceof java.net.SocketTimeoutException ||
               e instanceof java.net.ConnectException ||
               e instanceof java.net.UnknownHostException;
    }
}
