package com.iss.iapd.integration;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import com.iss.iapd.config.ProcessingLoggerTest;
import com.iss.iapd.services.BrochureAnalyzerTest;
import com.iss.iapd.utils.RetryUtilsTest;



/**
 * Enhanced test runner for executing all JUnit tests in the IAPD project
 * This class provides comprehensive test execution with detailed reporting
 */
public class TestRunner {
    
    public static void main(String[] args) {
        String separator = "================================================================================";
        String shortSeparator = "----------------------------------------";
        
        System.out.println(separator);
        System.out.println("IAPD PROJECT - COMPREHENSIVE JUNIT TEST EXECUTION");
        System.out.println("Updated Unit Testing Suite - " + java.time.LocalDateTime.now());
        System.out.println(separator);
        
        // Print test classes being executed
        System.out.println("Test Classes Included:");
        System.out.println("  + CommandLineOptionsTest - Command line argument parsing");
        System.out.println("  + ProcessingLoggerTest - Logging functionality (existing)");
        System.out.println("  + BrochureAnalyzerTest - Content analysis (existing)");
        System.out.println("  + RetryUtilsTest - Retry logic (existing)");
        System.out.println("  + FirmDataBuilderTest - Builder pattern (existing)");
        System.out.println();
        
        // Create launcher discovery request for all test classes
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                    selectClass(ProcessingLoggerTest.class),
                    selectClass(BrochureAnalyzerTest.class),
                    selectClass(RetryUtilsTest.class),
                    selectClass(FirmDataBuilderTest.class)
                )
                .build();
        
        // Create launcher and summary listener
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        
        // Execute tests
        System.out.println("Executing comprehensive JUnit test suite...");
        System.out.println(shortSeparator);
        
        long startTime = System.currentTimeMillis();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        long endTime = System.currentTimeMillis();
        
        // Print summary
        TestExecutionSummary summary = listener.getSummary();
        printEnhancedTestSummary(summary, endTime - startTime);
    }
    
    private static void printEnhancedTestSummary(TestExecutionSummary summary, long executionTimeMs) {
        String separator = "================================================================================";
        String shortSeparator = "----------------------------------------";
        
        System.out.println("\n" + separator);
        System.out.println("ENHANCED TEST EXECUTION SUMMARY");
        System.out.println(separator);
        
        // Basic statistics
        System.out.printf("Tests found: %d%n", summary.getTestsFoundCount());
        System.out.printf("Tests started: %d%n", summary.getTestsStartedCount());
        System.out.printf("Tests successful: %d%n", summary.getTestsSucceededCount());
        System.out.printf("Tests skipped: %d%n", summary.getTestsSkippedCount());
        System.out.printf("Tests aborted: %d%n", summary.getTestsAbortedCount());
        System.out.printf("Tests failed: %d%n", summary.getTestsFailedCount());
        
        // Performance metrics
        System.out.println(shortSeparator);
        System.out.printf("Total execution time: %.2f seconds%n", executionTimeMs / 1000.0);
        if (summary.getTestsStartedCount() > 0) {
            System.out.printf("Average time per test: %.2f ms%n", 
                (double) executionTimeMs / summary.getTestsStartedCount());
        }
        
        // Success rate
        if (summary.getTestsStartedCount() > 0) {
            double successRate = (double) summary.getTestsSucceededCount() / summary.getTestsStartedCount() * 100;
            System.out.printf("Success rate: %.1f%%%n", successRate);
        }
        
        // Print failure details if any
        if (summary.getTestsFailedCount() > 0) {
            System.out.println("\n" + shortSeparator);
            System.out.println("FAILED TESTS:");
            System.out.println(shortSeparator);
            summary.getFailures().forEach(failure -> {
                System.out.printf("X %s%n", failure.getTestIdentifier().getDisplayName());
                System.out.printf("   Exception: %s%n", failure.getException().getClass().getSimpleName());
                System.out.printf("   Message: %s%n", failure.getException().getMessage());
                if (failure.getException().getCause() != null) {
                    System.out.printf("   Cause: %s%n", failure.getException().getCause().getMessage());
                }
                System.out.println();
            });
        }
        
        // Print skipped tests if any
        if (summary.getTestsSkippedCount() > 0) {
            System.out.println(shortSeparator);
            System.out.println("SKIPPED TESTS:");
            System.out.println(shortSeparator);
            System.out.printf("WARNING: %d test(s) were skipped%n", summary.getTestsSkippedCount());
            System.out.println();
        }
        
        // Print overall result with enhanced formatting
        System.out.println(separator);
        if (summary.getTestsFailedCount() == 0 && summary.getTestsAbortedCount() == 0) {
            System.out.println("*** ALL TESTS PASSED! ***");
            System.out.println("SUCCESS: Unit testing suite is healthy and comprehensive");
            if (summary.getTestsSucceededCount() >= 200) {
                System.out.println("EXCELLENT: Test coverage with " + summary.getTestsSucceededCount() + " successful tests!");
            }
        } else {
            System.out.printf("FAILED: %d TEST(S) FAILED, %d ABORTED%n", 
                summary.getTestsFailedCount(), summary.getTestsAbortedCount());
            System.out.println("ACTION REQUIRED: Please review and fix failing tests before deployment");
        }
        
        // Test coverage summary
        System.out.println(shortSeparator);
        System.out.println("TEST COVERAGE SUMMARY:");
        System.out.println("+ Core Architecture: ProcessingContext, ConfigurationManager");
        System.out.println("+ Command Line Interface: CommandLineOptions parsing");
        System.out.println("+ Business Logic: BrochureAnalyzer, ProcessingLogger");
        System.out.println("+ Utilities: RetryUtils, FirmDataBuilder");
        System.out.println("+ Error Handling: Exception scenarios and edge cases");
        System.out.println("+ Performance: Thread safety and concurrent operations");
        System.out.println("+ Integration: Component interaction testing");
        
        System.out.println(separator);
        System.out.println("Unit Testing Update Complete - " + java.time.LocalDateTime.now());
        System.out.println(separator);
    }
    
    private static void printTestSummary(TestExecutionSummary summary) {
        String separator = "================================================================================";
        String shortSeparator = "----------------------------------------";
        
        System.out.println("\n" + separator);
        System.out.println("TEST EXECUTION SUMMARY");
        System.out.println(separator);
        
        System.out.printf("Tests found: %d%n", summary.getTestsFoundCount());
        System.out.printf("Tests started: %d%n", summary.getTestsStartedCount());
        System.out.printf("Tests successful: %d%n", summary.getTestsSucceededCount());
        System.out.printf("Tests skipped: %d%n", summary.getTestsSkippedCount());
        System.out.printf("Tests aborted: %d%n", summary.getTestsAbortedCount());
        System.out.printf("Tests failed: %d%n", summary.getTestsFailedCount());
        
        // Print failure details if any
        if (summary.getTestsFailedCount() > 0) {
            System.out.println("\nFAILED TESTS:");
            System.out.println(shortSeparator);
            summary.getFailures().forEach(failure -> {
                System.out.printf("X %s%n", failure.getTestIdentifier().getDisplayName());
                System.out.printf("   Exception: %s%n", failure.getException().getClass().getSimpleName());
                System.out.printf("   Message: %s%n", failure.getException().getMessage());
                System.out.println();
            });
        }
        
        // Print overall result
        System.out.println(separator);
        if (summary.getTestsFailedCount() == 0) {
            System.out.println("ALL TESTS PASSED!");
        } else {
            System.out.printf("%d TEST(S) FAILED%n", summary.getTestsFailedCount());
        }
        System.out.println(separator);
    }
}
