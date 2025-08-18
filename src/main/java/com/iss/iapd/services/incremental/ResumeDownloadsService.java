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
 * Service for handling resume downloads functionality
 * Finds existing download progress and determines resume point
 */
public class ResumeDownloadsService {
    
    /**
     * Resume information container
     */
    public static class ResumeInfo {
        private final Path filesToDownloadPath;
        private final Path filesToDownloadWithDownloadsPath;
        private final int resumeIndex;
        private final int totalRecords;
        private final int completedRecords;
        
        public ResumeInfo(Path filesToDownloadPath, Path filesToDownloadWithDownloadsPath, 
                         int resumeIndex, int totalRecords, int completedRecords) {
            this.filesToDownloadPath = filesToDownloadPath;
            this.filesToDownloadWithDownloadsPath = filesToDownloadWithDownloadsPath;
            this.resumeIndex = resumeIndex;
            this.totalRecords = totalRecords;
            this.completedRecords = completedRecords;
        }
        
        public Path getFilesToDownloadPath() { return filesToDownloadPath; }
        public Path getFilesToDownloadWithDownloadsPath() { return filesToDownloadWithDownloadsPath; }
        public int getResumeIndex() { return resumeIndex; }
        public int getTotalRecords() { return totalRecords; }
        public int getCompletedRecords() { return completedRecords; }
        public int getRemainingRecords() { return totalRecords - resumeIndex; }
        
        public boolean hasWorkToDo() { return resumeIndex < totalRecords; }
    }
    
    /**
     * Finds the most recent FilesToDownload_with_downloads.csv file
     * @return path to the most recent file, or null if none found
     */
    public Path findLatestFilesToDownloadWithDownloads() {
        File inputDir = new File(Config.BROCHURE_INPUT_PATH);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            ProcessingLogger.logWarning("Input directory does not exist: " + Config.BROCHURE_INPUT_PATH);
            return null;
        }
        
        File[] files = inputDir.listFiles((dir, name) -> 
            name.startsWith("FilesToDownload") && name.contains("_with_downloads.csv"));
        
        if (files == null || files.length == 0) {
            ProcessingLogger.logInfo("No FilesToDownload_with_downloads.csv files found for resume");
            return null;
        }
        
        // Find the most recent file by modification time
        File mostRecent = files[0];
        for (File file : files) {
            if (file.lastModified() > mostRecent.lastModified()) {
                mostRecent = file;
            }
        }
        
