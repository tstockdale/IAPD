package com.iss.iapd.services.incremental;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import com.iss.iapd.config.Config;
import com.iss.iapd.config.ProcessingLogger;

/**
 * Service for handling resume URL extraction functionality
 * Finds existing URL extraction progress and determines resume point
 */
public class ResumeURLExtractionService {
    
    /**
     * Resume information container for URL extraction
     */
    public static class ResumeInfo {
        private final Path filesToDownloadPath;
        private final Path firmDataPath;
        private final String lastCompleteFirmId;
        private final int resumeIndex;
        private final int totalFirmsInInput;
        private final int completedFirms;
        
        public ResumeInfo(Path filesToDownloadPath, Path firmDataPath, String lastCompleteFirmId,
                         int resumeIndex, int totalFirmsInInput, int completedFirms) {
            this.filesToDownloadPath = filesToDownloadPath;
            this.firmDataPath = firmDataPath;
            this.lastCompleteFirmId = lastCompleteFirmId;
            this.resumeIndex = resumeIndex;
            this.totalFirmsInInput = totalFirmsInInput;
            this.completedFirms = completedFirms;
        }
        
        public Path getFilesToDownloadPath() { return filesToDownloadPath; }
        public Path getFirmDataPath() { return firmDataPath; }
        public String getLastCompleteFirmId() { return lastCompleteFirmId; }
        public int getResumeIndex() { return resumeIndex; }
        public int getTotalFirmsInInput() { return totalFirmsInInput; }
        public int getCompletedFirms() { return completedFirms; }
        public int getRemainingFirms() { return totalFirmsInInput - resumeIndex; }
        
        public boolean hasWorkToDo() { return resumeIndex < totalFirmsInInput; }
    }
    
    /**
     * Finds the most recent FilesToDownload.csv file
     * @return path to the most recent file, or null if none found
     */
    public Path findLatestFilesToDownload() {
        File inputDir = new File(Config.BROCHURE_INPUT_PATH);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            ProcessingLogger.logWarning("Input directory does not exist: " + Config.BROCHURE_INPUT_PATH);
            return null;
        }
        
        File[] files = inputDir.listFiles((dir, name) -> 
            name.startsWith("FilesToDownload") && name.endsWith(".csv") && !name.contains("_with_downloads"));
        
        if (files == null || files.length == 0) {
            ProcessingLogger.logInfo("No FilesToDownload.csv files found for resume");
            return null;
        }
        
        // Find the most recent file by modification time
        File mostRecent = files[0];
        for (File file : files) {
            if (file.lastModified() > mostRecent.lastModified()) {
                mostRecent = file;
            }
        }
        
