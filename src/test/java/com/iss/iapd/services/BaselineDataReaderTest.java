package com.iss.iapd.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.iss.iapd.services.incremental.BaselineDataReader;

/**
 * Test suite for BaselineDataReader
 */
public class BaselineDataReaderTest {

    private BaselineDataReader reader;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        reader = new BaselineDataReader();
    }

    @AfterEach
    public void tearDown() {
        reader = null;
    }

    @Test
    @DisplayName("Read non-existent baseline file returns empty data")
    public void testReadNonExistentFile() {
        Path nonExistentFile = tempDir.resolve("non_existent.csv");
        BaselineDataReader.BaselineData data = reader.readBaselineData(nonExistentFile);

        assertNotNull(data);
        assertFalse(data.hasData());
        assertNull(data.getSourceFile());
        assertTrue(data.getFilingDates().isEmpty());
        assertTrue(data.getBrochureVersionIds().isEmpty());
        assertNull(data.getMaxDateSubmitted());
        assertEquals(0, data.getTotalRecords());
    }

    @Test
    @DisplayName("Read valid baseline file extracts data correctly")
    public void testReadValidBaselineFile() throws IOException {
        // Create a test CSV file
        Path testFile = tempDir.resolve("test_baseline.csv");
        String csvContent = "FirmCrdNb,Filing Date,brochureVersionId,Firm Name\n" +
                           "12345,03/15/2024,BR001,Test Firm 1\n" +
                           "67890,03/20/2024,BR002,Test Firm 2\n" +
                           "11111,03/10/2024,BR003,Test Firm 3\n";
        Files.writeString(testFile, csvContent);

        BaselineDataReader.BaselineData data = reader.readBaselineData(testFile);

        assertNotNull(data);
        assertTrue(data.hasData());
        assertEquals(testFile, data.getSourceFile());
        assertEquals(3, data.getFilingDates().size());
        assertEquals("03/15/2024", data.getFilingDates().get("12345"));
        assertEquals("03/20/2024", data.getFilingDates().get("67890"));
        assertEquals("03/10/2024", data.getFilingDates().get("11111"));
        assertEquals(3, data.getBrochureVersionIds().size());
        assertTrue(data.getBrochureVersionIds().contains("BR001"));
        assertTrue(data.getBrochureVersionIds().contains("BR002"));
        assertTrue(data.getBrochureVersionIds().contains("BR003"));
        assertEquals("03/20/2024", data.getMaxDateSubmitted());
    }

    @Test
    @DisplayName("Read baseline file with missing columns")
    public void testReadFileWithMissingColumns() throws IOException {
        Path testFile = tempDir.resolve("test_missing_columns.csv");
        String csvContent = "FirmCrdNb,Firm Name\n" +
                           "12345,Test Firm 1\n" +
                           "67890,Test Firm 2\n";
        Files.writeString(testFile, csvContent);

        BaselineDataReader.BaselineData data = reader.readBaselineData(testFile);

        // Should still read but with warnings
        assertNotNull(data);
        assertFalse(data.hasData()); // Invalid structure
    }

    @Test
    @DisplayName("Validate file structure with valid file")
    public void testValidateValidFileStructure() throws IOException {
        Path testFile = tempDir.resolve("test_valid.csv");
        String csvContent = "FirmCrdNb,Filing Date,brochureVersionId\n" +
                           "12345,03/15/2024,BR001\n";
        Files.writeString(testFile, csvContent);

        assertTrue(reader.validateFileStructure(testFile));
    }

    @Test
    @DisplayName("Validate file structure with missing FirmCrdNb column")
    public void testValidateMissingFirmCrdNb() throws IOException {
        Path testFile = tempDir.resolve("test_missing_firmcrdnb.csv");
        String csvContent = "Filing Date,brochureVersionId\n" +
                           "03/15/2024,BR001\n";
        Files.writeString(testFile, csvContent);

        assertFalse(reader.validateFileStructure(testFile));
    }

    @Test
    @DisplayName("Validate file structure with missing date column")
    public void testValidateMissingDateColumn() throws IOException {
        Path testFile = tempDir.resolve("test_missing_date.csv");
        String csvContent = "FirmCrdNb,brochureVersionId\n" +
                           "12345,BR001\n";
        Files.writeString(testFile, csvContent);

        assertFalse(reader.validateFileStructure(testFile));
    }

    @Test
    @DisplayName("Validate non-existent file returns false")
    public void testValidateNonExistentFile() {
        Path nonExistentFile = tempDir.resolve("non_existent.csv");
        assertFalse(reader.validateFileStructure(nonExistentFile));
    }

    @Test
    @DisplayName("Find latest output file in directory")
    public void testFindLatestOutputFile() throws IOException {
        // Create multiple output files with dates
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        Files.writeString(outputDir.resolve("IAPD_Data_20240315.csv"), "header\ndata1");
        Files.writeString(outputDir.resolve("IAPD_Data_20240320.csv"), "header\ndata2");
        Files.writeString(outputDir.resolve("IAPD_Data_20240310.csv"), "header\ndata3");
        Files.writeString(outputDir.resolve("other_file.csv"), "header\ndata4");

        var latestFile = reader.findLatestOutputFile(outputDir);

        assertTrue(latestFile.isPresent());
        assertEquals("IAPD_Data_20240320.csv", latestFile.get().getFileName().toString());
    }

    @Test
    @DisplayName("Find latest output file in empty directory")
    public void testFindLatestOutputFileEmptyDirectory() throws IOException {
        Path outputDir = tempDir.resolve("empty_output");
        Files.createDirectories(outputDir);

        var latestFile = reader.findLatestOutputFile(outputDir);

        assertFalse(latestFile.isPresent());
    }

    @Test
    @DisplayName("Find latest output file in non-existent directory")
    public void testFindLatestOutputFileNonExistentDirectory() {
        Path nonExistentDir = tempDir.resolve("non_existent_dir");

        var latestFile = reader.findLatestOutputFile(nonExistentDir);

        assertFalse(latestFile.isPresent());
    }

    @Test
    @DisplayName("Read latest baseline data from directory")
    public void testReadLatestBaselineData() throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        // Create older file
        String olderContent = "FirmCrdNb,Filing Date,brochureVersionId\n" +
                             "12345,03/10/2024,BR001\n";
        Files.writeString(outputDir.resolve("IAPD_Data_20240310.csv"), olderContent);

        // Create newer file
        String newerContent = "FirmCrdNb,Filing Date,brochureVersionId\n" +
                             "12345,03/20/2024,BR002\n" +
                             "67890,03/15/2024,BR003\n";
        Files.writeString(outputDir.resolve("IAPD_Data_20240320.csv"), newerContent);

        BaselineDataReader.BaselineData data = reader.readLatestBaselineData(outputDir);

        assertNotNull(data);
        assertTrue(data.hasData());
        assertEquals("IAPD_Data_20240320.csv", data.getSourceFile().getFileName().toString());
        assertEquals(2, data.getFilingDates().size());
        assertEquals("03/20/2024", data.getFilingDates().get("12345"));
    }

    @Test
    @DisplayName("Generate incremental file name")
    public void testGenerateIncrementalFileName() {
        String fileName = reader.generateIncrementalFileName("IA_FIRM_SEC_DATA", "20240315", ".csv");
        assertEquals("IA_FIRM_SEC_DATA_20240315_incremental.csv", fileName);
    }

    @Test
    @DisplayName("Read baseline file with malformed records")
    public void testReadFileWithMalformedRecords() throws IOException {
        Path testFile = tempDir.resolve("test_malformed.csv");
        String csvContent = "FirmCrdNb,Filing Date,brochureVersionId\n" +
                           "12345,03/15/2024,BR001\n" +
                           "invalid row with missing columns\n" +
                           "67890,03/20/2024,BR002\n";
        Files.writeString(testFile, csvContent);

        BaselineDataReader.BaselineData data = reader.readBaselineData(testFile);

        // Should read valid records and skip malformed ones
        // Note: The malformed row "invalid row with missing columns" will be read as a single field
        // which becomes a FirmCrdNb, so it actually gets counted (3 firms total including malformed)
        assertNotNull(data);
        assertTrue(data.hasData());
        assertEquals(3, data.getFilingDates().size());
    }

    @Test
    @DisplayName("Read baseline file with empty fields")
    public void testReadFileWithEmptyFields() throws IOException {
        Path testFile = tempDir.resolve("test_empty_fields.csv");
        String csvContent = "FirmCrdNb,Filing Date,brochureVersionId\n" +
                           "12345,03/15/2024,BR001\n" +
                           ",03/20/2024,BR002\n" +  // Empty FirmCrdNb
                           "67890,,BR003\n" +        // Empty filing date
                           "11111,03/25/2024,\n";   // Empty brochure version ID
        Files.writeString(testFile, csvContent);

        BaselineDataReader.BaselineData data = reader.readBaselineData(testFile);

        assertNotNull(data);
        assertTrue(data.hasData());
        // Should include all records with non-empty FirmCrdNb (12345, 67890, 11111) = 3 records
        assertEquals(3, data.getFilingDates().size());
        assertTrue(data.getFilingDates().containsKey("12345"));
        assertTrue(data.getFilingDates().containsKey("67890"));
        assertTrue(data.getFilingDates().containsKey("11111"));
        // Should include all non-empty brochure version IDs (BR001, BR002, BR003) = 3 IDs
        // BR002 comes from the row with empty FirmCrdNb, which is still processed for brochureVersionId
        assertEquals(3, data.getBrochureVersionIds().size());
    }

    @Test
    @DisplayName("BaselineData toString returns formatted string")
    public void testBaselineDataToString() throws IOException {
        Path testFile = tempDir.resolve("test.csv");
        String csvContent = "FirmCrdNb,Filing Date,brochureVersionId\n" +
                           "12345,03/15/2024,BR001\n";
        Files.writeString(testFile, csvContent);

        BaselineDataReader.BaselineData data = reader.readBaselineData(testFile);
        String toString = data.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("test.csv"));
        assertTrue(toString.contains("filingDates=1"));
        assertTrue(toString.contains("brochureVersionIds=1"));
        assertTrue(toString.contains("hasData=true"));
    }

    @Test
    @DisplayName("Read baseline with alternative column names")
    public void testReadBaselineWithAlternativeColumnNames() throws IOException {
        Path testFile = tempDir.resolve("test_alt_columns.csv");
        String csvContent = "FirmCrdNb,Filing_Date,BrochureVersionId\n" +
                           "12345,03/15/2024,BR001\n" +
                           "67890,03/20/2024,BR002\n";
        Files.writeString(testFile, csvContent);

        BaselineDataReader.BaselineData data = reader.readBaselineData(testFile);

        assertNotNull(data);
        assertTrue(data.hasData());
        assertEquals(2, data.getFilingDates().size());
        assertEquals(2, data.getBrochureVersionIds().size());
    }
}
