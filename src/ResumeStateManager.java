import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

/**
 * Manages resume state for brochure downloading and processing operations
 * Provides utilities for reading existing progress, validating files, and calculating resume statistics
 */
public class ResumeStateManager {
    
    /**
     * Resume statistics container
     */
    public static class ResumeStats {
        private final int totalFirms;
        private final int alreadyCompleted;
        private final int remaining;
        private final int failed;
        private final int corrupted;
        
        public ResumeStats(int totalFirms, int alreadyCompleted, int remaining, int failed, int corrupted) {
            this.totalFirms = totalFirms;
            this.alreadyCompleted = alreadyCompleted;
            this.remaining = remaining;
            this.failed = failed;
            this.corrupted = corrupted;
        }
        
        public int getTotalFirms() { return totalFirms; }
        public int getAlreadyCompleted() { return alreadyCompleted; }
        public int getRemaining() { return remaining; }
        public int getFailed() { return failed; }
        public int getCorrupted() { return corrupted; }
        
        @Override
        public String toString() {
            return String.format("Total: %d, Completed: %d, Remaining: %d, Failed: %d, Corrupted: %d",
                    totalFirms, alreadyCompleted, remaining, failed, corrupted);
        }
    }
    
    /**
     * Reads existing download status from a CSV file with download status column
     * @param csvFile path to CSV file with DownloadStatus column
     * @return map of FirmCrdNb to download status
     */
    public Map<String, String> getDownloadStatus(Path csvFile) {
        Map<String, String> statusMap = new HashMap<>();
        
        if (!Files.exists(csvFile)) {
            return statusMap;
        }
        
        try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                try {
                    String firmCrdNb = record.get("FirmCrdNb");
                    String downloadStatus = record.get("DownloadStatus");
                    
                    if (firmCrdNb != null && !firmCrdNb.trim().isEmpty()) {
                        statusMap.put(firmCrdNb.trim(), downloadStatus != null ? downloadStatus.trim() : "");
                    }
                } catch (Exception e) {
                    // Skip malformed records
                    ProcessingLogger.logWarning("Skipping malformed record in resume file: " + e.getMessage());
                }
            }
            
