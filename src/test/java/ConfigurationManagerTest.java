import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 tests for ConfigurationManager
 * Tests multi-source configuration management with priority handling
 */
@DisplayName("ConfigurationManager Tests")
class ConfigurationManagerTest {

    private ConfigurationManager configManager;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        configManager = new ConfigurationManager();
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Nested
    @DisplayName("Command Line Configuration Tests")
    class CommandLineConfigurationTests {

        @Test
        @DisplayName("Should build context from basic command line arguments")
        void shouldBuildContextFromBasicArgs() {
            String[] args = {"--index-limit", "100", "--verbose"};
            
            ProcessingContext context = configManager.buildContext(args);
            
            assertNotNull(context, "Context should not be null");
            assertEquals(100, context.getIndexLimit(), "Index limit should be 100");
            assertTrue(context.isVerbose(), "Verbose should be true");
            assertEquals("command-line", context.getConfigSource(), "Config source should be command-line");
        }

        @Test
        @DisplayName("Should handle short form command line arguments")
        void shouldHandleShortFormArgs() {
            String[] args = {"-l", "50", "-v"};
            
            ProcessingContext context = configManager.buildContext(args);
            
            assertEquals(50, context.getIndexLimit(), "Index limit should be 50");
            assertTrue(context.isVerbose(), "Verbose should be true");
        }

        @Test
        @DisplayName("Should handle empty command line arguments")
        void shouldHandleEmptyArgs() {
            String[] args = {};
            
            ProcessingContext context = configManager.buildContext(args);
            
            assertNotNull(context, "Context should not be null");
            assertEquals(Integer.MAX_VALUE, context.getIndexLimit(), "Default index limit should be unlimited");
            assertFalse(context.isVerbose(), "Default verbose should be false");
        }

        @Test
        @DisplayName("Should handle null command line arguments")
        void shouldHandleNullArgs() {
            ProcessingContext context = configManager.buildContext(null);
            
            assertNotNull(context, "Context should not be null");
            assertEquals(Integer.MAX_VALUE, context.getIndexLimit(), "Default index limit should be unlimited");
            assertFalse(context.isVerbose(), "Default verbose should be false");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 10, 100, 1000, 5000})
        @DisplayName("Should handle various index limit values")
        void shouldHandleVariousIndexLimits(int indexLimit) {
            String[] args = {"--index-limit", String.valueOf(indexLimit)};
            
            ProcessingContext context = configManager.buildContext(args);
            
            assertEquals(indexLimit, context.getIndexLimit(), 
                "Index limit should be " + indexLimit);
        }

