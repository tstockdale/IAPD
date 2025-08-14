package com.iss.iapd.services.incremental;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.model.FirmData;

/**
 * Manages incremental update logic for IAPD processing
 * Determines which firms need processing based on filing date comparisons
 */
public class IncrementalUpdateManager {
    
    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
    
    /**
     * Incremental update statistics container
     */
    public static class IncrementalStats {
        private final int totalCurrentFirms;
        private final int historicalFirms;
        private final int newFirms;
        private final int updatedFirms;
        private final int unchangedFirms;
        private final int toProcess;
        
        public IncrementalStats(int totalCurrentFirms, int historicalFirms, int newFirms, 
                               int updatedFirms, int unchangedFirms, int toProcess) {
            this.totalCurrentFirms = totalCurrentFirms;
            this.historicalFirms = historicalFirms;
            this.newFirms = newFirms;
            this.updatedFirms = updatedFirms;
            this.unchangedFirms = unchangedFirms;
            this.toProcess = toProcess;
        }
        
        public int getTotalCurrentFirms() { return totalCurrentFirms; }
        public int getHistoricalFirms() { return historicalFirms; }
        public int getNewFirms() { return newFirms; }
        public int getUpdatedFirms() { return updatedFirms; }
        public int getUnchangedFirms() { return unchangedFirms; }
        public int getToProcess() { return toProcess; }
        
        @Override
        public String toString() {
            return String.format("Total Current: %d, Historical: %d, New: %d, Updated: %d, Unchanged: %d, To Process: %d",
                    totalCurrentFirms, historicalFirms, newFirms, updatedFirms, unchangedFirms, toProcess);
        }
    }
    
