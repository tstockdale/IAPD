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
     * @return The result of the operation
     * @throws RuntimeException if all retries failed
     */
    public static <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long retryDelayMs, String operationName) {
        if (operation == null) {
            throw new NullPointerException("Operation cannot be null");
        }
        
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
                
                // Only retry if this is a transient exception and we haven't exceeded max retries
                if (attempt <= maxRetries && isTransientException(e)) {
                    ProcessingLogger.logWarning("Operation '" + operationName + "' failed on attempt " + attempt + 
                                               ", retrying in " + retryDelayMs + "ms. Error: " + e.getMessage());
                    
                    if (retryDelayMs > 0) {
                        try {
                            Thread.sleep(retryDelayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            ProcessingLogger.logError("Retry interrupted for operation: " + operationName, ie);
                            break;
                        }
                    }
                } else {
                    // Either exceeded max retries or non-transient exception
                    if (!isTransientException(e)) {
                        ProcessingLogger.logError("Operation '" + operationName + "' failed with non-transient exception", e);
                    } else {
                        ProcessingLogger.logError("Operation '" + operationName + "' failed after " + (maxRetries + 1) + " attempts", e);
                    }
                    break;
                }
            }
        }
        
        // Re-throw the last exception
        if (lastException instanceof RuntimeException) {
            throw (RuntimeException) lastException;
        } else {
            throw new RuntimeException(lastException);
        }
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
        if (e == null) {
            return false;
        }
        
        // Check if it's a known transient exception type
        if (e instanceof java.io.IOException ||
            e instanceof java.net.SocketTimeoutException ||
            e instanceof java.net.ConnectException ||
            e instanceof java.net.UnknownHostException) {
            return true;
        }
        
        // Check if it's a RuntimeException wrapping a transient exception
        if (e instanceof RuntimeException && e.getCause() instanceof java.io.IOException) {
            return true;
        }
        
        // Check message for transient keywords (but exclude non-transient exceptions)
        if (e instanceof IllegalArgumentException || 
            e instanceof IllegalStateException ||
            e instanceof NullPointerException) {
            return false; // These are never transient
        }
        
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            // Only consider it transient if it has network-related keywords
            // but exclude explicit "non-transient" messages
            if (lowerMessage.contains("non-transient")) {
                return false;
            }
            return lowerMessage.contains("timeout") ||
                   lowerMessage.contains("connection") ||
                   lowerMessage.contains("network") ||
                   lowerMessage.contains("socket") ||
                   lowerMessage.contains("unreachable") ||
                   lowerMessage.contains("reset") ||
                   lowerMessage.contains("unavailable");
        }
        
        return false;
    }
}