            ProcessingLogger.logInfo("Loaded download status for " + statusMap.size() + " firms from resume file");
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error reading download status from resume file: " + csvFile, e);
        }
        
        return statusMap;
    }
    
    /**
     * Reads existing processed firms from the final output CSV file
     * @param outputFile path to IAPD_Found.csv or similar output file
     * @return set of FirmCrdNb values that have already been processed
     */
    public Set<String> getProcessedFirms(Path outputFile) {
        Set<String> processedFirms = new HashSet<>();
        
        if (!Files.exists(outputFile)) {
            return processedFirms;
        }
        
        try (Reader reader = Files.newBufferedReader(outputFile, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                try {
                    String firmCrdNb = record.get("FirmCrdNb");
                    if (firmCrdNb != null && !firmCrdNb.trim().isEmpty()) {
                        processedFirms.add(firmCrdNb.trim());
                    }
                } catch (Exception e) {
                    // Skip malformed records
                    ProcessingLogger.logWarning("Skipping malformed record in output file: " + e.getMessage());
                }
            }
            
            ProcessingLogger.logInfo("Found " + processedFirms.size() + " already processed firms in output file");
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error reading processed firms from output file: " + outputFile, e);
        }
        
        return processedFirms;
    }
    
    /**
     * Validates PDF file integrity
     * @param pdfFile the PDF file to validate
     * @return true if the PDF file is valid and readable
     */
    public boolean validatePdfFile(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            return false;
        }
        
        // Check minimum file size (1KB)
        if (pdfFile.length() < 1024) {
            ProcessingLogger.logWarning("PDF file too small: " + pdfFile.getName() + " (" + pdfFile.length() + " bytes)");
            return false;
        }
        
        try (FileInputStream fis = new FileInputStream(pdfFile)) {
            // Check PDF magic bytes
            byte[] header = new byte[4];
            int bytesRead = fis.read(header);
            if (bytesRead < 4 || !Arrays.equals(header, "%PDF".getBytes())) {
                ProcessingLogger.logWarning("Invalid PDF header: " + pdfFile.getName());
                return false;
            }
            
            // Reset stream and try to extract text
            try (FileInputStream textStream = new FileInputStream(pdfFile)) {
                String text = PdfTextExtractor.getCleanedBrochureText(textStream);
                if (text == null || text.trim().length() == 0) {
                    ProcessingLogger.logWarning("PDF contains no extractable text: " + pdfFile.getName());
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            ProcessingLogger.logWarning("PDF validation failed for " + pdfFile.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Determines if a download should be retried based on current status
     * @param currentStatus the current download status
     * @return true if the download should be retried
     */
    public boolean shouldRetryDownload(String currentStatus) {
        if (currentStatus == null || currentStatus.trim().isEmpty()) {
            return true; // No status means not attempted
        }
        
        String status = currentStatus.trim().toUpperCase();
        
        // Retry failed downloads
        if (status.equals("FAILED") || status.startsWith("FAILED:") || 
            status.equals("ERROR") || status.startsWith("ERROR:")) {
            return true;
        }
        
        // Don't retry successful or skipped downloads
        if (status.equals("SUCCESS") || status.equals("SKIPPED") || 
            status.equals("NO_URL") || status.equals("INVALID_URL")) {
            return false;
        }
        
        // Default to retry for unknown statuses
        return true;
    }
    
    /**
     * Calculates resume statistics for download operations
     * @param totalFirms total number of firms to process
     * @param existingStatus map of existing download statuses
     * @param validatePdfs whether to validate existing PDF files
     * @return resume statistics
     */
    public ResumeStats calculateDownloadResumeStats(int totalFirms, Map<String, String> existingStatus, boolean validatePdfs) {
        int alreadyCompleted = 0;
        int failed = 0;
        int corrupted = 0;
        
        for (Map.Entry<String, String> entry : existingStatus.entrySet()) {
            String status = entry.getValue();
            
            if ("SUCCESS".equals(status)) {
                if (validatePdfs) {
                    // Check if PDF file actually exists and is valid
                    String firmCrdNb = entry.getKey();
                    File pdfFile = findPdfFileForFirm(firmCrdNb);
                    if (pdfFile != null && validatePdfFile(pdfFile)) {
                        alreadyCompleted++;
                    } else {
                        corrupted++;
                    }
                } else {
                    alreadyCompleted++;
                }
            } else if (shouldRetryDownload(status)) {
                failed++;
            } else {
                alreadyCompleted++; // SKIPPED, NO_URL, INVALID_URL, etc.
            }
        }
        
        int remaining = totalFirms - alreadyCompleted - failed;
        return new ResumeStats(totalFirms, alreadyCompleted, remaining, failed, corrupted);
    }
    
    /**
     * Calculates resume statistics for processing operations
     * @param totalFirms total number of firms to process
     * @param processedFirms set of already processed firm CRD numbers
     * @return resume statistics
     */
    public ResumeStats calculateProcessingResumeStats(int totalFirms, Set<String> processedFirms) {
        int alreadyCompleted = processedFirms.size();
        int remaining = totalFirms - alreadyCompleted;
        return new ResumeStats(totalFirms, alreadyCompleted, remaining, 0, 0);
    }
    
    /**
     * Finds the PDF file for a given firm CRD number
     * @param firmCrdNb the firm CRD number
     * @return the PDF file if found, null otherwise
     */
    private File findPdfFileForFirm(String firmCrdNb) {
        File downloadDir = new File(Config.DOWNLOAD_PATH);
        if (!downloadDir.exists()) {
            return null;
        }
        
        // Look for files matching the pattern: firmCrdNb_*.pdf
        File[] pdfFiles = downloadDir.listFiles((dir, name) -> 
            name.startsWith(firmCrdNb + "_") && name.endsWith(".pdf"));
        
        return (pdfFiles != null && pdfFiles.length > 0) ? pdfFiles[0] : null;
    }
    
    /**
     * Validates the structure of a resume CSV file
     * @param csvFile the CSV file to validate
     * @param requiredColumns array of required column names
     * @return true if the file has valid structure
     */
    public boolean validateResumeFileStructure(Path csvFile, String[] requiredColumns) {
        if (!Files.exists(csvFile)) {
            return false;
        }
        
        try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(false)
                    .build();
            
            Iterable<CSVRecord> records = format.parse(reader);
            CSVRecord headerRecord = records.iterator().next();
            
            // Check if all required columns are present
            for (String column : requiredColumns) {
                if (!headerRecord.toMap().containsKey(column)) {
                    ProcessingLogger.logWarning("Resume file missing required column: " + column);
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error validating resume file structure: " + csvFile, e);
            return false;
        }
    }
}
