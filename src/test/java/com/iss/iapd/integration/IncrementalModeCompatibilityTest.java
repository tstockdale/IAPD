package com.iss.iapd.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.iss.iapd.config.Config;
import com.iss.iapd.model.FirmData;
import com.iss.iapd.model.FirmDataBuilder;
import com.iss.iapd.services.incremental.IncrementalProcessingService;
import com.iss.iapd.services.incremental.BaselineDataReader;

/**
 * Integration test to verify incremental mode compatibility with new header structures
 */
public class IncrementalModeCompatibilityTest {
    
    @TempDir
    Path tempDir;
    
    private IncrementalProcessingService incrementalService;
    private BaselineDataReader baselineReader;

    @BeforeEach
    void setUp() {
        incrementalService = new IncrementalProcessingService();
        baselineReader = new BaselineDataReader();
    }
    
    @Test
    void testIncrementalModeWithNewHeaderFormat() throws IOException {
        // Create a baseline file with the new IAPD_DATA_HEADER format
        Path baselineFile = tempDir.resolve("IAPD_Data_20250101.csv");
        createBaselineFileWithNewFormat(baselineFile);
        
        // Verify the baseline file can be read correctly
        BaselineDataReader.BaselineData baselineData = baselineReader.readBaselineData(baselineFile);
        Map<String, String> historicalDates = baselineData.getFilingDates();
        
        assertNotNull(historicalDates);
        assertEquals(3, historicalDates.size());
        assertTrue(historicalDates.containsKey("12345"));
        assertTrue(historicalDates.containsKey("67890"));
        assertTrue(historicalDates.containsKey("11111"));
        assertEquals("01/15/2025", historicalDates.get("12345"));
        assertEquals("01/10/2025", historicalDates.get("67890"));
        assertEquals("01/20/2025", historicalDates.get("11111"));
    }
    
    @Test
    void testOutputDataReaderWithNewFormat() throws IOException {
        // Create an output file with the new format
        Path outputFile = tempDir.resolve("IAPD_Data_20250115.csv");
        createOutputFileWithNewFormat(outputFile);
        
        // Test BaselineDataReader functionality
        BaselineDataReader.BaselineData analysis = baselineReader.readLatestBaselineData(tempDir);
        
        assertNotNull(analysis);
        assertTrue(analysis.hasData());
        assertEquals("IAPD_Data_20250115.csv", analysis.getSourceFile().getFileName().toString());
        assertEquals("01/20/2025", analysis.getMaxDateSubmitted());
        assertEquals(3, analysis.getTotalRecords());

        // Test brochureVersionId extraction
        Set<String> brochureVersionIds = analysis.getBrochureVersionIds();
        assertNotNull(brochureVersionIds);
        assertEquals(3, brochureVersionIds.size());
        assertTrue(brochureVersionIds.contains("123456"));
        assertTrue(brochureVersionIds.contains("789012"));
        assertTrue(brochureVersionIds.contains("345678"));
    }
    
    @Test
    void testIncrementalProcessingLogic() throws IOException {
        // Create baseline file
        Path baselineFile = tempDir.resolve("IAPD_Data_baseline.csv");
        createBaselineFileWithNewFormat(baselineFile);
        
        // Create current firm data with some updates
        List<FirmData> currentFirms = createCurrentFirmData();
        
        // Get historical dates
        BaselineDataReader.BaselineData baselineData = baselineReader.readBaselineData(baselineFile);
        Map<String, String> historicalDates = baselineData.getFilingDates();

        // Test incremental logic
        Set<String> firmsToProcess = incrementalService.getFirmsToProcess(currentFirms, historicalDates);
        
        // Should process firms with newer filing dates or new firms
        assertNotNull(firmsToProcess);
        assertTrue(firmsToProcess.contains("12345")); // Updated filing date
        assertTrue(firmsToProcess.contains("99999")); // New firm
        assertFalse(firmsToProcess.contains("67890")); // Same filing date
        
        // Test statistics calculation
        IncrementalProcessingService.IncrementalStats stats = incrementalService.calculateIncrementalStats(currentFirms, historicalDates);
        assertNotNull(stats);
        assertEquals(4, stats.getTotalCurrentFirms());
        assertEquals(3, stats.getHistoricalFirms());
        assertEquals(1, stats.getNewFirms()); // Firm 99999
        assertEquals(1, stats.getUpdatedFirms()); // Firm 12345
        assertEquals(2, stats.getUnchangedFirms()); // Firms 67890 and 11111
        assertEquals(2, stats.getToProcess()); // New + Updated
    }
    
    @Test
    void testHeaderValidation() throws IOException {
        // Test with new format file
        Path newFormatFile = tempDir.resolve("IAPD_Data_new.csv");
        createOutputFileWithNewFormat(newFormatFile);

        assertTrue(baselineReader.validateFileStructure(newFormatFile));
        assertTrue(incrementalService.validateBaselineFileStructure(newFormatFile));

        // Test with old format file (missing some new fields)
        Path oldFormatFile = tempDir.resolve("IAPD_Data_old.csv");
        createOldFormatFile(oldFormatFile);

        assertTrue(incrementalService.validateBaselineFileStructure(oldFormatFile));
        // BaselineDataReader should still work with old format (falls back to Filing Date)
        assertTrue(baselineReader.validateFileStructure(oldFormatFile));
    }
    
