import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;

/**
 * Comprehensive JUnit 5 test suite for RetryUtils
 * Tests retry logic, exception handling, and edge cases
 */
public class RetryUtilsTest {
    
    @Nested
    @DisplayName("Basic Retry Logic Tests")
    class BasicRetryLogicTests {
        
        @Test
        @DisplayName("Should succeed on first attempt")
        void testSucceedOnFirstAttempt() {
            Supplier<String> operation = () -> "success";
            
            String result = RetryUtils.executeWithRetry(operation, 3, 100, "test operation");
            
            assertEquals("success", result);
        }
        
        @Test
        @DisplayName("Should succeed after retries")
        void testSucceedAfterRetries() {
            AtomicInteger attemptCount = new AtomicInteger(0);
            Supplier<String> operation = () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    throw new RuntimeException(new IOException("Transient failure"));
                }
                return "success after retries";
            };
            
            String result = RetryUtils.executeWithRetry(operation, 5, 50, "test operation");
            
            assertEquals("success after retries", result);
            assertEquals(3, attemptCount.get());
        }
        
        @Test
        @DisplayName("Should fail after max retries exceeded")
        void testFailAfterMaxRetriesExceeded() {
            AtomicInteger attemptCount = new AtomicInteger(0);
            Supplier<String> operation = () -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException(new IOException("Transient failure that always fails"));
            };
            
            assertThrows(RuntimeException.class, () -> {
                RetryUtils.executeWithRetry(operation, 3, 50, "test operation");
            });
            
            assertEquals(4, attemptCount.get()); // Initial attempt + 3 retries
        }
        
        @Test
        @DisplayName("Should use default retry parameters")
        void testDefaultRetryParameters() {
            AtomicInteger attemptCount = new AtomicInteger(0);
            Supplier<String> operation = () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 2) {
                    throw new RuntimeException("Transient failure");
                }
                return "success with defaults";
            };
            
            String result = RetryUtils.executeWithRetry(operation, "test operation");
            
            assertEquals("success with defaults", result);
            assertEquals(2, attemptCount.get());
        }
    }
    
    @Nested
    @DisplayName("Exception Classification Tests")
    class ExceptionClassificationTests {
        
        @Test
        @DisplayName("Should identify IOException as transient")
        void testIOExceptionIsTransient() {
            IOException ioException = new IOException("Network error");
            assertTrue(RetryUtils.isTransientException(ioException));
        }
        
        @Test
        @DisplayName("Should identify SocketTimeoutException as transient")
        void testSocketTimeoutExceptionIsTransient() {
            SocketTimeoutException timeoutException = new SocketTimeoutException("Connection timeout");
            assertTrue(RetryUtils.isTransientException(timeoutException));
        }
        
        @Test
        @DisplayName("Should identify ConnectException as transient")
        void testConnectExceptionIsTransient() {
            ConnectException connectException = new ConnectException("Connection refused");
            assertTrue(RetryUtils.isTransientException(connectException));
        }
        
        @Test
        @DisplayName("Should identify RuntimeException with network keywords as transient")
        void testRuntimeExceptionWithNetworkKeywordsIsTransient() {
            RuntimeException networkException = new RuntimeException("Connection reset");
            assertTrue(RetryUtils.isTransientException(networkException));
            
            RuntimeException timeoutException = new RuntimeException("Request timeout");
            assertTrue(RetryUtils.isTransientException(timeoutException));
            
            RuntimeException unavailableException = new RuntimeException("Service unavailable");
            assertTrue(RetryUtils.isTransientException(unavailableException));
        }
        
        @Test
        @DisplayName("Should not identify generic RuntimeException as transient")
        void testGenericRuntimeExceptionIsNotTransient() {
            RuntimeException genericException = new RuntimeException("Generic error");
            assertFalse(RetryUtils.isTransientException(genericException));
        }
        
        @Test
        @DisplayName("Should not identify IllegalArgumentException as transient")
        void testIllegalArgumentExceptionIsNotTransient() {
            IllegalArgumentException argException = new IllegalArgumentException("Invalid argument");
            assertFalse(RetryUtils.isTransientException(argException));
        }
        
        @Test
        @DisplayName("Should handle null exception gracefully")
        void testNullExceptionHandling() {
            assertFalse(RetryUtils.isTransientException(null));
        }
    }
    
    @Nested
    @DisplayName("Retry Behavior Tests")
    class RetryBehaviorTests {
        
        @Test
        @DisplayName("Should retry only transient exceptions")
        void testRetryOnlyTransientExceptions() {
            AtomicInteger attemptCount = new AtomicInteger(0);
            Supplier<String> operation = () -> {
                attemptCount.incrementAndGet();
                throw new IllegalArgumentException("Non-transient error");
            };
            
            assertThrows(RuntimeException.class, () -> {
                RetryUtils.executeWithRetry(operation, 3, 50, "test operation");
            });
            
            assertEquals(1, attemptCount.get()); // Should not retry non-transient exceptions
        }
        
        @Test
        @DisplayName("Should respect retry delay")
        void testRetryDelay() {
            AtomicInteger attemptCount = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();
            
            Supplier<String> operation = () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    throw new RuntimeException(new IOException("Transient network error"));
                }
                return "success";
            };
            
            String result = RetryUtils.executeWithRetry(operation, 3, 100, "test operation");
            long endTime = System.currentTimeMillis();
            
            assertEquals("success", result);
            assertEquals(3, attemptCount.get());
            // Should have at least 2 delays of 100ms each (between 3 attempts)
            assertTrue(endTime - startTime >= 200);
        }
        
        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 5, 10})
        @DisplayName("Should handle various max retry values")
        void testVariousMaxRetryValues(int maxRetries) {
            AtomicInteger attemptCount = new AtomicInteger(0);
            Supplier<String> operation = () -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException(new IOException("Always fails"));
            };
            
            assertThrows(RuntimeException.class, () -> {
                RetryUtils.executeWithRetry(operation, maxRetries, 10, "test operation");
            });
            
            assertEquals(maxRetries + 1, attemptCount.get()); // Initial attempt + retries
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle null operation gracefully")
        void testNullOperation() {
            assertThrows(NullPointerException.class, () -> {
                RetryUtils.executeWithRetry(null, 3, 100, "test operation");
            });
        }
        
        @Test
        @DisplayName("Should handle null operation name gracefully")
        void testNullOperationName() {
            Supplier<String> operation = () -> "success";
            
            assertDoesNotThrow(() -> {
                String result = RetryUtils.executeWithRetry(operation, 3, 100, null);
                assertEquals("success", result);
            });
        }
        
        @Test
        @DisplayName("Should handle empty operation name gracefully")
        void testEmptyOperationName() {
            Supplier<String> operation = () -> "success";
            
            assertDoesNotThrow(() -> {
                String result = RetryUtils.executeWithRetry(operation, 3, 100, "");
                assertEquals("success", result);
            });
        }
        
        @Test
        @DisplayName("Should handle negative retry delay")
        void testNegativeRetryDelay() {
            Supplier<String> operation = () -> "success";
            
            assertDoesNotThrow(() -> {
                String result = RetryUtils.executeWithRetry(operation, 3, -100, "test operation");
                assertEquals("success", result);
            });
        }
        
        @Test
        @DisplayName("Should handle zero retry delay")
        void testZeroRetryDelay() {
            AtomicInteger attemptCount = new AtomicInteger(0);
            Supplier<String> operation = () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 2) {
                    throw new RuntimeException(new IOException("Transient error"));
                }
                return "success";
            };
            
            String result = RetryUtils.executeWithRetry(operation, 3, 0, "test operation");
            
            assertEquals("success", result);
            assertEquals(2, attemptCount.get());
        }
        
        @Test
        @DisplayName("Should handle operation returning null")
        void testOperationReturningNull() {
            Supplier<String> operation = () -> null;
            
            String result = RetryUtils.executeWithRetry(operation, 3, 100, "test operation");
            
            assertNull(result);
        }
        
        @Test
        @DisplayName("Should handle different return types")
        void testDifferentReturnTypes() {
            // Test Integer return type
            Supplier<Integer> intOperation = () -> 42;
            Integer intResult = RetryUtils.executeWithRetry(intOperation, 3, 100, "int operation");
            assertEquals(Integer.valueOf(42), intResult);
            
            // Test Boolean return type
            Supplier<Boolean> boolOperation = () -> true;
            Boolean boolResult = RetryUtils.executeWithRetry(boolOperation, 3, 100, "bool operation");
            assertEquals(Boolean.TRUE, boolResult);
        }
    }
    
    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("Should handle concurrent retry operations")
        void testConcurrentRetryOperations() throws InterruptedException {
            final int numberOfThreads = 10;
            final AtomicInteger successCount = new AtomicInteger(0);
            Thread[] threads = new Thread[numberOfThreads];
            
            for (int i = 0; i < numberOfThreads; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    AtomicInteger attemptCount = new AtomicInteger(0);
                    Supplier<String> operation = () -> {
                        int attempt = attemptCount.incrementAndGet();
                        if (attempt < 2) {
                            throw new RuntimeException(new IOException("Transient error in thread " + threadId));
                        }
                        return "success from thread " + threadId;
                    };
                    
                    try {
                        String result = RetryUtils.executeWithRetry(operation, 3, 50, "thread " + threadId);
                        if (result.contains("success")) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Handle any unexpected exceptions
                    }
                });
            }
            
            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }
            
            assertEquals(numberOfThreads, successCount.get());
        }
    }
    
    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("Should complete fast operations quickly")
        void testFastOperationPerformance() {
            Supplier<String> fastOperation = () -> "fast result";
            
            long startTime = System.currentTimeMillis();
            String result = RetryUtils.executeWithRetry(fastOperation, 3, 100, "fast operation");
            long endTime = System.currentTimeMillis();
            
            assertEquals("fast result", result);
            assertTrue(endTime - startTime < 100); // Should complete in less than 100ms
        }
        
        @Test
        @DisplayName("Should handle operations with varying execution times")
        void testVaryingExecutionTimes() {
            AtomicInteger attemptCount = new AtomicInteger(0);
            Supplier<String> varyingOperation = () -> {
                int attempt = attemptCount.incrementAndGet();
                try {
                    Thread.sleep(attempt * 10); // Increasing delay with each attempt
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (attempt < 3) {
                    throw new RuntimeException(new IOException("Transient error"));
                }
                return "success after varying delays";
            };
            
            String result = RetryUtils.executeWithRetry(varyingOperation, 5, 50, "varying operation");
            
            assertEquals("success after varying delays", result);
            assertEquals(3, attemptCount.get());
        }
    }
}
