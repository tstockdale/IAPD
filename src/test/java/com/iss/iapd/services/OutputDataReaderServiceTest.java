package com.iss.iapd.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.iss.iapd.services.incremental.OutputDataReaderService;
import com.iss.iapd.config.Config;

/**
 * Unit tests for OutputDataReaderService
 */
public class OutputDataReaderServiceTest {
    
    private OutputDataReaderService service;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        service = new OutputDataReaderService();
    }
    
    @Test
    void testAnalyzeOutputDirectory_NoDirectory() {
        Path nonExistentDir = Paths.get("non-existent-directory");
        
        OutputDataReaderService.OutputDataAnalysis analysis = service.analyzeOutputDirectory(nonExistentDir);
        
        assertFalse(analysis.hasExistingData());
        assertNull(analysis.getLatestFile());
        assertNull(analysis.getMaxDateSubmitted());
        assertEquals(0, analysis.getTotalRecords());
    }
    
    @Test
    void testAnalyzeOutputDirectory_EmptyDirectory() throws IOException {
        OutputDataReaderService.OutputDataAnalysis analysis = service.analyzeOutputDirectory(tempDir);
        
        assertFalse(analysis.hasExistingData());
        assertNull(analysis.getLatestFile());
        assertNull(analysis.getMaxDateSubmitted());
        assertEquals(0, analysis.getTotalRecords());
    }
    
    @Test
    void testAnalyzeOutputDirectory_WithValidFile() throws IOException {
        // Create a test output file using the Config constant
        String testFileName = Config.OUTPUT_FILE_BASE_NAME + "_20250815.csv";
        Path testFile = tempDir.resolve(testFileName);
        String csvContent = "Date,FirmCrdNb,Business Name,dateSubmitted,Filing Date\n" +
                           "08/15/2025,12345,Test Firm 1,08/10/2025,08/10/2025\n" +
                           "08/15/2025,67890,Test Firm 2,08/12/2025,08/12/2025\n" +
                           "08/15/2025,11111,Test Firm 3,08/08/2025,08/08/2025\n";
        Files.write(testFile, csvContent.getBytes());
        
        OutputDataReaderService.OutputDataAnalysis analysis = service.analyzeOutputDirectory(tempDir);
        
        assertTrue(analysis.hasExistingData());
        assertNotNull(analysis.getLatestFile());
        assertEquals(testFileName, analysis.getLatestFile().getFileName().toString());
        assertEquals("08/12/2025", analysis.getMaxDateSubmitted()); // Should find the maximum date
        assertEquals(3, analysis.getTotalRecords());
    }
    
    @Test
    void testParseDate_ValidDate() {
        Date result = service.parseDate("08/15/2025");
        assertNotNull(result);
    }
    
    @Test
    void testParseDate_InvalidDate() {
        Date result = service.parseDate("invalid-date");
        assertNull(result);
    }
    
    @Test
    void testParseDate_NullDate() {
        Date result = service.parseDate(null);
        assertNull(result);
    }
    
    @Test
    void testParseDate_EmptyDate() {
        Date result = service.parseDate("");
        assertNull(result);
    }
    
    @Test
    void testIsDateMoreRecent_NewerDate() {
        boolean result = service.isDateMoreRecent("08/15/2025", "08/10/2025");
        assertTrue(result);
    }
    
    @Test
    void testIsDateMoreRecent_OlderDate() {
        boolean result = service.isDateMoreRecent("08/10/2025", "08/15/2025");
        assertFalse(result);
    }
    
    @Test
    void testIsDateMoreRecent_SameDate() {
        boolean result = service.isDateMoreRecent("08/15/2025", "08/15/2025");
        assertFalse(result);
    }
    
    @Test
    void testIsDateMoreRecent_NoMaxDate() {
        boolean result = service.isDateMoreRecent("08/15/2025", null);
        assertTrue(result); // Should return true when no max date (first run)
    }
    
    @Test
    void testIsDateMoreRecent_NoCurrentDate() {
        boolean result = service.isDateMoreRecent(null, "08/15/2025");
        assertFalse(result); // Should return false when no current date
    }
    
    @Test
    void testIsDateMoreRecent_BothNull() {
        boolean result = service.isDateMoreRecent(null, null);
        assertTrue(result); // Conservative approach - include if both null
    }
    
    @Test
    void testFindLatestOutputFile_MultipleFiles() throws IOException {
        // Create multiple output files with different dates using Config constant
        String file1 = Config.OUTPUT_FILE_BASE_NAME + "_20250810.csv";
        String file2 = Config.OUTPUT_FILE_BASE_NAME + "_20250815.csv";
        String file3 = Config.OUTPUT_FILE_BASE_NAME + "_20250812.csv";
        
        Files.write(tempDir.resolve(file1), "test content".getBytes());
        Files.write(tempDir.resolve(file2), "test content".getBytes());
        Files.write(tempDir.resolve(file3), "test content".getBytes());
        Files.write(tempDir.resolve("other_file.csv"), "test content".getBytes()); // Should be ignored
        
        var latestFile = service.findLatestOutputFile(tempDir);
        
        assertTrue(latestFile.isPresent());
        assertEquals(file2, latestFile.get().getFileName().toString()); // Should be the 20250815 file
    }
    
    @Test
    void testValidateOutputFileStructure_ValidFile() throws IOException {
        Path testFile = tempDir.resolve("test.csv");
        String csvContent = "Date,FirmCrdNb,dateSubmitted\n" +
                           "08/15/2025,12345,08/10/2025\n";
        Files.write(testFile, csvContent.getBytes());
        
        boolean result = service.validateOutputFileStructure(testFile);
        assertTrue(result);
    }
    
    @Test
    void testValidateOutputFileStructure_MissingDateColumn() throws IOException {
        Path testFile = tempDir.resolve("test.csv");
        String csvContent = "Date,FirmCrdNb,BusinessName\n" +
                           "08/15/2025,12345,Test Firm\n";
        Files.write(testFile, csvContent.getBytes());
        
        boolean result = service.validateOutputFileStructure(testFile);
        assertFalse(result);
    }
    
    @Test
    void testValidateOutputFileStructure_NonExistentFile() {
        Path nonExistentFile = tempDir.resolve("non-existent.csv");
        
        boolean result = service.validateOutputFileStructure(nonExistentFile);
        assertFalse(result);
    }
}