    private void createBaselineFileWithNewFormat(Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            // Write the new IAPD_DATA_HEADER
            writer.write(Config.IAPD_DATA_HEADER);
            writer.newLine();
            
            // Write sample data with all fields
            writer.write("08/18/2025,IA,12345,Y,\"Test Firm 1\",\"Test Legal Name 1\",\"123 Main St\",\"\",\"New York\",\"NY\",\"USA\",\"10001\",\"555-1234\",\"555-5678\",\"Investment Adviser\",\"NY\",\"01/01/2020\",\"01/15/2025\",\"1\",\"50\",\"1000000\",\"100\",\"https://example.com/brochure1\",\"123456\",\"Brochure 1\",\"01/15/2025\",\"01/16/2025\",\"test1.pdf\",\"Proxy Provider 1\",\"Class Action 1\",\"ESG Provider 1\",\"ESG Language 1\",\"compliance@test1.com\",\"proxy@test1.com\",\"brochure@test1.com\",\"item17@test1.com\",\"all@test1.com\",\"Does not vote\"");
            writer.newLine();
            
            writer.write("08/18/2025,IA,67890,Y,\"Test Firm 2\",\"Test Legal Name 2\",\"456 Oak Ave\",\"\",\"Boston\",\"MA\",\"USA\",\"02101\",\"555-2345\",\"555-6789\",\"Investment Adviser\",\"MA\",\"01/01/2021\",\"01/10/2025\",\"1\",\"75\",\"2000000\",\"200\",\"https://example.com/brochure2\",\"789012\",\"Brochure 2\",\"01/10/2025\",\"01/11/2025\",\"test2.pdf\",\"Proxy Provider 2\",\"Class Action 2\",\"ESG Provider 2\",\"ESG Language 2\",\"compliance@test2.com\",\"proxy@test2.com\",\"brochure@test2.com\",\"item17@test2.com\",\"all@test2.com\",\"Does not vote\"");
            writer.newLine();
            
            writer.write("08/18/2025,IA,11111,Y,\"Test Firm 3\",\"Test Legal Name 3\",\"789 Pine St\",\"\",\"Chicago\",\"IL\",\"USA\",\"60601\",\"555-3456\",\"555-7890\",\"Investment Adviser\",\"IL\",\"01/01/2022\",\"01/20/2025\",\"1\",\"25\",\"500000\",\"50\",\"https://example.com/brochure3\",\"345678\",\"Brochure 3\",\"01/20/2025\",\"01/21/2025\",\"test3.pdf\",\"Proxy Provider 3\",\"Class Action 3\",\"ESG Provider 3\",\"ESG Language 3\",\"compliance@test3.com\",\"proxy@test3.com\",\"brochure@test3.com\",\"item17@test3.com\",\"all@test3.com\",\"Does not vote\"");
            writer.newLine();
        }
    }
    
    private void createOutputFileWithNewFormat(Path file) throws IOException {
        createBaselineFileWithNewFormat(file); // Same format for this test
    }
    
    private void createOldFormatFile(Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            // Write old format header (just the basic firm fields)
            writer.write(Config.FIRM_HEADER);
            writer.newLine();
            
            // Write sample data with basic fields only
            writer.write("08/18/2025,IA,12345,Y,\"Test Firm 1\",\"Test Legal Name 1\",\"123 Main St\",\"\",\"New York\",\"NY\",\"USA\",\"10001\",\"555-1234\",\"555-5678\",\"Investment Adviser\",\"NY\",\"01/01/2020\",\"01/15/2025\",\"1\",\"50\",\"1000000\",\"100\",\"https://example.com/brochure1\"");
            writer.newLine();
            
            writer.write("08/18/2025,IA,67890,Y,\"Test Firm 2\",\"Test Legal Name 2\",\"456 Oak Ave\",\"\",\"Boston\",\"MA\",\"USA\",\"02101\",\"555-2345\",\"555-6789\",\"Investment Adviser\",\"MA\",\"01/01/2021\",\"01/10/2025\",\"1\",\"75\",\"2000000\",\"200\",\"https://example.com/brochure2\"");
            writer.newLine();
        }
    }
    
    private List<FirmData> createCurrentFirmData() {
        return List.of(
            new FirmDataBuilder()
                .setFirmCrdNb("12345")
                .setFilingDate("01/25/2025") // Updated date
                .setBusNm("Test Firm 1")
                .build(),
            new FirmDataBuilder()
                .setFirmCrdNb("67890")
                .setFilingDate("01/10/2025") // Same date
                .setBusNm("Test Firm 2")
                .build(),
            new FirmDataBuilder()
                .setFirmCrdNb("11111")
                .setFilingDate("01/20/2025") // Same date
                .setBusNm("Test Firm 3")
                .build(),
            new FirmDataBuilder()
                .setFirmCrdNb("99999")
                .setFilingDate("01/30/2025") // New firm
                .setBusNm("New Test Firm")
                .build()
        );
    }
}
