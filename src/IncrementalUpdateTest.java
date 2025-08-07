import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test class to demonstrate the incremental update functionality
 */
public class IncrementalUpdateTest {
    
    public static void main(String[] args) {
        System.out.println("=== Incremental Update Test ===");
        
        try {
            // Test 1: Command line parsing with incremental options
            testCommandLineParsing();
            
            // Test 2: ProcessingContext with incremental options
            testProcessingContextWithIncremental();
            
            // Test 3: IncrementalUpdateManager functionality
            testIncrementalUpdateManager();
            
            // Test 4: Date parsing and comparison
            testDateParsing();
            
            System.out.println("\n=== All Incremental Update Tests Completed Successfully ===");
            
        } catch (Exception e) {
            System.err.println("Error in incremental update test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testCommandLineParsing() {
        System.out.println("\n--- Test 1: Command Line Parsing ---");
        
        // Test basic incremental option
        String[] args1 = {"--incremental", "--baseline-file", "./Data/Output/IAPD_Data.csv"};
        CommandLineOptions options1 = CommandLineOptions.parseArgs(args1);
        
        System.out.println("Basic incremental test:");
        System.out.println("  Incremental Updates: " + options1.isIncrementalUpdates());
        System.out.println("  Incremental Downloads: " + options1.isIncrementalDownloads());
        System.out.println("  Incremental Processing: " + options1.isIncrementalProcessing());
        System.out.println("  Baseline File Path: " + options1.getBaselineFilePath());
        
        // Test individual incremental options
        String[] args2 = {"--incremental-downloads", "--baseline-file", "./Data/Output/IAPD_Data_20240101.csv", "--verbose"};
        CommandLineOptions options2 = CommandLineOptions.parseArgs(args2);
        
        System.out.println("\nIndividual incremental options test:");
        System.out.println("  Incremental Downloads: " + options2.isIncrementalDownloads());
        System.out.println("  Incremental Processing: " + options2.isIncrementalProcessing());
        System.out.println("  Baseline File Path: " + options2.getBaselineFilePath());
        System.out.println("  Verbose: " + options2.isVerbose());
        
        // Test incremental processing only
        String[] args3 = {"--incremental-processing", "--baseline-file", "./Data/Output/IAPD_Data.csv"};
        CommandLineOptions options3 = CommandLineOptions.parseArgs(args3);
        
        System.out.println("\nIncremental processing only test:");
        System.out.println("  Incremental Downloads: " + options3.isIncrementalDownloads());
        System.out.println("  Incremental Processing: " + options3.isIncrementalProcessing());
        System.out.println("  Baseline File Path: " + options3.getBaselineFilePath());
    }
    
    private static void testProcessingContextWithIncremental() {
        System.out.println("\n--- Test 2: ProcessingContext with Incremental ---");
        
        // Test context from command line options
        String[] args = {"--incremental", "--baseline-file", "./Data/Output/IAPD_Data.csv", "--verbose", "--index-limit", "100"};
        CommandLineOptions options = CommandLineOptions.parseArgs(args);
        ProcessingContext context1 = ProcessingContext.fromCommandLineOptions(options);
        
        System.out.println("Context from command line:");
        System.out.println("  Index Limit: " + context1.getIndexLimit());
        System.out.println("  Incremental Updates: " + context1.isIncrementalUpdates());
        System.out.println("  Incremental Downloads: " + context1.isIncrementalDownloads());
        System.out.println("  Incremental Processing: " + context1.isIncrementalProcessing());
        System.out.println("  Baseline File Path: " + context1.getBaselineFilePath());
        System.out.println("  Verbose: " + context1.isVerbose());
        
        // Test context using builder pattern
        ProcessingContext context2 = ProcessingContext.builder()
                .indexLimit(50)
                .incrementalDownloads(true)
                .incrementalProcessing(false)
                .baselineFilePath("./Data/Output/IAPD_Data_20240101.csv")
                .verbose(true)
                .configSource("test-builder")
                .build();
        
        System.out.println("\nContext from builder:");
        System.out.println("  Index Limit: " + context2.getIndexLimit());
        System.out.println("  Incremental Downloads: " + context2.isIncrementalDownloads());
        System.out.println("  Incremental Processing: " + context2.isIncrementalProcessing());
        System.out.println("  Baseline File Path: " + context2.getBaselineFilePath());
        System.out.println("  Config Source: " + context2.getConfigSource());
    }
    
    private static void testIncrementalUpdateManager() {
        System.out.println("\n--- Test 3: IncrementalUpdateManager ---");
        
        IncrementalUpdateManager incrementalManager = new IncrementalUpdateManager();
        
        // Test date comparison logic
        System.out.println("Date comparison tests:");
        System.out.println("  01/15/2024 vs 01/10/2024: " + incrementalManager.isFilingDateMoreRecent("01/15/2024", "01/10/2024"));
        System.out.println("  01/10/2024 vs 01/15/2024: " + incrementalManager.isFilingDateMoreRecent("01/10/2024", "01/15/2024"));
        System.out.println("  01/15/2024 vs 01/15/2024: " + incrementalManager.isFilingDateMoreRecent("01/15/2024", "01/15/2024"));
        System.out.println("  01/15/2024 vs null: " + incrementalManager.isFilingDateMoreRecent("01/15/2024", null));
        System.out.println("  null vs 01/15/2024: " + incrementalManager.isFilingDateMoreRecent(null, "01/15/2024"));
        
        // Test incremental file name generation
        System.out.println("\nFile name generation tests:");
        System.out.println("  Incremental XML file: " + incrementalManager.generateIncrementalFileName("IA_FIRM_SEC_DATA", "20250107", ".csv"));
        System.out.println("  Incremental download file: " + incrementalManager.generateIncrementalFileName("IA_FIRM_SEC_DATA", "20250107", "_with_downloads.csv"));
        
        // Test incremental statistics calculation
        System.out.println("\nIncremental statistics tests:");
        
        // Create mock current firms data
        List<FirmData> mockCurrentFirms = createMockFirmData();
        
        // Create mock historical dates
        Map<String, String> mockHistoricalDates = new HashMap<>();
        mockHistoricalDates.put("123456", "01/10/2024");  // Older date - should be updated
        mockHistoricalDates.put("234567", "01/20/2024");  // Same date - should be skipped
        mockHistoricalDates.put("345678", "01/25/2024");  // Newer date - should be skipped
        // 456789 not in historical - should be processed as new
        
        IncrementalUpdateManager.IncrementalStats stats = incrementalManager.calculateIncrementalStats(mockCurrentFirms, mockHistoricalDates);
        
        System.out.println("  Incremental Stats: " + stats.toString());
        
        // Test firms to process determination
        Set<String> firmsToProcess = incrementalManager.getFirmsToProcess(mockCurrentFirms, mockHistoricalDates);
        System.out.println("  Firms to process: " + firmsToProcess);
    }
    
    private static void testDateParsing() {
        System.out.println("\n--- Test 4: Date Parsing ---");
        
        IncrementalUpdateManager incrementalManager = new IncrementalUpdateManager();
        
        // Test various date formats
        String[] testDates = {
            "01/15/2024",
            "1/15/2024",
            "12/31/2023",
            "02/29/2024",  // Leap year
            "invalid-date",
            null,
            "",
            "  01/15/2024  "  // With whitespace
        };
        
        System.out.println("Date parsing tests:");
        for (String dateStr : testDates) {
            java.util.Date parsed = incrementalManager.parseFilingDate(dateStr);
            System.out.println("  '" + dateStr + "' -> " + (parsed != null ? parsed.toString() : "null"));
        }
    }
    
    /**
     * Creates mock firm data for testing
     */
    private static List<FirmData> createMockFirmData() {
        List<FirmData> firms = new ArrayList<>();
        
        // Firm with older filing date (should be updated)
        firms.add(new FirmData("SEC123", "123456", "SEC456", "Test Firm 1", "Test Legal 1",
                "123 Main St", "", "New York", "NY", "USA", "10001", "555-1234", "555-5678",
                "Investment Adviser", "NY", "01/01/2020", "01/15/2024", "1.0", "50", "1000000", "100",
                "http://example.com/brochure1.pdf"));
        
        // Firm with same filing date (should be skipped)
        firms.add(new FirmData("SEC234", "234567", "SEC567", "Test Firm 2", "Test Legal 2",
                "456 Oak Ave", "", "Boston", "MA", "USA", "02101", "555-2345", "555-6789",
                "Investment Adviser", "MA", "01/01/2020", "01/20/2024", "1.0", "75", "2000000", "200",
                "http://example.com/brochure2.pdf"));
        
        // Firm with newer filing date (should be skipped)
        firms.add(new FirmData("SEC345", "345678", "SEC678", "Test Firm 3", "Test Legal 3",
                "789 Pine St", "", "Chicago", "IL", "USA", "60601", "555-3456", "555-7890",
                "Investment Adviser", "IL", "01/01/2020", "01/30/2024", "1.0", "100", "3000000", "300",
                "http://example.com/brochure3.pdf"));
        
        // New firm not in historical data (should be processed)
        firms.add(new FirmData("SEC456", "456789", "SEC789", "Test Firm 4", "Test Legal 4",
                "321 Elm St", "", "Seattle", "WA", "USA", "98101", "555-4567", "555-8901",
                "Investment Adviser", "WA", "01/01/2020", "01/25/2024", "1.0", "25", "500000", "50",
                "http://example.com/brochure4.pdf"));
        
        return firms;
    }
    
    /**
     * Demonstrates the incremental workflow
     */
    public static void demonstrateIncrementalWorkflow() {
        System.out.println("\n=== Incremental Update Workflow Demonstration ===");
        
        System.out.println("1. Initial Run (Full Processing):");
        System.out.println("   - Process 10,000 firms from XML");
        System.out.println("   - Download 9,500 brochures");
        System.out.println("   - Analyze all brochures");
        System.out.println("   - Output: IAPD_Data.csv with 10,000 firms");
        
        System.out.println("\n2. Daily Update (Incremental Processing):");
        System.out.println("   - New XML contains 10,200 firms");
        System.out.println("   - Incremental analysis: 150 new firms, 50 updated firms");
        System.out.println("   - Process only 200 firms (98% reduction!)");
        System.out.println("   - Download 200 brochures");
        System.out.println("   - Analyze 200 brochures");
        System.out.println("   - Append results to existing IAPD_Data.csv");
        
        System.out.println("\n3. Incremental Benefits:");
        System.out.println("   - Time savings: 98% reduction in processing time");
        System.out.println("   - Network efficiency: Only download new/updated brochures");
        System.out.println("   - Data integrity: Cumulative results in IAPD_Data.csv");
        System.out.println("   - Audit trail: Incremental CSV files show what was processed");
    }
}
