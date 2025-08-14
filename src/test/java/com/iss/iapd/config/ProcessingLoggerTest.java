package com.iss.iapd.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.iss.iapd.config.ProcessingLogger;

/**
 * Comprehensive JUnit 5 test suite for ProcessingLogger
 * Tests logging functionality, counter operations, and configuration
 */
public class ProcessingLoggerTest {
    
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream standardOut = System.out;
    
    @BeforeEach
    void setUp() {
        // Reset counters before each test
        ProcessingLogger.resetCounters();
        // Capture system output for testing
        System.setOut(new PrintStream(outputStreamCaptor));
    }
    
    @AfterEach
    void tearDown() {
        // Restore standard output
        System.setOut(standardOut);
        // Reset counters after each test
        ProcessingLogger.resetCounters();
    }
    
    @Nested
    @DisplayName("Counter Operations Tests")
    class CounterOperationsTests {
        
        @Test
        @DisplayName("Should increment total firms processed counter")
        void testIncrementTotalFirmsProcessed() {
            assertEquals(0, ProcessingLogger.getTotalFirmsProcessed());
            
            ProcessingLogger.incrementTotalFirmsProcessed();
            assertEquals(1, ProcessingLogger.getTotalFirmsProcessed());
            
            ProcessingLogger.incrementTotalFirmsProcessed();
            assertEquals(2, ProcessingLogger.getTotalFirmsProcessed());
        }
        
        @Test
        @DisplayName("Should increment firms without brochures counter")
        void testIncrementFirmsWithoutBrochures() {
            assertEquals(0, ProcessingLogger.getFirmsWithoutBrochures());
            
            ProcessingLogger.incrementFirmsWithoutBrochures();
            assertEquals(1, ProcessingLogger.getFirmsWithoutBrochures());
            
            ProcessingLogger.incrementFirmsWithoutBrochures();
            assertEquals(2, ProcessingLogger.getFirmsWithoutBrochures());
        }
        
        @Test
        @DisplayName("Should increment firms with brochures counter")
        void testIncrementFirmsWithBrochures() {
            assertEquals(0, ProcessingLogger.getFirmsWithBrochures());
            
            ProcessingLogger.incrementFirmsWithBrochures();
            assertEquals(1, ProcessingLogger.getFirmsWithBrochures());
            
            ProcessingLogger.incrementFirmsWithBrochures();
            assertEquals(2, ProcessingLogger.getFirmsWithBrochures());
        }
        
        @Test
        @DisplayName("Should increment brochure download failures counter")
        void testIncrementBrochureDownloadFailures() {
            assertEquals(0, ProcessingLogger.getBrochureDownloadFailures());
            
            ProcessingLogger.incrementBrochureDownloadFailures();
            assertEquals(1, ProcessingLogger.getBrochureDownloadFailures());
            
            ProcessingLogger.incrementBrochureDownloadFailures();
            assertEquals(2, ProcessingLogger.getBrochureDownloadFailures());
        }
        
        @Test
        @DisplayName("Should increment filename parsing failures counter")
        void testIncrementFilenameParsingFailures() {
            assertEquals(0, ProcessingLogger.getFilenameParsingFailures());
            
            ProcessingLogger.incrementFilenameParsingFailures();
            assertEquals(1, ProcessingLogger.getFilenameParsingFailures());
            
            ProcessingLogger.incrementFilenameParsingFailures();
            assertEquals(2, ProcessingLogger.getFilenameParsingFailures());
        }
        
        @Test
        @DisplayName("Should reset all counters to zero")
        void testResetCounters() {
            // Set up some counter values
            ProcessingLogger.incrementTotalFirmsProcessed();
            ProcessingLogger.incrementFirmsWithoutBrochures();
            ProcessingLogger.incrementBrochureDownloadFailures();
            ProcessingLogger.incrementFilenameParsingFailures();
            ProcessingLogger.incrementFirmsWithBrochures();
            
            // Verify counters have values
            assertTrue(ProcessingLogger.getTotalFirmsProcessed() > 0);
            assertTrue(ProcessingLogger.getFirmsWithoutBrochures() > 0);
            assertTrue(ProcessingLogger.getBrochureDownloadFailures() > 0);
            assertTrue(ProcessingLogger.getFilenameParsingFailures() > 0);
            assertTrue(ProcessingLogger.getFirmsWithBrochures() > 0);
            
            // Reset and verify all are zero
            ProcessingLogger.resetCounters();
            assertEquals(0, ProcessingLogger.getTotalFirmsProcessed());
            assertEquals(0, ProcessingLogger.getFirmsWithoutBrochures());
            assertEquals(0, ProcessingLogger.getBrochureDownloadFailures());
            assertEquals(0, ProcessingLogger.getFilenameParsingFailures());
            assertEquals(0, ProcessingLogger.getFirmsWithBrochures());
        }
    }
    
