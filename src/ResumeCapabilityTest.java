/**
 * Test class to demonstrate the resume capability for brochure downloading and processing
 */
public class ResumeCapabilityTest {
    
    public static void main(String[] args) {
        System.out.println("=== Resume Capability Test ===");
        
        try {
            // Test 1: Command line parsing with resume options
            testCommandLineParsing();
            
            // Test 2: ProcessingContext with resume options
            testProcessingContextWithResume();
            
            // Test 3: ResumeStateManager functionality
            testResumeStateManager();
            
            System.out.println("\n=== All Resume Tests Completed Successfully ===");
            
        } catch (Exception e) {
            System.err.println("Error in resume capability test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testCommandLineParsing() {
        System.out.println("\n--- Test 1: Command Line Parsing ---");
        
        // Test basic resume option
        String[] args1 = {"--resume", "--index-limit", "100"};
        CommandLineOptions options1 = CommandLineOptions.parseArgs(args1);
        
        System.out.println("Basic resume test:");
        System.out.println("  Resume Enabled: " + options1.isResumeEnabled());
        System.out.println("  Resume Downloads: " + options1.isResumeDownloads());
        System.out.println("  Resume Processing: " + options1.isResumeProcessing());
        System.out.println("  Validate PDFs: " + options1.isValidatePdfs());
        System.out.println("  Force Restart: " + options1.isForceRestart());
        
        // Test individual resume options
        String[] args2 = {"--resume-downloads", "--no-validate-pdfs", "--verbose"};
        CommandLineOptions options2 = CommandLineOptions.parseArgs(args2);
        
        System.out.println("\nIndividual resume options test:");
        System.out.println("  Resume Downloads: " + options2.isResumeDownloads());
        System.out.println("  Resume Processing: " + options2.isResumeProcessing());
        System.out.println("  Validate PDFs: " + options2.isValidatePdfs());
        System.out.println("  Verbose: " + options2.isVerbose());
        
        // Test force restart
        String[] args3 = {"--force-restart", "--resume-processing"};
        CommandLineOptions options3 = CommandLineOptions.parseArgs(args3);
        
        System.out.println("\nForce restart test:");
        System.out.println("  Force Restart: " + options3.isForceRestart());
        System.out.println("  Resume Processing: " + options3.isResumeProcessing());
    }
    
    private static void testProcessingContextWithResume() {
        System.out.println("\n--- Test 2: ProcessingContext with Resume ---");
        
        // Test context from command line options
        String[] args = {"--resume", "--validate-pdfs", "--verbose", "--index-limit", "50"};
        CommandLineOptions options = CommandLineOptions.parseArgs(args);
        ProcessingContext context1 = ProcessingContext.fromCommandLineOptions(options);
        
        System.out.println("Context from command line:");
        System.out.println("  Index Limit: " + context1.getIndexLimit());
        System.out.println("  Resume Downloads: " + context1.isResumeDownloads());
        System.out.println("  Resume Processing: " + context1.isResumeProcessing());
        System.out.println("  Validate PDFs: " + context1.isValidatePdfs());
        System.out.println("  Force Restart: " + context1.isForceRestart());
        System.out.println("  Verbose: " + context1.isVerbose());
        
        // Test context using builder pattern
        ProcessingContext context2 = ProcessingContext.builder()
                .indexLimit(25)
                .resumeDownloads(true)
                .resumeProcessing(false)
                .validatePdfs(false)
                .forceRestart(true)
                .verbose(true)
                .configSource("test-builder")
                .build();
        
        System.out.println("\nContext from builder:");
        System.out.println("  Index Limit: " + context2.getIndexLimit());
        System.out.println("  Resume Downloads: " + context2.isResumeDownloads());
        System.out.println("  Resume Processing: " + context2.isResumeProcessing());
        System.out.println("  Validate PDFs: " + context2.isValidatePdfs());
        System.out.println("  Force Restart: " + context2.isForceRestart());
        System.out.println("  Config Source: " + context2.getConfigSource());
    }
    
    private static void testResumeStateManager() {
        System.out.println("\n--- Test 3: ResumeStateManager ---");
        
        ResumeStateManager resumeManager = new ResumeStateManager();
        
        // Test download status retry logic
        System.out.println("Download retry logic tests:");
        System.out.println("  Should retry null status: " + resumeManager.shouldRetryDownload(null));
        System.out.println("  Should retry empty status: " + resumeManager.shouldRetryDownload(""));
        System.out.println("  Should retry FAILED: " + resumeManager.shouldRetryDownload("FAILED"));
        System.out.println("  Should retry ERROR: " + resumeManager.shouldRetryDownload("ERROR"));
        System.out.println("  Should retry SUCCESS: " + resumeManager.shouldRetryDownload("SUCCESS"));
        System.out.println("  Should retry SKIPPED: " + resumeManager.shouldRetryDownload("SKIPPED"));
        System.out.println("  Should retry NO_URL: " + resumeManager.shouldRetryDownload("NO_URL"));
        System.out.println("  Should retry INVALID_URL: " + resumeManager.shouldRetryDownload("INVALID_URL"));
        
        // Test resume statistics calculation
        System.out.println("\nResume statistics tests:");
        
        // Create mock download status data
        java.util.Map<String, String> mockStatus = new java.util.HashMap<>();
        mockStatus.put("123456", "SUCCESS");
        mockStatus.put("234567", "SUCCESS");
        mockStatus.put("345678", "FAILED");
        mockStatus.put("456789", "ERROR");
        mockStatus.put("567890", "NO_URL");
        mockStatus.put("678901", "SKIPPED");
        
        ResumeStateManager.ResumeStats downloadStats = resumeManager.calculateDownloadResumeStats(
                10, mockStatus, false);
        
        System.out.println("  Download Resume Stats: " + downloadStats.toString());
        
        // Create mock processed firms data
        java.util.Set<String> mockProcessed = new java.util.HashSet<>();
        mockProcessed.add("123456");
        mockProcessed.add("234567");
        mockProcessed.add("345678");
        
        ResumeStateManager.ResumeStats processingStats = resumeManager.calculateProcessingResumeStats(
                8, mockProcessed);
        
        System.out.println("  Processing Resume Stats: " + processingStats.toString());
    }
    
    /**
     * Demonstrates the resume workflow
     */
    public static void demonstrateResumeWorkflow() {
        System.out.println("\n=== Resume Workflow Demonstration ===");
        
        System.out.println("1. Initial Run:");
        System.out.println("   - Process 1000 firms");
        System.out.println("   - Download 800 brochures successfully");
        System.out.println("   - 150 downloads failed");
        System.out.println("   - 50 firms had no brochure URL");
        System.out.println("   - Process crashes after analyzing 600 brochures");
        
        System.out.println("\n2. Resume Run with --resume:");
        System.out.println("   - Skip 800 successful downloads");
        System.out.println("   - Retry 150 failed downloads");
        System.out.println("   - Validate 800 existing PDF files");
        System.out.println("   - Skip 600 already analyzed brochures");
        System.out.println("   - Process remaining 200 brochures");
        
        System.out.println("\n3. Resume Benefits:");
        System.out.println("   - Save time: Skip 800 downloads (80% time saved)");
        System.out.println("   - Reliability: Automatic retry of failed downloads");
        System.out.println("   - Data integrity: PDF validation ensures quality");
        System.out.println("   - Progress tracking: Clear visibility into remaining work");
    }
}
