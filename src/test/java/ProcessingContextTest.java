import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 tests for ProcessingContext
 * Tests the unified context architecture with configuration and runtime state
 */
@DisplayName("ProcessingContext Tests")
class ProcessingContextTest {

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create context with default values using builder")
        void shouldCreateContextWithDefaults() {
            ProcessingContext context = ProcessingContext.builder().build();
            
            assertNotNull(context, "Context should not be null");
            assertEquals(Integer.MAX_VALUE, context.getIndexLimit(), "Default index limit should be unlimited");
            assertFalse(context.isVerbose(), "Default verbose should be false");
            assertEquals(3, context.getRetryCount(), "Default retry count should be 3");
            assertFalse(context.isSkipBrochureDownload(), "Default skip brochure download should be false");
            assertEquals("builder", context.getConfigSource(), "Default config source should be 'builder'");
            assertNotNull(context.getCreatedAt(), "Created timestamp should not be null");
        }

        @Test
        @DisplayName("Should create context with custom values using builder")
        void shouldCreateContextWithCustomValues() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(100)
                    .verbose(true)
                    .retryCount(5)
                    .skipBrochureDownload(true)
                    .configSource("test-config")
                    .build();
            
            assertEquals(100, context.getIndexLimit(), "Index limit should be set to 100");
            assertTrue(context.isVerbose(), "Verbose should be true");
            assertEquals(5, context.getRetryCount(), "Retry count should be 5");
            assertTrue(context.isSkipBrochureDownload(), "Skip brochure download should be true");
            assertEquals("test-config", context.getConfigSource(), "Config source should be 'test-config'");
        }

        @Test
        @DisplayName("Should handle method chaining in builder")
        void shouldHandleMethodChaining() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(50)
                    .verbose(true)
                    .retryCount(2)
                    .skipBrochureDownload(false)
                    .configSource("chained-config")
                    .build();
            
            assertAll("All builder values should be set correctly",
                () -> assertEquals(50, context.getIndexLimit()),
                () -> assertTrue(context.isVerbose()),
                () -> assertEquals(2, context.getRetryCount()),
                () -> assertFalse(context.isSkipBrochureDownload()),
                () -> assertEquals("chained-config", context.getConfigSource())
            );
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 10, 100, 1000, Integer.MAX_VALUE})
        @DisplayName("Should handle various index limit values")
        void shouldHandleVariousIndexLimits(int indexLimit) {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(indexLimit)
                    .build();
            
            assertEquals(indexLimit, context.getIndexLimit(), 
                "Index limit should be set to " + indexLimit);
        }

        @Test
        @DisplayName("Should handle negative index limit by setting to unlimited")
        void shouldHandleNegativeIndexLimit() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(-1)
                    .build();
            
            assertEquals(Integer.MAX_VALUE, context.getIndexLimit(), 
                "Negative index limit should be converted to unlimited");
        }
    }

    @Nested
    @DisplayName("Runtime State Management Tests")
    class RuntimeStateTests {

        private ProcessingContext context;

        @BeforeEach
        void setUp() {
            context = ProcessingContext.builder()
                    .indexLimit(10)
                    .verbose(true)
                    .build();
        }

        @Test
        @DisplayName("Should track processed firms count")
        void shouldTrackProcessedFirms() {
            assertEquals(0, context.getProcessedFirms(), "Initial processed firms should be 0");
            
            context.incrementProcessedFirms();
            assertEquals(1, context.getProcessedFirms(), "Processed firms should be 1 after increment");
            
            for (int i = 0; i < 5; i++) {
                context.incrementProcessedFirms();
            }
            assertEquals(6, context.getProcessedFirms(), "Processed firms should be 6 after 6 increments");
        }

        @Test
        @DisplayName("Should track successful downloads")
        void shouldTrackSuccessfulDownloads() {
            assertEquals(0, context.getSuccessfulDownloads(), "Initial successful downloads should be 0");
            
            context.incrementSuccessfulDownloads();
            assertEquals(1, context.getSuccessfulDownloads(), "Successful downloads should be 1 after increment");
            
            for (int i = 0; i < 3; i++) {
                context.incrementSuccessfulDownloads();
            }
            assertEquals(4, context.getSuccessfulDownloads(), "Successful downloads should be 4 after 4 increments");
        }

        @Test
        @DisplayName("Should track failed downloads")
        void shouldTrackFailedDownloads() {
            assertEquals(0, context.getFailedDownloads(), "Initial failed downloads should be 0");
            
            context.incrementFailedDownloads();
            assertEquals(1, context.getFailedDownloads(), "Failed downloads should be 1 after increment");
            
            for (int i = 0; i < 2; i++) {
                context.incrementFailedDownloads();
            }
            assertEquals(3, context.getFailedDownloads(), "Failed downloads should be 3 after 3 increments");
        }

        @Test
        @DisplayName("Should track brochures processed")
        void shouldTrackBrochuresProcessed() {
            assertEquals(0, context.getBrochuresProcessed(), "Initial brochures processed should be 0");
            
            context.incrementBrochuresProcessed();
            assertEquals(1, context.getBrochuresProcessed(), "Brochures processed should be 1 after increment");
            
            for (int i = 0; i < 4; i++) {
                context.incrementBrochuresProcessed();
            }
            assertEquals(5, context.getBrochuresProcessed(), "Brochures processed should be 5 after 5 increments");
        }

        @Test
        @DisplayName("Should track current processing phase")
        void shouldTrackCurrentPhase() {
            assertEquals(ProcessingPhase.INITIALIZATION, context.getCurrentPhase(), 
                "Initial phase should be INITIALIZATION");
            
            context.setCurrentPhase(ProcessingPhase.PARSING_XML);
            assertEquals(ProcessingPhase.PARSING_XML, context.getCurrentPhase(), 
                "Phase should be PARSING_XML after setting");
            
            context.setCurrentPhase(ProcessingPhase.DOWNLOADING_BROCHURES);
            assertEquals(ProcessingPhase.DOWNLOADING_BROCHURES, context.getCurrentPhase(), 
                "Phase should be DOWNLOADING_BROCHURES after setting");
        }

        @Test
        @DisplayName("Should track current processing file")
        void shouldTrackCurrentFile() {
            assertNull(context.getCurrentProcessingFile(), "Initial processing file should be null");
            
            context.setCurrentProcessingFile("test_file.xml");
            assertEquals("test_file.xml", context.getCurrentProcessingFile(), 
                "Processing file should be 'test_file.xml' after setting");
            
            context.setCurrentProcessingFile("another_file.pdf");
            assertEquals("another_file.pdf", context.getCurrentProcessingFile(), 
                "Processing file should be 'another_file.pdf' after setting");
        }
    }

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("Should correctly determine if index limit is reached")
        void shouldDetermineIndexLimitReached() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(5)
                    .build();
            
            assertFalse(context.hasReachedIndexLimit(), "Should not have reached limit initially");
            
            for (int i = 0; i < 4; i++) {
                context.incrementProcessedFirms();
                assertFalse(context.hasReachedIndexLimit(), 
                    "Should not have reached limit at " + (i + 1) + " firms");
            }
            
            context.incrementProcessedFirms(); // 5th firm
            assertTrue(context.hasReachedIndexLimit(), "Should have reached limit at 5 firms");
        }

        @Test
        @DisplayName("Should handle unlimited index limit")
        void shouldHandleUnlimitedIndexLimit() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(Integer.MAX_VALUE)
                    .build();
            
            for (int i = 0; i < 1000; i++) {
                context.incrementProcessedFirms();
                assertFalse(context.hasReachedIndexLimit(), 
                    "Should never reach unlimited limit");
            }
        }

        @Test
        @DisplayName("Should calculate processing rate correctly")
        void shouldCalculateProcessingRate() throws InterruptedException {
            ProcessingContext context = ProcessingContext.builder().build();
            
            // Initial rate should be 0
            assertEquals(0.0, context.getProcessingRate(), 0.01, 
                "Initial processing rate should be 0");
            
            // Process some firms with a small delay
            for (int i = 0; i < 5; i++) {
                context.incrementProcessedFirms();
                Thread.sleep(10); // Small delay to ensure time passes
            }
            
            double rate = context.getProcessingRate();
            assertTrue(rate > 0, "Processing rate should be greater than 0 after processing firms");
            assertTrue(rate < 1000, "Processing rate should be reasonable (less than 1000 firms/sec)");
        }

        @Test
        @DisplayName("Should track elapsed time correctly")
        void shouldTrackElapsedTime() throws InterruptedException {
            ProcessingContext context = ProcessingContext.builder().build();
            
            long initialTime = context.getElapsedTimeMs();
            assertTrue(initialTime >= 0, "Initial elapsed time should be non-negative");
            
            Thread.sleep(50); // Wait 50ms
            
            long laterTime = context.getElapsedTimeMs();
            assertTrue(laterTime > initialTime, "Elapsed time should increase");
            assertTrue(laterTime >= 50, "Elapsed time should be at least 50ms");
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    @Execution(ExecutionMode.CONCURRENT)
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent firm processing increments")
        void shouldHandleConcurrentFirmIncrements() throws InterruptedException {
            ProcessingContext context = ProcessingContext.builder().build();
            int threadCount = 10;
            int incrementsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < incrementsPerThread; j++) {
                            context.incrementProcessedFirms();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete within 5 seconds");
            assertEquals(threadCount * incrementsPerThread, context.getProcessedFirms(),
                "Total processed firms should equal threadCount * incrementsPerThread");

            executor.shutdown();
        }

        @Test
        @DisplayName("Should handle concurrent download tracking")
        void shouldHandleConcurrentDownloadTracking() throws InterruptedException {
            ProcessingContext context = ProcessingContext.builder().build();
            int threadCount = 5;
            int operationsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount * 2); // 2 operations per thread

            // Half threads increment successful downloads
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            context.incrementSuccessfulDownloads();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Half threads increment failed downloads
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            context.incrementFailedDownloads();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete within 5 seconds");
            assertEquals(threadCount * operationsPerThread, context.getSuccessfulDownloads(),
                "Successful downloads should be correct");
            assertEquals(threadCount * operationsPerThread, context.getFailedDownloads(),
                "Failed downloads should be correct");

            executor.shutdown();
        }

        @Test
        @DisplayName("Should handle concurrent phase and file updates")
        void shouldHandleConcurrentPhaseAndFileUpdates() throws InterruptedException {
            ProcessingContext context = ProcessingContext.builder().build();
            int threadCount = 3;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger phaseUpdateCount = new AtomicInteger(0);

            ProcessingPhase[] phases = {
                ProcessingPhase.PARSING_XML,
                ProcessingPhase.DOWNLOADING_BROCHURES,
                ProcessingPhase.PROCESSING_BROCHURES
            };

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 10; j++) {
                            context.setCurrentPhase(phases[threadIndex]);
                            context.setCurrentProcessingFile("file_" + threadIndex + "_" + j + ".xml");
                            phaseUpdateCount.incrementAndGet();
                            Thread.sleep(1); // Small delay
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete within 5 seconds");
            assertEquals(30, phaseUpdateCount.get(), "All phase updates should be counted");
            
            // Verify final state is one of the expected phases
            ProcessingPhase finalPhase = context.getCurrentPhase();
            assertTrue(finalPhase == ProcessingPhase.PARSING_XML || 
                      finalPhase == ProcessingPhase.DOWNLOADING_BROCHURES || 
                      finalPhase == ProcessingPhase.PROCESSING_BROCHURES,
                "Final phase should be one of the test phases");

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Command Line Integration Tests")
    class CommandLineIntegrationTests {

        @Test
        @DisplayName("Should create context from command line options")
        void shouldCreateContextFromCommandLineOptions() {
            try {
                CommandLineOptions options = CommandLineOptions.parseArgs(
                    new String[]{"--index-limit", "100", "--verbose"});
                ProcessingContext context = ProcessingContext.fromCommandLineOptions(options);
                
                assertNotNull(context, "Context should not be null");
                assertEquals(100, context.getIndexLimit(), "Index limit should be 100");
                assertTrue(context.isVerbose(), "Verbose should be true");
                assertEquals("command-line", context.getConfigSource(), "Config source should be 'command-line'");
            } catch (Exception e) {
                fail("Should not throw exception when creating context from valid command line options: " + e.getMessage());
            }
        }

        @ParameterizedTest
        @CsvSource({
            "'-l, 50', 50, false",
            "'--index-limit, 200, --verbose', 200, true",
            "'--verbose', 2147483647, true"
        })
        @DisplayName("Should handle various command line combinations")
        void shouldHandleVariousCommandLineCombinations(String argsString, int expectedLimit, boolean expectedVerbose) {
            try {
                String[] args = argsString.split(", ");
                CommandLineOptions options = CommandLineOptions.parseArgs(args);
                ProcessingContext context = ProcessingContext.fromCommandLineOptions(options);
                
                assertEquals(expectedLimit, context.getIndexLimit(), "Index limit should match expected");
                assertEquals(expectedVerbose, context.isVerbose(), "Verbose should match expected");
            } catch (Exception e) {
                fail("Should not throw exception for valid command line args: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("State Logging Tests")
    class StateLoggingTests {

        @Test
        @DisplayName("Should log current state without throwing exceptions")
        void shouldLogCurrentStateWithoutExceptions() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(10)
                    .verbose(true)
                    .build();
            
            context.setCurrentPhase(ProcessingPhase.PARSING_XML);
            context.setCurrentProcessingFile("test.xml");
            context.incrementProcessedFirms();
            context.incrementSuccessfulDownloads();
            
            assertDoesNotThrow(() -> context.logCurrentState(), 
                "Logging current state should not throw exceptions");
        }

        @Test
        @DisplayName("Should handle toString method correctly")
        void shouldHandleToStringCorrectly() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(50)
                    .verbose(true)
                    .retryCount(5)
                    .configSource("test")
                    .build();
            
            String contextString = context.toString();
            assertNotNull(contextString, "toString should not return null");
            assertTrue(contextString.contains("indexLimit=50"), "toString should contain index limit");
            assertTrue(contextString.contains("verbose=true"), "toString should contain verbose setting");
            assertTrue(contextString.contains("retryCount=5"), "toString should contain retry count");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null phase setting gracefully")
        void shouldHandleNullPhaseGracefully() {
            ProcessingContext context = ProcessingContext.builder().build();
            
            assertDoesNotThrow(() -> context.setCurrentPhase(null), 
                "Setting null phase should not throw exception");
        }

        @Test
        @DisplayName("Should handle null file setting gracefully")
        void shouldHandleNullFileGracefully() {
            ProcessingContext context = ProcessingContext.builder().build();
            
            assertDoesNotThrow(() -> context.setCurrentProcessingFile(null), 
                "Setting null file should not throw exception");
            assertNull(context.getCurrentProcessingFile(), "File should remain null");
        }

        @Test
        @DisplayName("Should handle empty string file setting")
        void shouldHandleEmptyStringFile() {
            ProcessingContext context = ProcessingContext.builder().build();
            
            context.setCurrentProcessingFile("");
            assertEquals("", context.getCurrentProcessingFile(), "Empty string file should be preserved");
            
            context.setCurrentProcessingFile("   ");
            assertEquals("   ", context.getCurrentProcessingFile(), "Whitespace file should be preserved");
        }

        @Test
        @DisplayName("Should handle very large counter values")
        void shouldHandleVeryLargeCounterValues() {
            ProcessingContext context = ProcessingContext.builder().build();
            
            // Increment counters to large values
            for (int i = 0; i < 1000000; i++) {
                context.incrementProcessedFirms();
                if (i % 2 == 0) context.incrementSuccessfulDownloads();
                if (i % 3 == 0) context.incrementFailedDownloads();
                if (i % 5 == 0) context.incrementBrochuresProcessed();
            }
            
            assertEquals(1000000, context.getProcessedFirms(), "Should handle large processed firms count");
            assertEquals(500000, context.getSuccessfulDownloads(), "Should handle large successful downloads count");
            assertEquals(333334, context.getFailedDownloads(), "Should handle large failed downloads count");
            assertEquals(200000, context.getBrochuresProcessed(), "Should handle large brochures processed count");
        }
    }
}
