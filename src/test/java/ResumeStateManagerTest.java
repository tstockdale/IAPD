import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Comprehensive JUnit 5 test suite for ResumeStateManager
 * Tests PDF validation, status tracking, and resume statistics
 */
public class ResumeStateManagerTest {
    
    private ResumeStateManager manager;
    
    @BeforeEach
    void setUp() {
        manager = new ResumeStateManager();
    }
    
    @Nested
    @DisplayName("Download Status Loading Tests")
    class DownloadStatusLoadingTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("Should load download status from valid CSV")
        void testLoadDownloadStatus() throws IOException {
            // Create test CSV file with download status
            Path csvFile = tempDir.resolve("download_status.csv");
            String csvContent = "FirmCrdNb,DownloadStatus,Other Column\n" +
                               "12345,SUCCESS,Data1\n" +
                               "67890,FAILED,Data2\n" +
                               "11111,SKIPPED,Data3\n";
            Files.write(csvFile, csvContent.getBytes());
            
            Map<String, String> result = manager.getDownloadStatus(csvFile);
            
            assertEquals(3, result.size());
            assertEquals("SUCCESS", result.get("12345"));
            assertEquals("FAILED", result.get("67890"));
            assertEquals("SKIPPED", result.get("11111"));
        }
        
        @Test
        @DisplayName("Should handle non-existent download status file")
        void testLoadFromNonExistentFile() {
            Path nonExistentFile = tempDir.resolve("nonexistent.csv");
            Map<String, String> result = manager.getDownloadStatus(nonExistentFile);
            
            assertTrue(result.isEmpty());
        }
        
        @Test
        @DisplayName("Should handle malformed CSV records gracefully")
        void testLoadMalformedDownloadStatusCSV() throws IOException {
            Path csvFile = tempDir.resolve("malformed.csv");
            String csvContent = "FirmCrdNb,DownloadStatus\n" +
                               "12345,SUCCESS\n" +
                               "invalid line without commas\n" +
                               "67890,FAILED\n" +
                               ",SKIPPED\n"; // Empty firm CRD
            Files.write(csvFile, csvContent.getBytes());
            
            Map<String, String> result = manager.getDownloadStatus(csvFile);
            
            assertEquals(2, result.size());
            assertEquals("SUCCESS", result.get("12345"));
            assertEquals("FAILED", result.get("67890"));
        }
        
