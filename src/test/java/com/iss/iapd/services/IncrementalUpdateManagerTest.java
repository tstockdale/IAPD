package com.iss.iapd.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import com.iss.iapd.model.FirmData;
import com.iss.iapd.model.FirmDataBuilder;
import com.iss.iapd.services.incremental.IncrementalUpdateManager;


/**
 * Comprehensive JUnit 5 test suite for IncrementalUpdateManager
 * Tests date parsing, file comparison logic, and incremental statistics
 */
public class IncrementalUpdateManagerTest {
    
    private IncrementalUpdateManager manager;
    
    @BeforeEach
    void setUp() {
        manager = new IncrementalUpdateManager();
    }
    
    @Nested
    @DisplayName("Date Parsing Tests")
    class DateParsingTests {
        
        @Test
        @DisplayName("Should parse valid date strings")
        void testParseValidDates() {
            Date result1 = manager.parseFilingDate("01/15/2024");
            Date result2 = manager.parseFilingDate("12/31/2023");
            Date result3 = manager.parseFilingDate("06/01/2025");
            
            assertNotNull(result1);
            assertNotNull(result2);
            assertNotNull(result3);
            
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
            assertEquals("01/15/2024", formatter.format(result1));
            assertEquals("12/31/2023", formatter.format(result2));
            assertEquals("06/01/2025", formatter.format(result3));
        }
        
        @Test
        @DisplayName("Should handle null and empty date strings")
        void testParseNullAndEmptyDates() {
            assertNull(manager.parseFilingDate(null));
            assertNull(manager.parseFilingDate(""));
            assertNull(manager.parseFilingDate("   "));
        }
        
        @Test
        @DisplayName("Should handle invalid date formats")
        void testParseInvalidDateFormats() {
            assertNull(manager.parseFilingDate("2024-01-15"));
            assertNull(manager.parseFilingDate("15/01/2024"));
            assertNull(manager.parseFilingDate("invalid"));
            assertNull(manager.parseFilingDate("13/32/2024"));
        }
        
        @Test
        @DisplayName("Should handle edge case dates")
        void testParseEdgeCaseDates() {
            Date leapYear = manager.parseFilingDate("02/29/2024");
            Date nonLeapYear = manager.parseFilingDate("02/29/2023");
            
            assertNotNull(leapYear);
            assertNull(nonLeapYear); // 2023 is not a leap year
        }
    }
    
    @Nested
    @DisplayName("Date Comparison Tests")
    class DateComparisonTests {
        
        @Test
        @DisplayName("Should correctly compare more recent dates")
        void testIsFilingDateMoreRecent() {
            assertTrue(manager.isFilingDateMoreRecent("01/15/2024", "01/14/2024"));
            assertTrue(manager.isFilingDateMoreRecent("02/01/2024", "01/31/2024"));
            assertTrue(manager.isFilingDateMoreRecent("01/01/2025", "12/31/2024"));
            
            assertFalse(manager.isFilingDateMoreRecent("01/14/2024", "01/15/2024"));
            assertFalse(manager.isFilingDateMoreRecent("01/31/2024", "02/01/2024"));
            assertFalse(manager.isFilingDateMoreRecent("12/31/2023", "01/01/2024"));
        }
        
        @Test
        @DisplayName("Should handle equal dates")
        void testEqualDates() {
            assertFalse(manager.isFilingDateMoreRecent("01/15/2024", "01/15/2024"));
            assertFalse(manager.isFilingDateMoreRecent("12/31/2023", "12/31/2023"));
        }
        
        @Test
        @DisplayName("Should handle null and empty dates conservatively")
        void testNullAndEmptyDateComparison() {
            // No historical date - treat as new firm (process it)
            assertTrue(manager.isFilingDateMoreRecent("01/15/2024", null));
            assertTrue(manager.isFilingDateMoreRecent("01/15/2024", ""));
            assertTrue(manager.isFilingDateMoreRecent("01/15/2024", "   "));
            
            // No current date - don't process (conservative)
            assertFalse(manager.isFilingDateMoreRecent(null, "01/15/2024"));
            assertFalse(manager.isFilingDateMoreRecent("", "01/15/2024"));
            assertFalse(manager.isFilingDateMoreRecent("   ", "01/15/2024"));
            
            // Both null - process conservatively
            assertTrue(manager.isFilingDateMoreRecent(null, null));
        }
        
        @Test
        @DisplayName("Should handle unparseable dates conservatively")
        void testUnparseableDateComparison() {
            // If either date is unparseable, process conservatively
            assertTrue(manager.isFilingDateMoreRecent("invalid", "01/15/2024"));
            assertTrue(manager.isFilingDateMoreRecent("01/15/2024", "invalid"));
            assertTrue(manager.isFilingDateMoreRecent("invalid", "invalid"));
        }
    }
    