    @Nested
    @DisplayName("Logging Methods Tests")
    class LoggingMethodsTests {
        
        @Test
        @DisplayName("Should log info messages without throwing exceptions")
        void testLogInfo() {
            assertDoesNotThrow(() -> {
                ProcessingLogger.logInfo("Test info message");
            });
        }
        
        @Test
        @DisplayName("Should log warning messages without throwing exceptions")
        void testLogWarning() {
            assertDoesNotThrow(() -> {
                ProcessingLogger.logWarning("Test warning message");
            });
        }
        
        @Test
        @DisplayName("Should log error messages without exceptions")
        void testLogErrorWithoutException() {
            assertDoesNotThrow(() -> {
                ProcessingLogger.logError("Test error message", null);
            });
        }
        
        @Test
        @DisplayName("Should log error messages with exceptions")
        void testLogErrorWithException() {
            Exception testException = new RuntimeException("Test exception");
            assertDoesNotThrow(() -> {
                ProcessingLogger.logError("Test error message with exception", testException);
            });
        }
        
        @Test
        @DisplayName("Should handle null error messages gracefully")
        void testLogErrorWithNullMessage() {
            assertDoesNotThrow(() -> {
                ProcessingLogger.logError(null, null);
            });
        }
    }
    
    @Nested
    @DisplayName("Processing Summary Tests")
    class ProcessingSummaryTests {
        
        @Test
        @DisplayName("Should print processing summary without throwing exceptions")
        void testPrintProcessingSummary() {
            // Set up some test data
            ProcessingLogger.incrementTotalFirmsProcessed();
            ProcessingLogger.incrementTotalFirmsProcessed();
            ProcessingLogger.incrementFirmsWithoutBrochures();
            ProcessingLogger.incrementFirmsWithBrochures();
            
            assertDoesNotThrow(() -> {
                ProcessingLogger.printProcessingSummary();
            });
        }
        
        @Test
        @DisplayName("Should print summary with zero values")
        void testPrintProcessingSummaryWithZeroValues() {
            ProcessingLogger.resetCounters();
            
            assertDoesNotThrow(() -> {
                ProcessingLogger.printProcessingSummary();
            });
        }
    }
    
    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("Should handle concurrent counter increments safely")
        void testConcurrentCounterIncrements() throws InterruptedException {
            final int numberOfThreads = 10;
            final int incrementsPerThread = 100;
            Thread[] threads = new Thread[numberOfThreads];
            
            // Create threads that increment counters concurrently
            for (int i = 0; i < numberOfThreads; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        ProcessingLogger.incrementTotalFirmsProcessed();
                        ProcessingLogger.incrementFirmsWithoutBrochures();
                        ProcessingLogger.incrementBrochureDownloadFailures();
                        ProcessingLogger.incrementFilenameParsingFailures();
                        ProcessingLogger.incrementFirmsWithBrochures();
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
            
            // Verify final counts
            int expectedCount = numberOfThreads * incrementsPerThread;
            assertEquals(expectedCount, ProcessingLogger.getTotalFirmsProcessed());
            assertEquals(expectedCount, ProcessingLogger.getFirmsWithoutBrochures());
            assertEquals(expectedCount, ProcessingLogger.getBrochureDownloadFailures());
            assertEquals(expectedCount, ProcessingLogger.getFilenameParsingFailures());
            assertEquals(expectedCount, ProcessingLogger.getFirmsWithBrochures());
        }
    }
    
    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle multiple rapid increments")
        void testRapidIncrements() {
            for (int i = 0; i < 1000; i++) {
                ProcessingLogger.incrementTotalFirmsProcessed();
            }
            assertEquals(1000, ProcessingLogger.getTotalFirmsProcessed());
        }
        
        @Test
        @DisplayName("Should handle empty string logging")
        void testEmptyStringLogging() {
            assertDoesNotThrow(() -> {
                ProcessingLogger.logInfo("");
                ProcessingLogger.logWarning("");
                ProcessingLogger.logError("", null);
            });
        }
        
        @Test
        @DisplayName("Should handle very long log messages")
        void testLongLogMessages() {
            StringBuilder longMessage = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longMessage.append("This is a very long log message. ");
            }
            
            assertDoesNotThrow(() -> {
                ProcessingLogger.logInfo(longMessage.toString());
                ProcessingLogger.logWarning(longMessage.toString());
                ProcessingLogger.logError(longMessage.toString(), null);
            });
        }
    }
}