        ProcessingLogger.logInfo("Found most recent download status file: " + mostRecent.getName());
        return mostRecent.toPath();
    }
    
    /**
     * Finds the corresponding FilesToDownload.csv file
     * @return path to the FilesToDownload.csv file, or null if not found
     */
    public Path findFilesToDownload() {
        File inputDir = new File(Config.BROCHURE_INPUT_PATH);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            return null;
        }
        
        File[] files = inputDir.listFiles((dir, name) -> 
            name.startsWith("FilesToDownload") && name.endsWith(".csv") && !name.contains("_with_downloads"));
        
        if (files == null || files.length == 0) {
            ProcessingLogger.logWarning("No FilesToDownload.csv file found");
            return null;
        }
        
        // Find the most recent FilesToDownload.csv
        File mostRecent = files[0];
        for (File file : files) {
            if (file.lastModified() > mostRecent.lastModified()) {
                mostRecent = file;
            }
        }
        
        ProcessingLogger.logInfo("Found FilesToDownload file: " + mostRecent.getName());
        return mostRecent.toPath();
    }
    
    /**
     * Analyzes the download progress and determines resume point
     * @param filesToDownloadWithDownloadsPath path to the incomplete download status file
     * @param filesToDownloadPath path to the complete FilesToDownload file
     * @return ResumeInfo containing resume details
     */
    public ResumeInfo analyzeResumePoint(Path filesToDownloadWithDownloadsPath, Path filesToDownloadPath) {
        if (!Files.exists(filesToDownloadWithDownloadsPath) || !Files.exists(filesToDownloadPath)) {
            ProcessingLogger.logError("Required files for resume analysis do not exist", null);
            return null;
        }
        
        try {
            // Count total records in FilesToDownload
            int totalRecords = countRecordsInFile(filesToDownloadPath);
            
            // Find last successful download in the status file
            int lastSuccessfulIndex = findLastSuccessfulDownloadIndex(filesToDownloadWithDownloadsPath);
            
            // Resume point is the next record after the last successful one
            int resumeIndex = lastSuccessfulIndex + 1;
            
            ProcessingLogger.logInfo("Resume analysis complete:");
            ProcessingLogger.logInfo("  Total records: " + totalRecords);
            ProcessingLogger.logInfo("  Last successful index: " + lastSuccessfulIndex);
            ProcessingLogger.logInfo("  Resume from index: " + resumeIndex);
            ProcessingLogger.logInfo("  Remaining records: " + (totalRecords - resumeIndex));
            
            return new ResumeInfo(filesToDownloadPath, filesToDownloadWithDownloadsPath, 
                                resumeIndex, totalRecords, lastSuccessfulIndex + 1);
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error analyzing resume point", e);
            return null;
        }
    }
    
    /**
     * Finds the index of the last successful download in the status file
     * @param filesToDownloadWithDownloadsPath path to the download status file
     * @return index of last successful download, or -1 if none found
     */
    private int findLastSuccessfulDownloadIndex(Path filesToDownloadWithDownloadsPath) {
        int lastSuccessfulIndex = -1;
        int currentIndex = 0;
        
        try (Reader reader = Files.newBufferedReader(filesToDownloadWithDownloadsPath, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                try {
                    String downloadStatus = record.get("downloadStatus");
                    if ("SUCCESS".equals(downloadStatus)) {
                        lastSuccessfulIndex = currentIndex;
                    }
                    currentIndex++;
                } catch (Exception e) {
                    ProcessingLogger.logWarning("Skipping malformed record at index " + currentIndex + ": " + e.getMessage());
                    currentIndex++;
                }
            }
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error reading download status file for resume analysis", e);
        }
        
        return lastSuccessfulIndex;
    }
    
    /**
     * Counts the number of records in a CSV file
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
     * Determines if resume downloads is possible
     * @return ResumeInfo if resume is possible, null otherwise
     */
    public ResumeInfo checkResumeCapability() {
        // Find the required files
        Path filesToDownloadWithDownloadsPath = findLatestFilesToDownloadWithDownloads();
        if (filesToDownloadWithDownloadsPath == null) {
            ProcessingLogger.logInfo("Resume downloads not possible: No download status file found");
            return null;
        }
        
        Path filesToDownloadPath = findFilesToDownload();
        if (filesToDownloadPath == null) {
            ProcessingLogger.logInfo("Resume downloads not possible: No FilesToDownload file found");
            return null;
        }
        
        // Analyze resume point
        ResumeInfo resumeInfo = analyzeResumePoint(filesToDownloadWithDownloadsPath, filesToDownloadPath);
        if (resumeInfo == null) {
            ProcessingLogger.logInfo("Resume downloads not possible: Could not analyze resume point");
            return null;
        }
        
        if (!resumeInfo.hasWorkToDo()) {
            ProcessingLogger.logInfo("Resume downloads not needed: All downloads appear to be complete");
            return null;
        }
        
        return resumeInfo;
    }
    
    /**
     * Logs resume statistics in a formatted way
     */
    public void logResumeStats(ResumeInfo resumeInfo) {
        ProcessingLogger.logInfo("=== RESUME DOWNLOADS MODE ===");
        ProcessingLogger.logInfo("FilesToDownload: " + resumeInfo.getFilesToDownloadPath().getFileName());
        ProcessingLogger.logInfo("Download Status File: " + resumeInfo.getFilesToDownloadWithDownloadsPath().getFileName());
        ProcessingLogger.logInfo("Resume Analysis:");
        ProcessingLogger.logInfo("  - Total records: " + resumeInfo.getTotalRecords());
        ProcessingLogger.logInfo("  - Already completed: " + resumeInfo.getCompletedRecords());
        ProcessingLogger.logInfo("  - Resume from index: " + resumeInfo.getResumeIndex());
        ProcessingLogger.logInfo("  - Remaining to download: " + resumeInfo.getRemainingRecords());
        ProcessingLogger.logInfo("=============================");
    }
}