    /**
     * Parses a filing date string in MM/dd/yyyy format
     * @param dateString the date string to parse
     * @return parsed Date object, or null if parsing fails
     */
    public Date parseFilingDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return dateFormatter.parse(dateString.trim());
        } catch (ParseException e) {
            ProcessingLogger.logWarning("Failed to parse filing date: " + dateString + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Reads historical filing dates from the baseline IAPD_Data.csv file
     * @param baselineFile path to the baseline IAPD_Data.csv file
     * @return map of FirmCrdNb to filing date string
     */
    public Map<String, String> getHistoricalFilingDates(Path baselineFile) {
        Map<String, String> historicalDates = new HashMap<>();
        
        if (!Files.exists(baselineFile)) {
            ProcessingLogger.logInfo("Baseline file does not exist: " + baselineFile + " - treating as first run");
            return historicalDates;
        }
        
        try (Reader reader = Files.newBufferedReader(baselineFile, StandardCharsets.UTF_8)) {
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
                    String filingDate = record.get("Filing Date");
                    
                    if (firmCrdNb != null && !firmCrdNb.trim().isEmpty()) {
                        historicalDates.put(firmCrdNb.trim(), filingDate != null ? filingDate.trim() : "");
                    }
                } catch (Exception e) {
                    // Skip malformed records
                    ProcessingLogger.logWarning("Skipping malformed record in baseline file: " + e.getMessage());
                }
            }
            
            ProcessingLogger.logInfo("Loaded historical filing dates for " + historicalDates.size() + " firms from baseline file");
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error reading historical filing dates from baseline file: " + baselineFile, e);
        }
        
        return historicalDates;
    }
    
    /**
     * Determines if the current filing date is more recent than the historical filing date
     * @param currentDate current filing date string
     * @param historicalDate historical filing date string
     * @return true if current date is more recent, false otherwise
     */
    public boolean isFilingDateMoreRecent(String currentDate, String historicalDate) {
        // If no historical date, treat as new firm (process it)
        if (historicalDate == null || historicalDate.trim().isEmpty()) {
            return true;
        }
        
        // If no current date, don't process (conservative approach)
        if (currentDate == null || currentDate.trim().isEmpty()) {
            return false;
        }
        
        Date current = parseFilingDate(currentDate);
        Date historical = parseFilingDate(historicalDate);
        
        // If either date is unparseable, process it (conservative approach)
        if (current == null || historical == null) {
            ProcessingLogger.logWarning("Date parsing failed - processing firm conservatively. Current: " + 
                    currentDate + ", Historical: " + historicalDate);
            return true;
        }
        
        // Return true if current date is after historical date
        return current.after(historical);
    }
    
    /**
     * Determines which firms need processing based on incremental logic
     * @param currentFirms list of current firm data
     * @param historicalDates map of historical filing dates
     * @return set of firm CRD numbers that need processing
     */
    public Set<String> getFirmsToProcess(List<FirmData> currentFirms, Map<String, String> historicalDates) {
        Set<String> firmsToProcess = new HashSet<>();
        
        for (FirmData firm : currentFirms) {
            String firmCrdNb = firm.getFirmCrdNb();
            String currentFilingDate = firm.getFilingDate();
            String historicalFilingDate = historicalDates.get(firmCrdNb);
            
            // Process if firm is new OR has more recent filing date
            if (historicalFilingDate == null || isFilingDateMoreRecent(currentFilingDate, historicalFilingDate)) {
                firmsToProcess.add(firmCrdNb);
            }
        }
        ProcessingLogger.logInfo("Incremental number of firms to process = " + firmsToProcess.size() + ".");
        
        return firmsToProcess;
    }
    
    /**
     * Calculates incremental update statistics
     * @param currentFirms list of current firm data
     * @param historicalDates map of historical filing dates
     * @return incremental statistics
     */
    public IncrementalStats calculateIncrementalStats(List<FirmData> currentFirms, Map<String, String> historicalDates) {
        int totalCurrentFirms = currentFirms.size();
        int historicalFirms = historicalDates.size();
        int newFirms = 0;
        int updatedFirms = 0;
        int unchangedFirms = 0;
        
        for (FirmData firm : currentFirms) {
            String firmCrdNb = firm.getFirmCrdNb();
            String currentFilingDate = firm.getFilingDate();
            String historicalFilingDate = historicalDates.get(firmCrdNb);
            
            if (historicalFilingDate == null) {
                // New firm
                newFirms++;
            } else if (isFilingDateMoreRecent(currentFilingDate, historicalFilingDate)) {
                // Updated firm
                updatedFirms++;
            } else {
                // Unchanged firm
                unchangedFirms++;
            }
        }
        
        int toProcess = newFirms + updatedFirms;
        
        return new IncrementalStats(totalCurrentFirms, historicalFirms, newFirms, 
                                   updatedFirms, unchangedFirms, toProcess);
    }
    
    /**
     * Filters a list of firms to only include those that need processing
     * @param currentFirms list of current firm data
     * @param historicalDates map of historical filing dates
     * @return filtered list containing only firms that need processing
     */
    public List<FirmData> filterFirmsForProcessing(List<FirmData> currentFirms, Map<String, String> historicalDates) {
        Set<String> firmsToProcess = getFirmsToProcess(currentFirms, historicalDates);
        
        return currentFirms.stream()
                .filter(firm -> firmsToProcess.contains(firm.getFirmCrdNb()))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Generates incremental file name based on base name and current date
     * @param baseName base file name (e.g., "IA_FIRM_SEC_DATA")
     * @param date date string (e.g., "20250107")
     * @param extension file extension (e.g., ".csv")
     * @return incremental file name (e.g., "IA_FIRM_SEC_DATA_20250107_incremental.csv")
     */
    public String generateIncrementalFileName(String baseName, String date, String extension) {
        return baseName + "_" + date + "_incremental" + extension;
    }
    
    /**
     * Validates the structure of a baseline CSV file
     * @param baselineFile the baseline file to validate
     * @return true if the file has valid structure for incremental processing
     */
    public boolean validateBaselineFileStructure(Path baselineFile) {
        if (!Files.exists(baselineFile)) {
            return false;
        }
        
        try (Reader reader = Files.newBufferedReader(baselineFile, StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(false)
                    .build();
            
            Iterable<CSVRecord> records = format.parse(reader);
            CSVRecord headerRecord = records.iterator().next();
            
            // Check for required columns
            String[] requiredColumns = {"FirmCrdNb", "Filing Date"};
            for (String column : requiredColumns) {
                if (!headerRecord.toMap().containsKey(column)) {
                    ProcessingLogger.logWarning("Baseline file missing required column for incremental processing: " + column);
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error validating baseline file structure: " + baselineFile, e);
            return false;
        }
    }
    
    /**
     * Logs incremental update statistics in a formatted way
     * @param stats the incremental statistics to log
     * @param baselineFile the baseline file used
     */
    public void logIncrementalStats(IncrementalStats stats, Path baselineFile) {
        ProcessingLogger.logInfo("=== INCREMENTAL UPDATE MODE ===");
        ProcessingLogger.logInfo("Baseline File: " + baselineFile + " (" + stats.getHistoricalFirms() + " historical firms)");
        ProcessingLogger.logInfo("Current XML Data: " + stats.getTotalCurrentFirms() + " firms");
        ProcessingLogger.logInfo("Incremental Analysis:");
        ProcessingLogger.logInfo("  - New firms: " + stats.getNewFirms() + " (not in baseline)");
        ProcessingLogger.logInfo("  - Updated firms: " + stats.getUpdatedFirms() + " (more recent filing dates)");
        ProcessingLogger.logInfo("  - Unchanged firms: " + stats.getUnchangedFirms() + " (skipped)");
        ProcessingLogger.logInfo("  - Total to process: " + stats.getToProcess() + " firms");
    }
}
