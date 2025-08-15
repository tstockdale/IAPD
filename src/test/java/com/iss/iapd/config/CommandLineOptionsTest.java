package com.iss.iapd.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Comprehensive JUnit 5 tests for CommandLineOptions
 * Tests command line argument parsing with validation and error handling
 */
@DisplayName("CommandLineOptions Tests")
public class CommandLineOptionsTest {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Nested
    @DisplayName("Basic Argument Parsing Tests")
    class BasicArgumentParsingTests {

        @Test
        @DisplayName("Should parse empty arguments with defaults")
        void shouldParseEmptyArgsWithDefaults() {
            String[] args = {};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertNotNull(options, "Options should not be null");
            assertEquals(Integer.MAX_VALUE, options.getIndexLimit(), "Default index limit should be unlimited");
            assertFalse(options.isVerbose(), "Default verbose should be false");
            assertFalse(options.isShowHelp(), "Default show help should be false");
        }

        @Test
        @DisplayName("Should parse null arguments with defaults")
        void shouldParseNullArgsWithDefaults() {
            CommandLineOptions options = CommandLineOptions.parseArgs(null);
            
            assertNotNull(options, "Options should not be null");
            assertEquals(Integer.MAX_VALUE, options.getIndexLimit(), "Default index limit should be unlimited");
            assertFalse(options.isVerbose(), "Default verbose should be false");
            assertFalse(options.isShowHelp(), "Default show help should be false");
        }