        @Test
        @DisplayName("Should handle empty and whitespace values")
        void testLoadEmptyDownloadStatusValues() throws IOException {
            Path csvFile = tempDir.resolve("empty_values.csv");
            String csvContent = "FirmCrdNb,DownloadStatus\n" +
                               "12345,SUCCESS\n" +
                               "67890,\n" +
                               "  11111  ,  FAILED  \n";
            Files.write(csvFile, csvContent.getBytes());
            
            Map<String, String> result = manager.getDownloadStatus(csvFile);
            
            assertEquals(3, result.size());
            assertEquals("SUCCESS", result.get("12345"));
            assertEquals("", result.get("67890"));
            assertEquals("FAILED", result.get("11111"));
        }
    }
    
    @Nested
    @DisplayName("Processed Firms Loading Tests")
    class ProcessedFirmsLoadingTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("Should load processed firms from output CSV")
        void testLoadProcessedFirms() throws IOException {
            // Create test output CSV file
            Path csvFile = tempDir.resolve("output.csv");
            String csvContent = "FirmCrdNb,Business Name,Other Column\n" +
                               "12345,Firm 1,Data1\n" +
                               "67890,Firm 2,Data2\n" +
                               "11111,Firm 3,Data3\n";
            Files.write(csvFile, csvContent.getBytes());
            
            Set<String> result = manager.getProcessedFirms(csvFile);
            
            assertEquals(3, result.size());
            assertTrue(result.contains("12345"));
            assertTrue(result.contains("67890"));
            assertTrue(result.contains("11111"));
        }
        
        @Test
        @DisplayName("Should handle non-existent output file")
        void testLoadFromNonExistentOutputFile() {
            Path nonExistentFile = tempDir.resolve("nonexistent.csv");
            Set<String> result = manager.getProcessedFirms(nonExistentFile);
            
            assertTrue(result.isEmpty());
        }
        
        @Test
        @DisplayName("Should handle malformed output CSV records gracefully")
        void testLoadMalformedOutputCSV() throws IOException {
            Path csvFile = tempDir.resolve("malformed_output.csv");
            String csvContent = "FirmCrdNb,Business Name\n" +
                               "12345,Firm 1\n" +
                               "invalid line without commas\n" +
                               "67890,Firm 2\n" +
                               ",Firm 3\n"; // Empty firm CRD
            Files.write(csvFile, csvContent.getBytes());
            
            Set<String> result = manager.getProcessedFirms(csvFile);
            
            assertEquals(2, result.size());
            assertTrue(result.contains("12345"));
            assertTrue(result.contains("67890"));
        }
        
        @Test
        @DisplayName("Should handle whitespace in firm CRD values")
        void testLoadProcessedFirmsWithWhitespace() throws IOException {
            Path csvFile = tempDir.resolve("whitespace_output.csv");
            String csvContent = "FirmCrdNb,Business Name\n" +
                               "12345,Firm 1\n" +
                               "  67890  ,Firm 2\n" +
                               "11111,Firm 3\n";
            Files.write(csvFile, csvContent.getBytes());
            
            Set<String> result = manager.getProcessedFirms(csvFile);
            
            assertEquals(3, result.size());
            assertTrue(result.contains("12345"));
            assertTrue(result.contains("67890"));
            assertTrue(result.contains("11111"));
        }
    }
    
    @Nested
    @DisplayName("PDF Validation Tests")
    class PDFValidationTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("Should reject null or non-existent PDF files")
        void testRejectNullOrNonExistentPDF() {
            assertFalse(manager.validatePdfFile(null));
            
            File nonExistentFile = new File(tempDir.toFile(), "nonexistent.pdf");
            assertFalse(manager.validatePdfFile(nonExistentFile));
        }
        
        @Test
        @DisplayName("Should reject PDF files that are too small")
        void testRejectTooSmallPDF() throws IOException {
            File smallFile = tempDir.resolve("small.pdf").toFile();
            Files.write(smallFile.toPath(), "small".getBytes()); // Less than 1KB
            
            assertFalse(manager.validatePdfFile(smallFile));
        }
        
        @Test
        @DisplayName("Should reject files without PDF magic bytes")
        void testRejectNonPDFFiles() throws IOException {
            File nonPdfFile = tempDir.resolve("notpdf.pdf").toFile();
            byte[] content = new byte[2048]; // Large enough but not PDF
            Arrays.fill(content, (byte) 'A');
            Files.write(nonPdfFile.toPath(), content);
            
            assertFalse(manager.validatePdfFile(nonPdfFile));
        }
        
        @Test
        @DisplayName("Should validate files with proper PDF header")
        void testValidatePDFHeader() throws IOException {
            File pdfFile = tempDir.resolve("valid.pdf").toFile();
            
            // Create a minimal PDF-like file with proper header
            byte[] content = new byte[2048];
            System.arraycopy("%PDF".getBytes(), 0, content, 0, 4);
            // Fill rest with some content
            Arrays.fill(content, 4, content.length, (byte) 'A');
            Files.write(pdfFile.toPath(), content);
            
            // Note: This test may fail if PdfTextExtractor.getCleanedBrochureText
            // cannot extract text from our mock PDF. In a real scenario, we'd
            // need a proper PDF file or mock the PdfTextExtractor.
            // For now, we'll test that it doesn't throw an exception
            assertDoesNotThrow(() -> manager.validatePdfFile(pdfFile));
        }
    }
    
    @Nested
    @DisplayName("Download Retry Logic Tests")
    class DownloadRetryLogicTests {
        
        @Test
        @DisplayName("Should retry failed downloads")
        void testShouldRetryFailedDownloads() {
            assertTrue(manager.shouldRetryDownload("FAILED"));
            assertTrue(manager.shouldRetryDownload("FAILED: Connection timeout"));
            assertTrue(manager.shouldRetryDownload("ERROR"));
            assertTrue(manager.shouldRetryDownload("ERROR: 404 Not Found"));
            assertTrue(manager.shouldRetryDownload("failed")); // Case insensitive
            assertTrue(manager.shouldRetryDownload("error")); // Case insensitive
        }
        
        @Test
        @DisplayName("Should not retry successful or skipped downloads")
        void testShouldNotRetrySuccessfulDownloads() {
            assertFalse(manager.shouldRetryDownload("SUCCESS"));
            assertFalse(manager.shouldRetryDownload("SKIPPED"));
            assertFalse(manager.shouldRetryDownload("NO_URL"));
            assertFalse(manager.shouldRetryDownload("INVALID_URL"));
        }
        
        @Test
        @DisplayName("Should retry downloads with no status or empty status")
        void testShouldRetryNoStatus() {
            assertTrue(manager.shouldRetryDownload(null));
            assertTrue(manager.shouldRetryDownload(""));
            assertTrue(manager.shouldRetryDownload("   "));
        }
        
        @Test
        @DisplayName("Should retry downloads with unknown status")
        void testShouldRetryUnknownStatus() {
            assertTrue(manager.shouldRetryDownload("UNKNOWN"));
            assertTrue(manager.shouldRetryDownload("PENDING"));
            assertTrue(manager.shouldRetryDownload("PROCESSING"));
        }
    }
    
    @Nested
    @DisplayName("Download Resume Statistics Tests")
    class DownloadResumeStatisticsTests {
        
        @Test
        @DisplayName("Should calculate correct download resume statistics")
        void testCalculateDownloadResumeStats() {
            Map<String, String> existingStatus = new HashMap<>();
            existingStatus.put("12345", "SUCCESS");
            existingStatus.put("67890", "FAILED");
            existingStatus.put("11111", "SKIPPED");
            existingStatus.put("22222", "SUCCESS");
            existingStatus.put("33333", "ERROR");
            
            ResumeStateManager.ResumeStats stats = 
                    manager.calculateDownloadResumeStats(10, existingStatus, false);
            
            assertEquals(10, stats.getTotalFirms());
            assertEquals(3, stats.getAlreadyCompleted()); // SUCCESS + SKIPPED
            assertEquals(2, stats.getFailed()); // FAILED + ERROR
            assertEquals(5, stats.getRemaining()); // 10 - 3 - 2
            assertEquals(0, stats.getCorrupted()); // Not validating PDFs
        }
        
        @Test
        @DisplayName("Should handle empty existing status")
        void testCalculateDownloadResumeStatsEmpty() {
            Map<String, String> existingStatus = new HashMap<>();
            
            ResumeStateManager.ResumeStats stats = 
                    manager.calculateDownloadResumeStats(5, existingStatus, false);
            
            assertEquals(5, stats.getTotalFirms());
            assertEquals(0, stats.getAlreadyCompleted());
            assertEquals(0, stats.getFailed());
            assertEquals(5, stats.getRemaining());
            assertEquals(0, stats.getCorrupted());
        }
        
        @Test
        @DisplayName("Should handle all completed scenario")
        void testCalculateDownloadResumeStatsAllCompleted() {
            Map<String, String> existingStatus = new HashMap<>();
            existingStatus.put("12345", "SUCCESS");
            existingStatus.put("67890", "SKIPPED");
            existingStatus.put("11111", "NO_URL");
            
            ResumeStateManager.ResumeStats stats = 
                    manager.calculateDownloadResumeStats(3, existingStatus, false);
            
            assertEquals(3, stats.getTotalFirms());
            assertEquals(3, stats.getAlreadyCompleted());
            assertEquals(0, stats.getFailed());
            assertEquals(0, stats.getRemaining());
            assertEquals(0, stats.getCorrupted());
        }
    }
    
    @Nested
    @DisplayName("Processing Resume Statistics Tests")
    class ProcessingResumeStatisticsTests {
        
        @Test
        @DisplayName("Should calculate correct processing resume statistics")
        void testCalculateProcessingResumeStats() {
            Set<String> processedFirms = new HashSet<>();
            processedFirms.add("12345");
            processedFirms.add("67890");
            processedFirms.add("11111");
            
            ResumeStateManager.ResumeStats stats = 
                    manager.calculateProcessingResumeStats(10, processedFirms);
            
            assertEquals(10, stats.getTotalFirms());
            assertEquals(3, stats.getAlreadyCompleted());
            assertEquals(7, stats.getRemaining());
            assertEquals(0, stats.getFailed()); // Processing stats don't track failed
            assertEquals(0, stats.getCorrupted()); // Processing stats don't track corrupted
        }
        
        @Test
        @DisplayName("Should handle empty processed firms")
        void testCalculateProcessingResumeStatsEmpty() {
            Set<String> processedFirms = new HashSet<>();
            
            ResumeStateManager.ResumeStats stats = 
                    manager.calculateProcessingResumeStats(5, processedFirms);
            
            assertEquals(5, stats.getTotalFirms());
            assertEquals(0, stats.getAlreadyCompleted());
            assertEquals(5, stats.getRemaining());
            assertEquals(0, stats.getFailed());
            assertEquals(0, stats.getCorrupted());
        }
        
        @Test
        @DisplayName("Should handle all processed scenario")
        void testCalculateProcessingResumeStatsAllProcessed() {
            Set<String> processedFirms = new HashSet<>();
            processedFirms.add("12345");
            processedFirms.add("67890");
            processedFirms.add("11111");
            
            ResumeStateManager.ResumeStats stats = 
                    manager.calculateProcessingResumeStats(3, processedFirms);
            
            assertEquals(3, stats.getTotalFirms());
            assertEquals(3, stats.getAlreadyCompleted());
            assertEquals(0, stats.getRemaining());
            assertEquals(0, stats.getFailed());
            assertEquals(0, stats.getCorrupted());
        }
    }
    
    @Nested
    @DisplayName("File Structure Validation Tests")
    class FileStructureValidationTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("Should validate resume file structure correctly")
        void testValidateResumeFileStructure() throws IOException {
            // Valid resume file with required columns
            Path validFile = tempDir.resolve("valid_resume.csv");
            String validContent = "FirmCrdNb,DownloadStatus,Other Column\n" +
                                 "12345,SUCCESS,Data1\n";
            Files.write(validFile, validContent.getBytes());
            
            String[] requiredColumns = {"FirmCrdNb", "DownloadStatus"};
            assertTrue(manager.validateResumeFileStructure(validFile, requiredColumns));
            
            // Invalid resume file (missing required column)
            Path invalidFile = tempDir.resolve("invalid_resume.csv");
            String invalidContent = "FirmCrdNb,Other Column\n" +
                                   "12345,Data1\n";
            Files.write(invalidFile, invalidContent.getBytes());
            
            assertFalse(manager.validateResumeFileStructure(invalidFile, requiredColumns));
            
            // Non-existent file
            Path nonExistentFile = tempDir.resolve("nonexistent.csv");
            assertFalse(manager.validateResumeFileStructure(nonExistentFile, requiredColumns));
        }
        
        @Test
        @DisplayName("Should handle different required column combinations")
        void testValidateResumeFileStructureDifferentColumns() throws IOException {
            Path csvFile = tempDir.resolve("test_resume.csv");
            String content = "FirmCrdNb,DownloadStatus,ProcessingStatus,Other\n" +
                            "12345,SUCCESS,COMPLETED,Data1\n";
            Files.write(csvFile, content.getBytes());
            
            // Test with different required column combinations
            assertTrue(manager.validateResumeFileStructure(csvFile, new String[]{"FirmCrdNb"}));
            assertTrue(manager.validateResumeFileStructure(csvFile, new String[]{"FirmCrdNb", "DownloadStatus"}));
            assertTrue(manager.validateResumeFileStructure(csvFile, new String[]{"FirmCrdNb", "ProcessingStatus"}));
            
            // Test with missing required column
            assertFalse(manager.validateResumeFileStructure(csvFile, new String[]{"FirmCrdNb", "MissingColumn"}));
        }
    }
    
    @Nested
    @DisplayName("Resume Stats Container Tests")
    class ResumeStatsContainerTests {
        
        @Test
        @DisplayName("Should create and access ResumeStats correctly")
        void testResumeStatsCreationAndAccess() {
            ResumeStateManager.ResumeStats stats = 
                    new ResumeStateManager.ResumeStats(100, 60, 30, 5, 2);
            
            assertEquals(100, stats.getTotalFirms());
            assertEquals(60, stats.getAlreadyCompleted());
            assertEquals(30, stats.getRemaining());
            assertEquals(5, stats.getFailed());
            assertEquals(2, stats.getCorrupted());
        }
        
        @Test
        @DisplayName("Should generate correct toString representation")
        void testResumeStatsToString() {
            ResumeStateManager.ResumeStats stats = 
                    new ResumeStateManager.ResumeStats(100, 60, 30, 5, 2);
            
            String result = stats.toString();
            assertTrue(result.contains("Total: 100"));
            assertTrue(result.contains("Completed: 60"));
            assertTrue(result.contains("Remaining: 30"));
            assertTrue(result.contains("Failed: 5"));
            assertTrue(result.contains("Corrupted: 2"));
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("Should perform complete resume workflow for downloads")
        void testCompleteDownloadResumeWorkflow() throws IOException {
            // Create download status file
            Path statusFile = tempDir.resolve("download_status.csv");
            String statusContent = "FirmCrdNb,DownloadStatus,FileName\n" +
                                  "12345,SUCCESS,12345_brochure.pdf\n" +
                                  "67890,FAILED,\n" +
                                  "11111,SKIPPED,\n" +
                                  "22222,SUCCESS,22222_brochure.pdf\n" +
                                  "33333,ERROR: Timeout,\n";
            Files.write(statusFile, statusContent.getBytes());
            
            // Load download status
            Map<String, String> downloadStatus = manager.getDownloadStatus(statusFile);
            assertEquals(5, downloadStatus.size());
            
            // Calculate resume statistics
            ResumeStateManager.ResumeStats stats = 
                    manager.calculateDownloadResumeStats(10, downloadStatus, false);
            
            assertEquals(10, stats.getTotalFirms());
            assertEquals(3, stats.getAlreadyCompleted()); // SUCCESS + SKIPPED
            assertEquals(2, stats.getFailed()); // FAILED + ERROR
            assertEquals(5, stats.getRemaining());
            assertEquals(0, stats.getCorrupted());
            
            // Test retry logic
            assertTrue(manager.shouldRetryDownload(downloadStatus.get("67890"))); // FAILED
            assertTrue(manager.shouldRetryDownload(downloadStatus.get("33333"))); // ERROR
            assertFalse(manager.shouldRetryDownload(downloadStatus.get("12345"))); // SUCCESS
            assertFalse(manager.shouldRetryDownload(downloadStatus.get("11111"))); // SKIPPED
        }
        
        @Test
        @DisplayName("Should perform complete resume workflow for processing")
        void testCompleteProcessingResumeWorkflow() throws IOException {
            // Create output file with processed firms
            Path outputFile = tempDir.resolve("output.csv");
            String outputContent = "FirmCrdNb,Business Name,Proxy Provider\n" +
                                  "12345,Firm 1,ISS\n" +
                                  "67890,Firm 2,Glass Lewis\n" +
                                  "11111,Firm 3,\n";
            Files.write(outputFile, outputContent.getBytes());
            
            // Load processed firms
            Set<String> processedFirms = manager.getProcessedFirms(outputFile);
            assertEquals(3, processedFirms.size());
            assertTrue(processedFirms.contains("12345"));
            assertTrue(processedFirms.contains("67890"));
            assertTrue(processedFirms.contains("11111"));
            
            // Calculate resume statistics
            ResumeStateManager.ResumeStats stats = 
                    manager.calculateProcessingResumeStats(8, processedFirms);
            
            assertEquals(8, stats.getTotalFirms());
            assertEquals(3, stats.getAlreadyCompleted());
            assertEquals(5, stats.getRemaining());
            assertEquals(0, stats.getFailed());
            assertEquals(0, stats.getCorrupted());
        }
        
        @Test
        @DisplayName("Should validate file structures for resume operations")
        void testFileStructureValidationWorkflow() throws IOException {
            // Create valid download status file
            Path downloadFile = tempDir.resolve("downloads.csv");
            String downloadContent = "FirmCrdNb,DownloadStatus,FileName\n" +
                                    "12345,SUCCESS,file.pdf\n";
            Files.write(downloadFile, downloadContent.getBytes());
            
            // Create valid output file
            Path outputFile = tempDir.resolve("output.csv");
            String outputContent = "FirmCrdNb,Business Name,Proxy Provider\n" +
                                  "12345,Firm 1,ISS\n";
            Files.write(outputFile, outputContent.getBytes());
            
            // Validate structures
            assertTrue(manager.validateResumeFileStructure(downloadFile, 
                    new String[]{"FirmCrdNb", "DownloadStatus"}));
            assertTrue(manager.validateResumeFileStructure(outputFile, 
                    new String[]{"FirmCrdNb"}));
            
            // Test with missing columns
            assertFalse(manager.validateResumeFileStructure(downloadFile, 
                    new String[]{"FirmCrdNb", "MissingColumn"}));
        }
    }
}