        ProcessingLogger.logInfo("Found most recent FilesToDownload file: " + mostRecent.getName());
        return mostRecent.toPath();
    }
    
    /**
     * Finds the corresponding IA_FIRM_SEC_DATA.csv file based on date matching
     * @param filesToDownloadPath path to the FilesToDownload file
     * @return path to the corresponding firm data file, or null if not found
     */
    public Path findCorrespondingFirmDataFile(Path filesToDownloadPath) {
        if (filesToDownloadPath == null) {
            return null;
        }
        
        // Extract date from FilesToDownload filename (e.g., "FilesToDownload_20250819.csv")
        String fileName = filesToDownloadPath.getFileName().toString();
        String dateString = extractDateFromFileName(fileName, "FilesToDownload_", ".csv");
        
        if (dateString == null) {
            ProcessingLogger.logWarning("Could not extract date from FilesToDownload filename: " + fileName);
            return null;
        }
        
        // Look for corresponding IA_FIRM_SEC_DATA file
        File inputDir = new File(Config.BROCHURE_INPUT_PATH);
        String expectedFirmDataName = "IA_FIRM_SEC_DATA_" + dateString + ".csv";
        File firmDataFile = new File(inputDir, expectedFirmDataName);
        
        if (firmDataFile.exists()) {
            ProcessingLogger.logInfo("Found corresponding firm data file: " + expectedFirmDataName);
            return firmDataFile.toPath();
        } else {
            ProcessingLogger.logWarning("Could not find corresponding firm data file: " + expectedFirmDataName);
            return null;
        }
    }
    
    /**
     * Finds the last complete firm ID in the FilesToDownload file
     * A complete record must have all required fields: firmId, firmName, brochureVersionId, brochureName, dateSubmitted
     * @param filesToDownloadPath path to the FilesToDownload file
     * @return the FirmCrdNb of the last complete firm, or null if none found
     */
    public String findLastCompleteFirmId(Path filesToDownloadPath) {
        if (!Files.exists(filesToDownloadPath)) {
            ProcessingLogger.logError("FilesToDownload file does not exist: " + filesToDownloadPath, null);
            return null;
        }
        
        String lastCompleteFirmId = null;
        int recordCount = 0;
        
        try (Reader reader = Files.newBufferedReader(filesToDownloadPath, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                recordCount++;
                try {
                    String firmId = record.get("firmId");
                    String firmName = record.get("firmName");
                    String brochureVersionId = record.get("brochureVersionId");
                    String brochureName = record.get("brochureName");
                    String dateSubmitted = record.get("dateSubmitted");
                    
                    // Check if all required fields are present and non-empty
                    if (isNotEmpty(firmId) && isNotEmpty(firmName) && isNotEmpty(brochureVersionId) && 
                        isNotEmpty(brochureName) && isNotEmpty(dateSubmitted)) {
                        lastCompleteFirmId = firmId;
                    } else {
                        ProcessingLogger.logInfo("Found incomplete record at index " + (recordCount - 1) + 
                            " - firmId: " + firmId + ", missing or empty fields detected");
                        break; // Stop at first incomplete record
                    }
                } catch (Exception e) {
                    ProcessingLogger.logWarning("Error reading record at index " + (recordCount - 1) + ": " + e.getMessage());
                    break;
                }
            }
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error reading FilesToDownload file for resume analysis", e);
            return null;
        }
        
        ProcessingLogger.logInfo("Found " + recordCount + " records in FilesToDownload file");
        if (lastCompleteFirmId != null) {
            ProcessingLogger.logInfo("Last complete firm ID: " + lastCompleteFirmId);
        } else {
            ProcessingLogger.logInfo("No complete records found in FilesToDownload file");
        }
        
        return lastCompleteFirmId;
    }
    
    /**
     * Finds the resume point in the firm data file based on the last complete firm ID
     * @param firmDataPath path to the IA_FIRM_SEC_DATA file
     * @param lastCompleteFirmId the last complete firm ID from FilesToDownload
     * @return the index to resume from (next record after the last complete one), or 0 if not found
     */
    public int findResumePointInFirmData(Path firmDataPath, String lastCompleteFirmId) {
        if (!Files.exists(firmDataPath) || lastCompleteFirmId == null) {
            ProcessingLogger.logWarning("Cannot find resume point - firm data file missing or no last complete firm ID");
            return 0;
        }
        
        int currentIndex = 0;
        int resumeIndex = 0;
        
        try (Reader reader = Files.newBufferedReader(firmDataPath, StandardCharsets.UTF_8)) {
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
                    if (lastCompleteFirmId.equals(firmCrdNb)) {
                        resumeIndex = currentIndex + 1; // Resume from next record
                        ProcessingLogger.logInfo("Found matching firm " + lastCompleteFirmId + " at index " + currentIndex);
                        ProcessingLogger.logInfo("Will resume from index " + resumeIndex);
                        break;
                    }
                    currentIndex++;
                } catch (Exception e) {
                    ProcessingLogger.logWarning("Error reading firm data record at index " + currentIndex + ": " + e.getMessage());
                    currentIndex++;
                }
            }
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error finding resume point in firm data file", e);
            return 0;
        }
        
        return resumeIndex;
    }
    
    /**
     * Counts the total number of records in a CSV file
     * @param csvFile path to the CSV file
     * @return number of records (excluding header)
     */
    private int countRecordsInFile(Path csvFile) {
        int count = 0;
        try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (@SuppressWarnings("unused") CSVRecord ignored : records) {
                count++;
            }
        } catch (Exception e) {
            ProcessingLogger.logError("Error counting records in file: " + csvFile, e);
        }
        return count;
    }
    
    /**
     * Determines if resume URL extraction is possible
     * @return ResumeInfo if resume is possible, null otherwise
     */
    public ResumeInfo checkResumeCapability() {
        // Find the required files
        Path filesToDownloadPath = findLatestFilesToDownload();
        if (filesToDownloadPath == null) {
            ProcessingLogger.logInfo("Resume URL extraction not possible: No FilesToDownload file found");
            return null;
        }
        
        Path firmDataPath = findCorrespondingFirmDataFile(filesToDownloadPath);
        if (firmDataPath == null) {
            ProcessingLogger.logInfo("Resume URL extraction not possible: No corresponding firm data file found");
            return null;
        }
        
        // Find the last complete firm ID
        String lastCompleteFirmId = findLastCompleteFirmId(filesToDownloadPath);
        if (lastCompleteFirmId == null) {
            ProcessingLogger.logInfo("Resume URL extraction not possible: No complete records found in FilesToDownload");
            return null;
        }
        
        // Find resume point in firm data
        int resumeIndex = findResumePointInFirmData(firmDataPath, lastCompleteFirmId);
        int totalFirmsInInput = countRecordsInFile(firmDataPath);
        
        if (resumeIndex >= totalFirmsInInput) {
            ProcessingLogger.logInfo("Resume URL extraction not needed: All firms appear to be processed");
            return null;
        }
        
        // Count completed firms (those that have been processed)
        int completedFirms = resumeIndex;
        
        return new ResumeInfo(filesToDownloadPath, firmDataPath, lastCompleteFirmId, 
                            resumeIndex, totalFirmsInInput, completedFirms);
    }
    
    /**
     * Logs resume statistics in a formatted way
     */
    public void logResumeStats(ResumeInfo resumeInfo) {
        ProcessingLogger.logInfo("=== RESUME URL EXTRACTION MODE ===");
        ProcessingLogger.logInfo("FilesToDownload: " + resumeInfo.getFilesToDownloadPath().getFileName());
        ProcessingLogger.logInfo("Firm Data File: " + resumeInfo.getFirmDataPath().getFileName());
        ProcessingLogger.logInfo("Resume Analysis:");
        ProcessingLogger.logInfo("  - Total firms in input: " + resumeInfo.getTotalFirmsInInput());
        ProcessingLogger.logInfo("  - Already processed: " + resumeInfo.getCompletedFirms());
        ProcessingLogger.logInfo("  - Last complete firm ID: " + resumeInfo.getLastCompleteFirmId());
        ProcessingLogger.logInfo("  - Resume from index: " + resumeInfo.getResumeIndex());
        ProcessingLogger.logInfo("  - Remaining to process: " + resumeInfo.getRemainingFirms());
        ProcessingLogger.logInfo("===================================");
    }
    
    /**
     * Helper method to check if a string is not empty
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Helper method to extract date string from filename
     */
    private String extractDateFromFileName(String fileName, String prefix, String suffix) {
        if (fileName.startsWith(prefix) && fileName.endsWith(suffix)) {
            int startIndex = prefix.length();
            int endIndex = fileName.length() - suffix.length();
            if (startIndex < endIndex) {
                return fileName.substring(startIndex, endIndex);
            }
        }
        return null;
    }
}