    @Nested
    @DisplayName("Historical Data Loading Tests")
    class HistoricalDataLoadingTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("Should load historical filing dates from valid CSV")
        void testLoadHistoricalFilingDates() throws IOException {
            // Create test CSV file
            Path csvFile = tempDir.resolve("test_baseline.csv");
            String csvContent = "FirmCrdNb,Filing Date,Other Column\n" +
                               "12345,01/15/2024,Data1\n" +
                               "67890,02/20/2024,Data2\n" +
                               "11111,03/10/2024,Data3\n";
            Files.write(csvFile, csvContent.getBytes());
            
            Map<String, String> result = manager.getHistoricalFilingDates(csvFile);
            
            assertEquals(3, result.size());
            assertEquals("01/15/2024", result.get("12345"));
            assertEquals("02/20/2024", result.get("67890"));
            assertEquals("03/10/2024", result.get("11111"));
        }
        
        @Test
        @DisplayName("Should handle non-existent baseline file")
        void testLoadFromNonExistentFile() {
            Path nonExistentFile = tempDir.resolve("nonexistent.csv");
            Map<String, String> result = manager.getHistoricalFilingDates(nonExistentFile);
            
            assertTrue(result.isEmpty());
        }
        
        @Test
        @DisplayName("Should handle malformed CSV records gracefully")
        void testLoadMalformedCSV() throws IOException {
            Path csvFile = tempDir.resolve("malformed.csv");
            String csvContent = "FirmCrdNb,Filing Date\n" +
                               "12345,01/15/2024\n" +
                               "invalid line without commas\n" +
                               "67890,02/20/2024\n" +
                               ",03/10/2024\n"; // Empty firm CRD
            Files.write(csvFile, csvContent.getBytes());
            
            Map<String, String> result = manager.getHistoricalFilingDates(csvFile);
            
            assertEquals(2, result.size());
            assertEquals("01/15/2024", result.get("12345"));
            assertEquals("02/20/2024", result.get("67890"));
        }
        