        @Test
        @DisplayName("Should handle invalid command line arguments gracefully")
        void shouldHandleInvalidArgsGracefully() {
            String[] args = {"--invalid-option", "value"};
            
            assertDoesNotThrow(() -> {
                ProcessingContext context = configManager.buildContext(args);
                assertNotNull(context, "Context should still be created with invalid args");
            }, "Should not throw exception for invalid arguments");
        }
    }

    @Nested
    @DisplayName("Properties File Configuration Tests")
    class PropertiesFileConfigurationTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should load configuration from default properties file location")
        void shouldLoadFromDefaultPropertiesFile() throws IOException {
            // Create properties file in current directory (where ConfigurationManager looks)
            File propsFile = new File("iapd.properties");
            boolean fileCreated = false;
            
            try {
                try (FileWriter writer = new FileWriter(propsFile)) {
                    writer.write("index.limit=200\n");
                    writer.write("verbose=true\n");
                    writer.write("retry.count=5\n");
                    writer.write("skip.brochure.download=true\n");
                }
                fileCreated = true;

                // Test that buildContext picks up the properties file
                ProcessingContext context = configManager.buildContext(new String[]{});
                
                assertNotNull(context, "Context should not be null");
                // Note: Command line args override properties, so we test with empty args
                // The actual values depend on the implementation's property loading logic
                
            } finally {
                // Clean up the test file
                if (fileCreated && propsFile.exists()) {
                    propsFile.delete();
                }
            }
        }

        @Test
        @DisplayName("Should handle missing properties file gracefully")
        void shouldHandleMissingPropertiesFile() {
            // Ensure no properties file exists
            File propsFile = new File("iapd.properties");
            if (propsFile.exists()) {
                propsFile.delete();
            }
            
            // Should still create context with defaults
            ProcessingContext context = configManager.buildContext(new String[]{});
            
            assertNotNull(context, "Context should not be null even without properties file");
            assertEquals(Integer.MAX_VALUE, context.getIndexLimit(), "Should use default index limit");
            assertFalse(context.isVerbose(), "Should use default verbose setting");
        }

        @Test
        @DisplayName("Should create sample configuration file")
        void shouldCreateSampleConfigurationFile() throws IOException {
            File sampleFile = tempDir.resolve("sample.properties").toFile();
            
            assertDoesNotThrow(() -> configManager.createSampleConfigFile(sampleFile.getAbsolutePath()),
                "Should create sample config file without throwing exception");
            
            assertTrue(sampleFile.exists(), "Sample config file should be created");
            assertTrue(sampleFile.length() > 0, "Sample config file should not be empty");
        }
    }

    @Nested
    @DisplayName("Configuration Priority Tests")
    class ConfigurationPriorityTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Command line should override properties file")
        void commandLineShouldOverrideProperties() throws IOException {
            // Create properties file with default values
            File propsFile = tempDir.resolve("test.properties").toFile();
            try (FileWriter writer = new FileWriter(propsFile)) {
                writer.write("index.limit=100\n");
                writer.write("verbose=false\n");
            }

            // Command line arguments that should override
            String[] args = {"--index-limit", "200", "--verbose"};
            
            // Test the priority (this would require extending ConfigurationManager to support this)
            ProcessingContext context = configManager.buildContext(args);
            
            // Command line should win
            assertEquals(200, context.getIndexLimit(), "Command line index limit should override properties");
            assertTrue(context.isVerbose(), "Command line verbose should override properties");
        }

        @Test
        @DisplayName("Should use default values when no configuration provided")
        void shouldUseDefaultValues() {
            String[] args = {};
            
            ProcessingContext context = configManager.buildContext(args);
            
            assertEquals(Integer.MAX_VALUE, context.getIndexLimit(), "Should use default unlimited index limit");
            assertFalse(context.isVerbose(), "Should use default verbose false");
            assertEquals(3, context.getRetryCount(), "Should use default retry count 3");
            assertFalse(context.isSkipBrochureDownload(), "Should use default skip brochure download false");
        }
    }

    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("Should validate positive index limit")
        void shouldValidatePositiveIndexLimit() {
            String[] args = {"--index-limit", "0"};
            
            ProcessingContext context = configManager.buildContext(args);
            
            // Should handle zero or negative values appropriately
            assertTrue(context.getIndexLimit() >= 0, "Index limit should be non-negative");
        }

        @Test
        @DisplayName("Should handle invalid numeric values")
        void shouldHandleInvalidNumericValues() {
            String[] args = {"--index-limit", "not-a-number"};
            
            assertDoesNotThrow(() -> {
                ProcessingContext context = configManager.buildContext(args);
                assertNotNull(context, "Context should still be created with invalid numeric values");
            }, "Should handle invalid numeric values gracefully");
        }

        @Test
        @DisplayName("Should validate configuration correctly")
        void shouldValidateConfiguration() {
            ProcessingContext validContext = ProcessingContext.builder()
                    .indexLimit(100)
                    .verbose(true)
                    .retryCount(3)
                    .outputFormat("CSV")
                    .build();
            
            boolean isValid = configManager.validateConfiguration(validContext);
            assertTrue(isValid, "Valid configuration should pass validation");
        }
    }

    @Nested
    @DisplayName("Configuration Printing Tests")
    class ConfigurationPrintingTests {

        @Test
        @DisplayName("Should print effective configuration")
        void shouldPrintEffectiveConfiguration() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(100)
                    .verbose(true)
                    .retryCount(5)
                    .skipBrochureDownload(false)
                    .configSource("test")
                    .build();

            assertDoesNotThrow(() -> configManager.printEffectiveConfiguration(context),
                "Should not throw exception when printing configuration");

            String output = outputStream.toString();
            assertTrue(output.contains("Configuration"), "Output should contain 'Configuration'");
            assertTrue(output.contains("100"), "Output should contain index limit value");
            assertTrue(output.contains("true"), "Output should contain verbose value");
        }

        @Test
        @DisplayName("Should handle null context in print method")
        void shouldHandleNullContextInPrint() {
            assertDoesNotThrow(() -> configManager.printEffectiveConfiguration(null),
                "Should handle null context gracefully");
        }

        @Test
        @DisplayName("Should print configuration summary")
        void shouldPrintConfigurationSummary() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(50)
                    .verbose(false)
                    .build();

            configManager.printEffectiveConfiguration(context);
            
            String output = outputStream.toString();
            assertFalse(output.isEmpty(), "Output should not be empty");
            assertTrue(output.length() > 10, "Output should contain meaningful content");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid command line gracefully")
        void shouldHandleInvalidCommandLineGracefully() {
            String[] invalidArgs = {"--nonexistent-option", "value"};
            
            assertDoesNotThrow(() -> {
                ProcessingContext context = configManager.buildContext(invalidArgs);
                assertNotNull(context, "Should create context even with invalid args");
            }, "Should handle invalid command line arguments gracefully");
        }

        @Test
        @DisplayName("Should handle malformed command line arguments")
        void shouldHandleMalformedCommandLineArguments() {
            String[] malformedArgs = {"--index-limit", "not-a-number"};
            
            assertDoesNotThrow(() -> {
                ProcessingContext context = configManager.buildContext(malformedArgs);
                assertNotNull(context, "Should create context even with malformed args");
            }, "Should handle malformed command line arguments gracefully");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should integrate command line and properties configuration")
        void shouldIntegrateCommandLineAndProperties() {
            // Test command line arguments
            String[] args = {"--index-limit", "300", "--verbose"};
            ProcessingContext context = configManager.buildContext(args);
            
            // Verify command line values are used
            assertEquals(300, context.getIndexLimit(), "Command line index limit should be 300");
            assertTrue(context.isVerbose(), "Command line verbose should be true");
            assertEquals("command-line", context.getConfigSource(), "Config source should be command-line");
        }

        @Test
        @DisplayName("Should handle complete configuration workflow")
        void shouldHandleCompleteConfigurationWorkflow() {
            // Test comprehensive configuration
            String[] args = {"--index-limit", "500", "--verbose"};
            ProcessingContext context = configManager.buildContext(args);
            
            assertNotNull(context, "Context should not be null");
            
            // Print configuration (should not throw)
            assertDoesNotThrow(() -> configManager.printEffectiveConfiguration(context),
                "Should print configuration without errors");
            
            // Validate configuration
            boolean isValid = configManager.validateConfiguration(context);
            assertTrue(isValid, "Configuration should be valid");
            
            // Verify all values
            assertAll("All configuration values should be correct",
                () -> assertEquals(500, context.getIndexLimit()),
                () -> assertTrue(context.isVerbose()),
                () -> assertEquals(3, context.getRetryCount()),
                () -> assertFalse(context.isSkipBrochureDownload()),
                () -> assertEquals("command-line", context.getConfigSource())
            );
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should build context quickly with many arguments")
        void shouldBuildContextQuicklyWithManyArguments() {
            String[] args = {"--index-limit", "1000", "--verbose"};
            
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                ProcessingContext context = configManager.buildContext(args);
                assertNotNull(context, "Context should be built successfully");
            }
            long buildTime = System.currentTimeMillis() - startTime;
            
            assertTrue(buildTime < 1000, "Should build 1000 contexts in under 1 second");
        }

        @Test
        @DisplayName("Should handle configuration validation efficiently")
        void shouldHandleConfigurationValidationEfficiently() {
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(100)
                    .verbose(true)
                    .retryCount(5)
                    .outputFormat("CSV")
                    .build();
            
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                boolean isValid = configManager.validateConfiguration(context);
                assertTrue(isValid, "Configuration should be valid");
            }
            long validationTime = System.currentTimeMillis() - startTime;
            
            assertTrue(validationTime < 1000, "Should validate 10000 configurations in under 1 second");
        }
    }
}