        @Test
        @DisplayName("Should parse index limit with long form")
        void shouldParseIndexLimitLongForm() {
            String[] args = {"--index-limit", "100"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals(100, options.getIndexLimit(), "Index limit should be 100");
            assertFalse(options.isVerbose(), "Verbose should remain default false");
            assertFalse(options.isShowHelp(), "Show help should remain default false");
        }

        @Test
        @DisplayName("Should parse index limit with short form")
        void shouldParseIndexLimitShortForm() {
            String[] args = {"-l", "50"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals(50, options.getIndexLimit(), "Index limit should be 50");
            assertFalse(options.isVerbose(), "Verbose should remain default false");
            assertFalse(options.isShowHelp(), "Show help should remain default false");
        }

        @Test
        @DisplayName("Should parse verbose with long form")
        void shouldParseVerboseLongForm() {
            String[] args = {"--verbose"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertTrue(options.isVerbose(), "Verbose should be true");
            assertEquals(Integer.MAX_VALUE, options.getIndexLimit(), "Index limit should remain default unlimited");
            assertFalse(options.isShowHelp(), "Show help should remain default false");
        }

        @Test
        @DisplayName("Should parse verbose with short form")
        void shouldParseVerboseShortForm() {
            String[] args = {"-v"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertTrue(options.isVerbose(), "Verbose should be true");
            assertEquals(Integer.MAX_VALUE, options.getIndexLimit(), "Index limit should remain default unlimited");
            assertFalse(options.isShowHelp(), "Show help should remain default false");
        }

        @Test
        @DisplayName("Should parse help with long form")
        void shouldParseHelpLongForm() {
            String[] args = {"--help"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertTrue(options.isShowHelp(), "Show help should be true");
            assertEquals(Integer.MAX_VALUE, options.getIndexLimit(), "Index limit should remain default unlimited");
            assertFalse(options.isVerbose(), "Verbose should remain default false");
        }

        @Test
        @DisplayName("Should parse help with short form")
        void shouldParseHelpShortForm() {
            String[] args = {"-h"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertTrue(options.isShowHelp(), "Show help should be true");
            assertEquals(Integer.MAX_VALUE, options.getIndexLimit(), "Index limit should remain default unlimited");
            assertFalse(options.isVerbose(), "Verbose should remain default false");
        }
    }

    @Nested
    @DisplayName("Combined Arguments Tests")
    class CombinedArgumentsTests {

        @Test
        @DisplayName("Should parse multiple arguments together")
        void shouldParseMultipleArguments() {
            String[] args = {"--index-limit", "200", "--verbose"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("All arguments should be parsed correctly",
                () -> assertEquals(200, options.getIndexLimit(), "Index limit should be 200"),
                () -> assertTrue(options.isVerbose(), "Verbose should be true"),
                () -> assertFalse(options.isShowHelp(), "Show help should remain false")
            );
        }

        @Test
        @DisplayName("Should parse mixed long and short forms")
        void shouldParseMixedForms() {
            String[] args = {"-l", "150", "--verbose"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("Mixed forms should be parsed correctly",
                () -> assertEquals(150, options.getIndexLimit(), "Index limit should be 150"),
                () -> assertTrue(options.isVerbose(), "Verbose should be true"),
                () -> assertFalse(options.isShowHelp(), "Show help should remain false")
            );
        }

        @Test
        @DisplayName("Should parse all arguments together")
        void shouldParseAllArguments() {
            String[] args = {"--index-limit", "300", "--verbose", "--help"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("All arguments should be parsed correctly",
                () -> assertEquals(300, options.getIndexLimit(), "Index limit should be 300"),
                () -> assertTrue(options.isVerbose(), "Verbose should be true"),
                () -> assertTrue(options.isShowHelp(), "Show help should be true")
            );
        }

        @Test
        @DisplayName("Should handle arguments in different order")
        void shouldHandleArgumentsInDifferentOrder() {
            String[] args = {"--verbose", "--index-limit", "75", "--help"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("Arguments in different order should be parsed correctly",
                () -> assertEquals(75, options.getIndexLimit(), "Index limit should be 75"),
                () -> assertTrue(options.isVerbose(), "Verbose should be true"),
                () -> assertTrue(options.isShowHelp(), "Show help should be true")
            );
        }
    }

    @Nested
    @DisplayName("Index Limit Validation Tests")
    class IndexLimitValidationTests {

        @ParameterizedTest
        @ValueSource(ints = {1, 10, 100, 1000, 5000, 10000})
        @DisplayName("Should handle various positive index limits")
        void shouldHandlePositiveIndexLimits(int indexLimit) {
            String[] args = {"--index-limit", String.valueOf(indexLimit)};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals(indexLimit, options.getIndexLimit(), 
                "Index limit should be " + indexLimit);
        }

        @Test
        @DisplayName("Should handle zero index limit")
        void shouldHandleZeroIndexLimit() {
            String[] args = {"--index-limit", "0"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals(0, options.getIndexLimit(), "Index limit should be 0");
        }

        @Test
        @DisplayName("Should handle negative index limit")
        void shouldHandleNegativeIndexLimit() {
            String[] args = {"--index-limit", "-1"};
            
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw IllegalArgumentException for negative index limit");
        }

        @Test
        @DisplayName("Should handle very large index limit")
        void shouldHandleVeryLargeIndexLimit() {
            String[] args = {"--index-limit", String.valueOf(Integer.MAX_VALUE)};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals(Integer.MAX_VALUE, options.getIndexLimit(), 
                "Index limit should be Integer.MAX_VALUE");
        }

        @Test
        @DisplayName("Should handle invalid numeric index limit")
        void shouldHandleInvalidNumericIndexLimit() {
            String[] args = {"--index-limit", "not-a-number"};
            
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw IllegalArgumentException for non-numeric index limit");
        }

        @Test
        @DisplayName("Should handle missing index limit value")
        void shouldHandleMissingIndexLimitValue() {
            String[] args = {"--index-limit"};
            
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw IllegalArgumentException for missing index limit value");
        }

        @Test
        @DisplayName("Should handle index limit with extra spaces")
        void shouldHandleIndexLimitWithSpaces() {
            String[] args = {"--index-limit", "  100  "};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals(100, options.getIndexLimit(), "Index limit should be 100 after trimming spaces");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle unknown arguments gracefully")
        void shouldHandleUnknownArguments() {
            String[] args = {"--unknown-option", "value"};
            
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw IllegalArgumentException for unknown arguments");
        }

        @Test
        @DisplayName("Should handle malformed arguments")
        void shouldHandleMalformedArguments() {
            String[] args = {"malformed-argument"};
            
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw IllegalArgumentException for malformed arguments");
        }

        @Test
        @DisplayName("Should handle duplicate arguments")
        void shouldHandleDuplicateArguments() {
            String[] args = {"--index-limit", "100", "--index-limit", "200"};
            
            // Should use the last value or throw exception depending on implementation
            assertDoesNotThrow(() -> {
                CommandLineOptions options = CommandLineOptions.parseArgs(args);
                assertTrue(options.getIndexLimit() == 100 || options.getIndexLimit() == 200,
                    "Should handle duplicate arguments consistently");
            }, "Should handle duplicate arguments gracefully");
        }

        @Test
        @DisplayName("Should handle empty string arguments")
        void shouldHandleEmptyStringArguments() {
            String[] args = {"", "--verbose", ""};
            
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw IllegalArgumentException for empty string arguments");
        }

        @Test
        @DisplayName("Should provide meaningful error messages")
        void shouldProvideMeaningfulErrorMessages() {
            String[] args = {"--index-limit", "invalid"};
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw IllegalArgumentException for invalid arguments");
            
            assertNotNull(exception.getMessage(), "Exception message should not be null");
            assertFalse(exception.getMessage().isEmpty(), "Exception message should not be empty");
            assertTrue(exception.getMessage().length() > 10, "Exception message should be meaningful");
        }
    }

    @Nested
    @DisplayName("Help Functionality Tests")
    class HelpFunctionalityTests {

        @Test
        @DisplayName("Should print usage information")
        void shouldPrintUsageInformation() {
            CommandLineOptions.printUsage();
            
            String output = outputStream.toString();
            assertFalse(output.isEmpty(), "Usage output should not be empty");
            assertTrue(output.contains("Usage"), "Usage output should contain 'Usage'");
            assertTrue(output.contains("--index-limit"), "Usage output should contain --index-limit option");
            assertTrue(output.contains("--verbose"), "Usage output should contain --verbose option");
            assertTrue(output.contains("--help"), "Usage output should contain --help option");
        }

        @Test
        @DisplayName("Should print comprehensive help information")
        void shouldPrintComprehensiveHelpInformation() {
            CommandLineOptions.printUsage();
            
            String output = outputStream.toString();
            assertTrue(output.contains("-l"), "Help should contain short form -l");
            assertTrue(output.contains("-v"), "Help should contain short form -v");
            assertTrue(output.contains("-h"), "Help should contain short form -h");
            assertTrue(output.length() > 100, "Help output should be comprehensive");
        }

        @Test
        @DisplayName("Should handle help request properly")
        void shouldHandleHelpRequestProperly() {
            String[] args = {"--help"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertTrue(options.isShowHelp(), "Should indicate help was requested");
            
            // When help is requested, other defaults should still be set
            assertEquals(Integer.MAX_VALUE, options.getIndexLimit(), "Default index limit should be set");
            assertFalse(options.isVerbose(), "Default verbose should be set");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle single character arguments")
        void shouldHandleSingleCharacterArguments() {
            String[] args = {"-l", "1", "-v", "-h"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("Single character arguments should be parsed correctly",
                () -> assertEquals(1, options.getIndexLimit(), "Index limit should be 1"),
                () -> assertTrue(options.isVerbose(), "Verbose should be true"),
                () -> assertTrue(options.isShowHelp(), "Show help should be true")
            );
        }

        @Test
        @DisplayName("Should handle arguments with equals sign")
        void shouldHandleArgumentsWithEqualsSign() {
            // This test depends on whether the implementation supports --option=value format
            String[] args = {"--index-limit=250"};
            
            // This might throw an exception if equals format is not supported
            assertDoesNotThrow(() -> {
                CommandLineOptions options = CommandLineOptions.parseArgs(args);
                // If supported, should parse correctly; if not, should throw exception
            }, "Should handle equals format consistently");
        }

        @Test
        @DisplayName("Should handle case sensitivity")
        void shouldHandleCaseSensitivity() {
            String[] args = {"--INDEX-LIMIT", "100"};
            
            // Should be case sensitive and throw exception for wrong case
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should be case sensitive for argument names");
        }

        @Test
        @DisplayName("Should handle arguments with special characters")
        void shouldHandleArgumentsWithSpecialCharacters() {
            String[] args = {"--index-limit", "100", "--verbose", "extra-arg-with-dashes"};
            
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should handle special characters in arguments appropriately");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete workflow")
        void shouldHandleCompleteWorkflow() {
            String[] args = {"--index-limit", "1000", "--verbose", "--incremental", "--month", "january"};
            
            // Parse arguments
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            assertNotNull(options, "Options should be parsed");
            
            // Verify all options are set correctly
            assertAll("All options should be parsed correctly",
                () -> assertEquals(1000, options.getIndexLimit(), "Index limit should be 1000"),
                () -> assertTrue(options.isVerbose(), "Verbose should be enabled"),
                () -> assertTrue(options.isIncrementalUpdates(), "Incremental updates should be enabled"),
                () -> assertEquals("january", options.getMonthName(), "Month name should be january")
            );
        }

        @Test
        @DisplayName("Should handle toString method correctly")
        void shouldHandleToStringMethodCorrectly() {
            String[] args = {"--incremental", "--month", "december", "--verbose"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            String toString = options.toString();
            
            assertAll("toString should contain all relevant information",
                () -> assertTrue(toString.contains("incrementalUpdates=true"), "Should show incremental updates"),
                () -> assertTrue(toString.contains("monthName='december'"), "Should show month name"),
                () -> assertTrue(toString.contains("verbose=true"), "Should show verbose setting")
            );
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should parse arguments quickly")
        void shouldParseArgumentsQuickly() {
            String[] args = {"--index-limit", "1000", "--verbose", "--help"};
            
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                CommandLineOptions.parseArgs(args);
            }
            long endTime = System.currentTimeMillis();
            
            long totalTime = endTime - startTime;
            assertTrue(totalTime < 1000, "Should parse 1000 argument sets in under 1 second");
        }

        @Test
        @DisplayName("Should handle large argument arrays efficiently")
        void shouldHandleLargeArgumentArraysEfficiently() {
            // Create large argument array (though not realistic for command line)
            String[] args = new String[1000];
            for (int i = 0; i < 999; i += 2) {
                args[i] = "--unknown-option-" + i;
                args[i + 1] = "value" + i;
            }
            args[998] = "--index-limit";
            args[999] = "100";
            
            long startTime = System.currentTimeMillis();
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should handle large arrays and still validate properly");
            long endTime = System.currentTimeMillis();
            
            long totalTime = endTime - startTime;
            assertTrue(totalTime < 100, "Should process large argument array quickly");
        }
    }

    @Nested
    @DisplayName("Month Option Tests")
    class MonthOptionTests {

        @Test
        @DisplayName("Should parse valid month with incremental mode")
        void shouldParseValidMonthWithIncrementalMode() {
            String[] args = {"--incremental", "--month", "january"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("Month option should be parsed correctly",
                () -> assertTrue(options.isIncrementalUpdates(), "Incremental updates should be enabled"),
                () -> assertEquals("january", options.getMonthName(), "Month name should be january")
            );
        }

        @Test
        @DisplayName("Should parse valid month with incremental downloads")
        void shouldParseValidMonthWithIncrementalDownloads() {
            String[] args = {"--incremental-downloads", "--month", "december"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("Month option should be parsed correctly",
                () -> assertTrue(options.isIncrementalDownloads(), "Incremental downloads should be enabled"),
                () -> assertEquals("december", options.getMonthName(), "Month name should be december")
            );
        }

        @Test
        @DisplayName("Should parse valid month with incremental processing")
        void shouldParseValidMonthWithIncrementalProcessing() {
            String[] args = {"--incremental-processing", "--month", "june"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("Month option should be parsed correctly",
                () -> assertTrue(options.isIncrementalProcessing(), "Incremental processing should be enabled"),
                () -> assertEquals("june", options.getMonthName(), "Month name should be june")
            );
        }

        @Test
        @DisplayName("Should handle month names case insensitively")
        void shouldHandleMonthNamesCaseInsensitively() {
            String[] args = {"--incremental", "--month", "MARCH"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals("march", options.getMonthName(), "Month name should be normalized to lowercase");
        }

        @Test
        @DisplayName("Should parse month with equals format")
        void shouldParseMonthWithEqualsFormat() {
            String[] args = {"--incremental", "--month=april"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals("april", options.getMonthName(), "Month name should be parsed from equals format");
        }

        @Test
        @DisplayName("Should throw exception for invalid month name")
        void shouldThrowExceptionForInvalidMonthName() {
            String[] args = {"--incremental", "--month", "invalidmonth"};
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw exception for invalid month name");
            
            assertTrue(exception.getMessage().contains("Invalid month name"), 
                "Exception message should mention invalid month name");
            assertTrue(exception.getMessage().contains("invalidmonth"), 
                "Exception message should include the invalid month name");
        }

        @Test
        @DisplayName("Should throw exception for month without incremental mode")
        void shouldThrowExceptionForMonthWithoutIncrementalMode() {
            String[] args = {"--month", "january"};
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw exception when month is used without incremental mode");
            
            assertTrue(exception.getMessage().contains("--month option can only be used with incremental mode"), 
                "Exception message should explain month requires incremental mode");
        }

        @Test
        @DisplayName("Should throw exception for missing month value")
        void shouldThrowExceptionForMissingMonthValue() {
            String[] args = {"--incremental", "--month"};
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw exception for missing month value");
            
            assertTrue(exception.getMessage().contains("Missing value for --month"), 
                "Exception message should mention missing value");
        }

        @Test
        @DisplayName("Should handle month with extra spaces")
        void shouldHandleMonthWithExtraSpaces() {
            String[] args = {"--incremental", "--month", "  february  "};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals("february", options.getMonthName(), "Month name should be trimmed");
        }

        @ParameterizedTest
        @ValueSource(strings = {"january", "february", "march", "april", "may", "june", 
                               "july", "august", "september", "october", "november", "december"})
        @DisplayName("Should accept all valid month names")
        void shouldAcceptAllValidMonthNames(String monthName) {
            String[] args = {"--incremental", "--month", monthName};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals(monthName, options.getMonthName(), "Should accept valid month: " + monthName);
        }

        @ParameterizedTest
        @ValueSource(strings = {"JANUARY", "February", "MaRcH", "APRIL"})
        @DisplayName("Should handle mixed case month names")
        void shouldHandleMixedCaseMonthNames(String monthName) {
            String[] args = {"--incremental", "--month", monthName};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertEquals(monthName.toLowerCase(), options.getMonthName(), 
                "Should normalize mixed case month: " + monthName);
        }

        @Test
        @DisplayName("Should work with combined incremental and month options")
        void shouldWorkWithCombinedIncrementalAndMonthOptions() {
            String[] args = {"--incremental", "--baseline-file", "test.csv", "--month", "september", "--verbose"};
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("All options should be parsed correctly",
                () -> assertTrue(options.isIncrementalUpdates(), "Incremental updates should be enabled"),
                () -> assertEquals("test.csv", options.getBaselineFilePath(), "Baseline file should be set"),
                () -> assertEquals("september", options.getMonthName(), "Month name should be september"),
                () -> assertTrue(options.isVerbose(), "Verbose should be enabled")
            );
        }
    }

    @Nested
    @DisplayName("Parameterized Tests")
    static class ParameterizedTests {

        @ParameterizedTest
        @CsvSource({
            "'--index-limit, 100', 100, false, false",
            "'--verbose', 2147483647, true, false",
            "'--help', 2147483647, false, true",
            "'-l, 50, -v', 50, true, false",
            "'-h', 2147483647, false, true"
        })
        @DisplayName("Should parse various argument combinations")
        void shouldParseVariousArgumentCombinations(String argsString, int expectedLimit, 
                boolean expectedVerbose, boolean expectedHelp) {
            String[] args = argsString.split(", ");
            
            CommandLineOptions options = CommandLineOptions.parseArgs(args);
            
            assertAll("All parsed values should match expected",
                () -> assertEquals(expectedLimit, options.getIndexLimit(), "Index limit should match"),
                () -> assertEquals(expectedVerbose, options.isVerbose(), "Verbose should match"),
                () -> assertEquals(expectedHelp, options.isShowHelp(), "Help should match")
            );
        }

        static Stream<Arguments> invalidArgumentsProvider() {
            return Stream.of(
                Arguments.of((Object) new String[]{"--index-limit", "-1"}),
                Arguments.of((Object) new String[]{"--index-limit", "abc"}),
                Arguments.of((Object) new String[]{"--unknown-option"}),
                Arguments.of((Object) new String[]{"--index-limit"}),
                Arguments.of((Object) new String[]{"invalid-format"})
            );
        }

        @ParameterizedTest
        @MethodSource("invalidArgumentsProvider")
        @DisplayName("Should throw exceptions for invalid arguments")
        void shouldThrowExceptionsForInvalidArguments(String[] args) {
            assertThrows(IllegalArgumentException.class, () -> {
                CommandLineOptions.parseArgs(args);
            }, "Should throw IllegalArgumentException for invalid arguments: " + String.join(" ", args));
        }
    }
}