        @Test
        @DisplayName("Should handle empty and whitespace values")
        void testLoadEmptyValues() throws IOException {
            Path csvFile = tempDir.resolve("empty_values.csv");
            String csvContent = "FirmCrdNb,Filing Date\n" +
                               "12345,01/15/2024\n" +
                               "67890,\n" +
                               "  11111  ,  02/20/2024  \n";
            Files.write(csvFile, csvContent.getBytes());
            
            Map<String, String> result = manager.getHistoricalFilingDates(csvFile);
            
            assertEquals(3, result.size());
            assertEquals("01/15/2024", result.get("12345"));
            assertEquals("", result.get("67890"));
            assertEquals("02/20/2024", result.get("11111"));
        }
    }
    
    @Nested
    @DisplayName("Firms to Process Logic Tests")
    class FirmsToProcessTests {
        
        @Test
        @DisplayName("Should identify new firms for processing")
        void testIdentifyNewFirms() {
            List<FirmData> currentFirms = createTestFirmData();
            Map<String, String> historicalDates = new HashMap<>();
            historicalDates.put("12345", "01/15/2024");
            // 67890 is not in historical data (new firm)
            
            Set<String> result = manager.getFirmsToProcess(currentFirms, historicalDates);
            
            assertTrue(result.contains("67890")); // New firm
            assertEquals(1, result.size());
        }
        
        @Test
        @DisplayName("Should identify updated firms for processing")
        void testIdentifyUpdatedFirms() {
            List<FirmData> currentFirms = createTestFirmData();
            Map<String, String> historicalDates = new HashMap<>();
            historicalDates.put("12345", "01/10/2024"); // Older date
            historicalDates.put("67890", "02/25/2024"); // Same date
            
            Set<String> result = manager.getFirmsToProcess(currentFirms, historicalDates);
            
            assertTrue(result.contains("12345")); // Updated firm
            assertFalse(result.contains("67890")); // Unchanged firm
            assertEquals(1, result.size());
        }
        
        @Test
        @DisplayName("Should handle empty historical data")
        void testEmptyHistoricalData() {
            List<FirmData> currentFirms = createTestFirmData();
            Map<String, String> historicalDates = new HashMap<>();
            
            Set<String> result = manager.getFirmsToProcess(currentFirms, historicalDates);
            
            assertEquals(2, result.size()); // All firms are new
            assertTrue(result.contains("12345"));
            assertTrue(result.contains("67890"));
        }
        
        private List<FirmData> createTestFirmData() {
            List<FirmData> firms = new ArrayList<>();
            
            FirmData firm1 = new FirmDataBuilder()
                    .setFirmCrdNb("12345")
                    .setFilingDate("01/15/2024")
                    .setBusNm("Test Firm 1")
                    .build();
            
            FirmData firm2 = new FirmDataBuilder()
                    .setFirmCrdNb("67890")
                    .setFilingDate("02/25/2024")
                    .setBusNm("Test Firm 2")
                    .build();
            
            firms.add(firm1);
            firms.add(firm2);
            return firms;
        }
    }
    
    @Nested
    @DisplayName("Incremental Statistics Tests")
    class IncrementalStatisticsTests {
        
        @Test
        @DisplayName("Should calculate correct incremental statistics")
        void testCalculateIncrementalStats() {
            List<FirmData> currentFirms = createTestFirmDataForStats();
            Map<String, String> historicalDates = new HashMap<>();
            historicalDates.put("12345", "01/10/2024"); // Will be updated
            historicalDates.put("67890", "02/25/2024"); // Unchanged
            historicalDates.put("99999", "12/01/2023"); // Not in current (historical only)
            // 11111 is new (not in historical)
            
            IncrementalUpdateManager.IncrementalStats stats = 
                    manager.calculateIncrementalStats(currentFirms, historicalDates);
            
            assertEquals(3, stats.getTotalCurrentFirms());
            assertEquals(3, stats.getHistoricalFirms());
            assertEquals(1, stats.getNewFirms()); // 11111
            assertEquals(1, stats.getUpdatedFirms()); // 12345
            assertEquals(1, stats.getUnchangedFirms()); // 67890
            assertEquals(2, stats.getToProcess()); // 11111 + 12345
        }
        
        @Test
        @DisplayName("Should handle all new firms scenario")
        void testAllNewFirmsStats() {
            List<FirmData> currentFirms = createTestFirmDataForStats();
            Map<String, String> historicalDates = new HashMap<>();
            
            IncrementalUpdateManager.IncrementalStats stats = 
                    manager.calculateIncrementalStats(currentFirms, historicalDates);
            
            assertEquals(3, stats.getTotalCurrentFirms());
            assertEquals(0, stats.getHistoricalFirms());
            assertEquals(3, stats.getNewFirms());
            assertEquals(0, stats.getUpdatedFirms());
            assertEquals(0, stats.getUnchangedFirms());
            assertEquals(3, stats.getToProcess());
        }
        
        @Test
        @DisplayName("Should handle no changes scenario")
        void testNoChangesStats() {
            List<FirmData> currentFirms = createTestFirmDataForStats();
            Map<String, String> historicalDates = new HashMap<>();
            historicalDates.put("12345", "01/15/2024"); // Same date
            historicalDates.put("67890", "02/25/2024"); // Same date
            historicalDates.put("11111", "03/10/2024"); // Same date
            
            IncrementalUpdateManager.IncrementalStats stats = 
                    manager.calculateIncrementalStats(currentFirms, historicalDates);
            
            assertEquals(3, stats.getTotalCurrentFirms());
            assertEquals(3, stats.getHistoricalFirms());
            assertEquals(0, stats.getNewFirms());
            assertEquals(0, stats.getUpdatedFirms());
            assertEquals(3, stats.getUnchangedFirms());
            assertEquals(0, stats.getToProcess());
        }
        
        private List<FirmData> createTestFirmDataForStats() {
            List<FirmData> firms = new ArrayList<>();
            
            firms.add(new FirmDataBuilder()
                    .setFirmCrdNb("12345")
                    .setFilingDate("01/15/2024")
                    .setBusNm("Test Firm 1")
                    .build());
            
            firms.add(new FirmDataBuilder()
                    .setFirmCrdNb("67890")
                    .setFilingDate("02/25/2024")
                    .setBusNm("Test Firm 2")
                    .build());
            
            firms.add(new FirmDataBuilder()
                    .setFirmCrdNb("11111")
                    .setFilingDate("03/10/2024")
                    .setBusNm("Test Firm 3")
                    .build());
            
            return firms;
        }
    }
    
    @Nested
    @DisplayName("File Filtering Tests")
    class FileFilteringTests {
        
        @Test
        @DisplayName("Should filter firms correctly for processing")
        void testFilterFirmsForProcessing() {
            List<FirmData> currentFirms = createTestFirmDataForStats();
            Map<String, String> historicalDates = new HashMap<>();
            historicalDates.put("12345", "01/10/2024"); // Will be updated
            historicalDates.put("67890", "02/25/2024"); // Unchanged
            
            List<FirmData> result = manager.filterFirmsForProcessing(currentFirms, historicalDates);
            
            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(f -> "12345".equals(f.getFirmCrdNb())));
            assertTrue(result.stream().anyMatch(f -> "11111".equals(f.getFirmCrdNb())));
            assertFalse(result.stream().anyMatch(f -> "67890".equals(f.getFirmCrdNb())));
        }
        
        private List<FirmData> createTestFirmDataForStats() {
            List<FirmData> firms = new ArrayList<>();
            
            firms.add(new FirmDataBuilder()
                    .setFirmCrdNb("12345")
                    .setFilingDate("01/15/2024")
                    .setBusNm("Test Firm 1")
                    .build());
            
            firms.add(new FirmDataBuilder()
                    .setFirmCrdNb("67890")
                    .setFilingDate("02/25/2024")
                    .setBusNm("Test Firm 2")
                    .build());
            
            firms.add(new FirmDataBuilder()
                    .setFirmCrdNb("11111")
                    .setFilingDate("03/10/2024")
                    .setBusNm("Test Firm 3")
                    .build());
            
            return firms;
        }
    }
    
    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {
        
        @Test
        @DisplayName("Should generate correct incremental file names")
        void testGenerateIncrementalFileName() {
            String result1 = manager.generateIncrementalFileName("IA_FIRM_SEC_DATA", "20250107", ".csv");
            String result2 = manager.generateIncrementalFileName("IAPD_Found", "20241225", ".csv");
            
            assertEquals("IA_FIRM_SEC_DATA_20250107_incremental.csv", result1);
            assertEquals("IAPD_Found_20241225_incremental.csv", result2);
        }
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("Should validate baseline file structure correctly")
        void testValidateBaselineFileStructure() throws IOException {
            // Valid baseline file
            Path validFile = tempDir.resolve("valid_baseline.csv");
            String validContent = "FirmCrdNb,Filing Date,Other Column\n" +
                                 "12345,01/15/2024,Data1\n";
            Files.write(validFile, validContent.getBytes());
            
            assertTrue(manager.validateBaselineFileStructure(validFile));
            
            // Invalid baseline file (missing required column)
            Path invalidFile = tempDir.resolve("invalid_baseline.csv");
            String invalidContent = "FirmCrdNb,Other Column\n" +
                                   "12345,Data1\n";
            Files.write(invalidFile, invalidContent.getBytes());
            
            assertFalse(manager.validateBaselineFileStructure(invalidFile));
            
            // Non-existent file
            Path nonExistentFile = tempDir.resolve("nonexistent.csv");
            assertFalse(manager.validateBaselineFileStructure(nonExistentFile));
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("Should perform complete incremental workflow")
        void testCompleteIncrementalWorkflow() throws IOException {
            // Create baseline file
            Path baselineFile = tempDir.resolve("baseline.csv");
            String baselineContent = "FirmCrdNb,Filing Date,Business Name\n" +
                                    "12345,01/10/2024,Old Firm 1\n" +
                                    "67890,02/25/2024,Unchanged Firm\n" +
                                    "99999,12/01/2023,Historical Only Firm\n";
            Files.write(baselineFile, baselineContent.getBytes());
            
            // Create current firm data
            List<FirmData> currentFirms = new ArrayList<>();
            currentFirms.add(new FirmDataBuilder()
                    .setFirmCrdNb("12345")
                    .setFilingDate("01/15/2024") // Updated
                    .setBusNm("Updated Firm 1")
                    .build());
            currentFirms.add(new FirmDataBuilder()
                    .setFirmCrdNb("67890")
                    .setFilingDate("02/25/2024") // Unchanged
                    .setBusNm("Unchanged Firm")
                    .build());
            currentFirms.add(new FirmDataBuilder()
                    .setFirmCrdNb("11111")
                    .setFilingDate("03/10/2024") // New
                    .setBusNm("New Firm")
                    .build());
            
            // Load historical data
            Map<String, String> historicalDates = manager.getHistoricalFilingDates(baselineFile);
            assertEquals(3, historicalDates.size());
            
            // Calculate statistics
            IncrementalUpdateManager.IncrementalStats stats = 
                    manager.calculateIncrementalStats(currentFirms, historicalDates);
            
            assertEquals(3, stats.getTotalCurrentFirms());
            assertEquals(3, stats.getHistoricalFirms());
            assertEquals(1, stats.getNewFirms());
            assertEquals(1, stats.getUpdatedFirms());
            assertEquals(1, stats.getUnchangedFirms());
            assertEquals(2, stats.getToProcess());
            
            // Filter firms for processing
            List<FirmData> toProcess = manager.filterFirmsForProcessing(currentFirms, historicalDates);
            assertEquals(2, toProcess.size());
            
            // Verify correct firms are selected
            Set<String> processedFirmIds = new HashSet<>();
            for (FirmData firm : toProcess) {
                processedFirmIds.add(firm.getFirmCrdNb());
            }
            assertTrue(processedFirmIds.contains("12345")); // Updated
            assertTrue(processedFirmIds.contains("11111")); // New
            assertFalse(processedFirmIds.contains("67890")); // Unchanged
        }
    }
}
