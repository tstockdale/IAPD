package com.iss.iapd.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.iss.iapd.model.FirmData;
import com.iss.iapd.model.FirmDataBuilder;
import com.iss.iapd.services.incremental.BaselineDataReader;
import com.iss.iapd.services.incremental.IncrementalProcessingService;

/**
 * Comprehensive test suite for IncrementalProcessingService
 */
public class IncrementalProcessingServiceTest {

    private IncrementalProcessingService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        service = new IncrementalProcessingService();
    }

    @Test
    @DisplayName("Get firms to process - all new firms")
    public void testGetFirmsToProcessAllNew() {
        List<FirmData> currentFirms = createTestFirms(
            createFirmData("12345", "03/15/2024"),
            createFirmData("67890", "03/20/2024"),
            createFirmData("11111", "03/10/2024")
        );

        Map<String, String> historicalDates = new HashMap<>();

        Set<String> firmsToProcess = service.getFirmsToProcess(currentFirms, historicalDates);

        assertEquals(3, firmsToProcess.size());
        assertTrue(firmsToProcess.contains("12345"));
        assertTrue(firmsToProcess.contains("67890"));
        assertTrue(firmsToProcess.contains("11111"));
    }

    @Test
    @DisplayName("Get firms to process - mix of new and updated")
    public void testGetFirmsToProcessMixed() {
        List<FirmData> currentFirms = createTestFirms(
            createFirmData("12345", "03/20/2024"), // Updated
            createFirmData("67890", "03/15/2024"), // Unchanged
            createFirmData("11111", "03/25/2024")  // New
        );

        Map<String, String> historicalDates = new HashMap<>();
        historicalDates.put("12345", "03/15/2024");
        historicalDates.put("67890", "03/15/2024");

        Set<String> firmsToProcess = service.getFirmsToProcess(currentFirms, historicalDates);

        assertEquals(2, firmsToProcess.size());
        assertTrue(firmsToProcess.contains("12345")); // Updated
        assertTrue(firmsToProcess.contains("11111")); // New
        assertFalse(firmsToProcess.contains("67890")); // Unchanged
    }

    @Test
    @DisplayName("Get firms to process - all unchanged")
    public void testGetFirmsToProcessAllUnchanged() {
        List<FirmData> currentFirms = createTestFirms(
            createFirmData("12345", "03/15/2024"),
            createFirmData("67890", "03/20/2024")
        );

        Map<String, String> historicalDates = new HashMap<>();
        historicalDates.put("12345", "03/15/2024");
        historicalDates.put("67890", "03/20/2024");

        Set<String> firmsToProcess = service.getFirmsToProcess(currentFirms, historicalDates);

        assertEquals(0, firmsToProcess.size());
    }

    @Test
    @DisplayName("Calculate incremental stats - all scenarios")
    public void testCalculateIncrementalStats() {
        List<FirmData> currentFirms = createTestFirms(
            createFirmData("12345", "03/20/2024"), // Updated
            createFirmData("67890", "03/15/2024"), // Unchanged
            createFirmData("11111", "03/25/2024"), // New
            createFirmData("22222", "03/10/2024")  // Unchanged
        );

        Map<String, String> historicalDates = new HashMap<>();
        historicalDates.put("12345", "03/15/2024");
        historicalDates.put("67890", "03/15/2024");
        historicalDates.put("22222", "03/10/2024");
        historicalDates.put("99999", "03/01/2024"); // Removed firm

        IncrementalProcessingService.IncrementalStats stats =
            service.calculateIncrementalStats(currentFirms, historicalDates);

        assertEquals(4, stats.getTotalCurrentFirms());
        assertEquals(4, stats.getHistoricalFirms());
        assertEquals(1, stats.getNewFirms());
        assertEquals(1, stats.getUpdatedFirms());
        assertEquals(2, stats.getUnchangedFirms());
        assertEquals(2, stats.getToProcess());
    }

    @Test
    @DisplayName("Calculate incremental stats - first run (no historical data)")
    public void testCalculateIncrementalStatsFirstRun() {
        List<FirmData> currentFirms = createTestFirms(
            createFirmData("12345", "03/15/2024"),
            createFirmData("67890", "03/20/2024"),
            createFirmData("11111", "03/10/2024")
        );

        Map<String, String> historicalDates = new HashMap<>();

        IncrementalProcessingService.IncrementalStats stats =
            service.calculateIncrementalStats(currentFirms, historicalDates);

        assertEquals(3, stats.getTotalCurrentFirms());
        assertEquals(0, stats.getHistoricalFirms());
        assertEquals(3, stats.getNewFirms());
        assertEquals(0, stats.getUpdatedFirms());
        assertEquals(0, stats.getUnchangedFirms());
        assertEquals(3, stats.getToProcess());
    }

    @Test
    @DisplayName("Filter firms for processing")
    public void testFilterFirmsForProcessing() {
        List<FirmData> currentFirms = createTestFirms(
            createFirmData("12345", "03/20/2024"), // Updated
            createFirmData("67890", "03/15/2024"), // Unchanged
            createFirmData("11111", "03/25/2024")  // New
        );

        Map<String, String> historicalDates = new HashMap<>();
        historicalDates.put("12345", "03/15/2024");
        historicalDates.put("67890", "03/15/2024");

        List<FirmData> filtered = service.filterFirmsForProcessing(currentFirms, historicalDates);

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(f -> f.getFirmCrdNb().equals("12345")));
        assertTrue(filtered.stream().anyMatch(f -> f.getFirmCrdNb().equals("11111")));
        assertFalse(filtered.stream().anyMatch(f -> f.getFirmCrdNb().equals("67890")));
    }

    @Test
    @DisplayName("Should skip brochure version ID")
    public void testShouldSkipBrochureVersionId() {
        Set<String> existingVersionIds = new HashSet<>();
        existingVersionIds.add("BR001");
        existingVersionIds.add("BR002");

        assertTrue(service.shouldSkipBrochureVersionId("BR001", existingVersionIds));
        assertTrue(service.shouldSkipBrochureVersionId("BR002", existingVersionIds));
        assertFalse(service.shouldSkipBrochureVersionId("BR003", existingVersionIds));
        assertFalse(service.shouldSkipBrochureVersionId(null, existingVersionIds));
        assertFalse(service.shouldSkipBrochureVersionId("", existingVersionIds));
        assertFalse(service.shouldSkipBrochureVersionId("   ", existingVersionIds));
    }

    @Test
    @DisplayName("Generate incremental file name")
    public void testGenerateIncrementalFileName() {
        String fileName = service.generateIncrementalFileName("IA_FIRM_SEC_DATA", "20240315", ".csv");
        assertEquals("IA_FIRM_SEC_DATA_20240315_incremental.csv", fileName);
    }

    @Test
    @DisplayName("Validate baseline file structure")
    public void testValidateBaselineFileStructure() throws IOException {
        // Create valid baseline file
        Path validFile = tempDir.resolve("valid_baseline.csv");
        String validContent = "FirmCrdNb,Filing Date,Firm Name\n" +
                             "12345,03/15/2024,Test Firm\n";
        Files.writeString(validFile, validContent);

        assertTrue(service.validateBaselineFileStructure(validFile));

        // Create invalid baseline file (missing required column)
        Path invalidFile = tempDir.resolve("invalid_baseline.csv");
        String invalidContent = "Firm Name,Filing Date\n" +
                               "Test Firm,03/15/2024\n";
        Files.writeString(invalidFile, invalidContent);

        assertFalse(service.validateBaselineFileStructure(invalidFile));
    }

    @Test
    @DisplayName("Read baseline data from file")
    public void testReadBaselineData() throws IOException {
        Path baselineFile = tempDir.resolve("baseline.csv");
        String content = "FirmCrdNb,Filing Date,brochureVersionId\n" +
                        "12345,03/15/2024,BR001\n" +
                        "67890,03/20/2024,BR002\n";
        Files.writeString(baselineFile, content);

        BaselineDataReader.BaselineData data = service.readBaselineData(baselineFile);

        assertNotNull(data);
        assertTrue(data.hasData());
        assertEquals(2, data.getFilingDates().size());
        assertEquals("03/15/2024", data.getFilingDates().get("12345"));
        assertEquals("03/20/2024", data.getFilingDates().get("67890"));
    }

    @Test
    @DisplayName("Read latest baseline data from directory")
    public void testReadLatestBaselineData() throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        // Create multiple IAPD_Data files
        String content1 = "FirmCrdNb,Filing Date,brochureVersionId\n12345,03/10/2024,BR001\n";
        String content2 = "FirmCrdNb,Filing Date,brochureVersionId\n12345,03/20/2024,BR002\n";

        Files.writeString(outputDir.resolve("IAPD_Data_20240310.csv"), content1);
        Files.writeString(outputDir.resolve("IAPD_Data_20240320.csv"), content2);

        BaselineDataReader.BaselineData data = service.readLatestBaselineData(outputDir);

        assertNotNull(data);
        assertTrue(data.hasData());
        assertEquals("IAPD_Data_20240320.csv", data.getSourceFile().getFileName().toString());
        assertEquals("03/20/2024", data.getFilingDates().get("12345"));
    }

    @Test
    @DisplayName("IncrementalStats toString returns formatted string")
    public void testIncrementalStatsToString() {
        IncrementalProcessingService.IncrementalStats stats =
            new IncrementalProcessingService.IncrementalStats(100, 90, 10, 5, 85, 15);

        String toString = stats.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("Total Current: 100"));
        assertTrue(toString.contains("Historical: 90"));
        assertTrue(toString.contains("New: 10"));
        assertTrue(toString.contains("Updated: 5"));
        assertTrue(toString.contains("Unchanged: 85"));
        assertTrue(toString.contains("To Process: 15"));
    }

    @Test
    @DisplayName("Service can be constructed with custom BaselineDataReader")
    public void testConstructorWithCustomReader() {
        BaselineDataReader customReader = new BaselineDataReader();
        IncrementalProcessingService customService = new IncrementalProcessingService(customReader);

        assertNotNull(customService);
        assertEquals(customReader, customService.getBaselineDataReader());
    }

    @Test
    @DisplayName("Get firms to process with empty current firms list")
    public void testGetFirmsToProcessEmptyCurrentFirms() {
        List<FirmData> currentFirms = new ArrayList<>();
        Map<String, String> historicalDates = new HashMap<>();
        historicalDates.put("12345", "03/15/2024");

        Set<String> firmsToProcess = service.getFirmsToProcess(currentFirms, historicalDates);

        assertEquals(0, firmsToProcess.size());
    }

    @Test
    @DisplayName("Calculate stats with firms having invalid dates")
    public void testCalculateStatsWithInvalidDates() {
        List<FirmData> currentFirms = createTestFirms(
            createFirmData("12345", "invalid-date"),
            createFirmData("67890", "03/15/2024")
        );

        Map<String, String> historicalDates = new HashMap<>();
        historicalDates.put("12345", "03/10/2024");
        historicalDates.put("67890", "03/15/2024");

        IncrementalProcessingService.IncrementalStats stats =
            service.calculateIncrementalStats(currentFirms, historicalDates);

        // Firm with invalid current date should be processed (conservative approach)
        assertEquals(1, stats.getUpdatedFirms());
        assertEquals(1, stats.getUnchangedFirms());
    }

    // Helper methods

    private FirmData createFirmData(String firmCrdNb, String filingDate) {
        return new FirmDataBuilder()
                .setFirmCrdNb(firmCrdNb)
                .setFilingDate(filingDate)
                .setBusNm("Test Firm " + firmCrdNb)
                .setSECNb("SEC-" + firmCrdNb)
                .build();
    }

    private List<FirmData> createTestFirms(FirmData... firms) {
        return Arrays.asList(firms);
    }
}
