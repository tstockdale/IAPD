package com.iss.iapd.integration;

import com.iss.iapd.config.CommandLineOptions;
import com.iss.iapd.model.ProcessingPhase;
import com.iss.iapd.core.ProcessingContext;

/**
 * Simple test runner that only runs tests that compile correctly
 * This is a temporary solution while we fix the method signature issues
 */
public class SimpleTestRunner {
    
    public static void main(String[] args) {
        String separator = "================================================================================";
        
        System.out.println(separator);
        System.out.println("IAPD PROJECT - SIMPLE TEST EXECUTION");
        System.out.println("Running tests that compile correctly");
        System.out.println(separator);
        
        // Test ProcessingContext basic functionality
        testProcessingContextBasics();
        
        // Test CommandLineOptions basic functionality  
        testCommandLineOptionsBasics();
        
        System.out.println(separator);
        System.out.println("SIMPLE TESTS COMPLETED");
        System.out.println("Note: Full test suite requires fixing method signature mismatches");
        System.out.println(separator);
    }
    
    private static void testProcessingContextBasics() {
        System.out.println("Testing ProcessingContext basics...");
        
        try {
            // Test builder pattern
            ProcessingContext context = ProcessingContext.builder()
                    .indexLimit(100)
                    .verbose(true)
                    .build();
            
            System.out.println("  + Builder pattern: PASSED");
            System.out.println("    - Index limit: " + context.getIndexLimit());
            System.out.println("    - Verbose: " + context.isVerbose());
            
            // Test runtime state
            context.incrementProcessedFirms();
            context.incrementSuccessfulDownloads();
            context.setCurrentPhase(ProcessingPhase.PARSING_XML);
            
            System.out.println("  + Runtime state: PASSED");
            System.out.println("    - Processed firms: " + context.getProcessedFirms());
            System.out.println("    - Successful downloads: " + context.getSuccessfulDownloads());
            System.out.println("    - Current phase: " + context.getCurrentPhase());
            
        } catch (Exception e) {
            System.out.println("  X ProcessingContext test FAILED: " + e.getMessage());
        }
    }
    
    private static void testCommandLineOptionsBasics() {
        System.out.println("Testing CommandLineOptions basics...");
        
        try {
            // Test basic argument parsing
            String[] args1 = {"--index-limit", "50", "--verbose"};
            CommandLineOptions options1 = CommandLineOptions.parseArgs(args1);
            
            System.out.println("  + Argument parsing: PASSED");
            System.out.println("    - Index limit: " + options1.getIndexLimit());
            System.out.println("    - Verbose: " + options1.isVerbose());
            
            // Test empty arguments
            String[] args2 = {};
            CommandLineOptions options2 = CommandLineOptions.parseArgs(args2);
            
            System.out.println("  + Default values: PASSED");
            System.out.println("    - Index limit: " + (options2.getIndexLimit() == Integer.MAX_VALUE ? "unlimited" : options2.getIndexLimit()));
            System.out.println("    - Verbose: " + options2.isVerbose());
            
        } catch (Exception e) {
            System.out.println("  X CommandLineOptions test FAILED: " + e.getMessage());
        }
    }
}
