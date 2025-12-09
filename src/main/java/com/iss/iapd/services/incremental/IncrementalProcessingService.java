package com.iss.iapd.services.incremental;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.model.FirmData;
import com.iss.iapd.utils.DateComparator;

/**
 * Unified service for incremental processing operations
 * Consolidates all incremental processing logic into a single cohesive service
 */
public class IncrementalProcessingService {

    private final BaselineDataReader baselineDataReader;

    /**
     * Constructs an IncrementalProcessingService with default dependencies
     */
    public IncrementalProcessingService() {
        this.baselineDataReader = new BaselineDataReader();
    }

    /**
     * Constructs an IncrementalProcessingService with custom BaselineDataReader
     * @param baselineDataReader the baseline data reader to use
     */
    public IncrementalProcessingService(BaselineDataReader baselineDataReader) {
        this.baselineDataReader = baselineDataReader;
    }

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
     * Reads baseline data from the specified file
     * @param baselineFile path to the baseline CSV file
     * @return BaselineData containing historical information
     */
    public BaselineDataReader.BaselineData readBaselineData(Path baselineFile) {
        return baselineDataReader.readBaselineData(baselineFile);
    }

    /**
     * Reads baseline data from the latest file in the output directory
     * @param outputDirectory the directory containing IAPD_DATA files
     * @return BaselineData from the latest file
     */
    public BaselineDataReader.BaselineData readLatestBaselineData(Path outputDirectory) {
        return baselineDataReader.readLatestBaselineData(outputDirectory);
    }

    /**
     * Determines which firms need processing based on incremental logic
     * @param currentFirms list of current firm data
     * @param historicalDates map of historical filing dates (FirmCrdNb -> filing date)
     * @return set of firm CRD numbers that need processing
     */
    public Set<String> getFirmsToProcess(List<FirmData> currentFirms, Map<String, String> historicalDates) {
        Set<String> firmsToProcess = new HashSet<>();

        for (FirmData firm : currentFirms) {
            String firmCrdNb = firm.getFirmCrdNb();
            String currentFilingDate = firm.getFilingDate();
            String historicalFilingDate = historicalDates.get(firmCrdNb);

            // Process if firm is new OR has more recent filing date
            if (historicalFilingDate == null || DateComparator.isFilingDateMoreRecent(currentFilingDate, historicalFilingDate)) {
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
            } else if (DateComparator.isFilingDateMoreRecent(currentFilingDate, historicalFilingDate)) {
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
     * Checks if a brochure version ID should be skipped (already processed)
     * @param brochureVersionId the brochure version ID to check
     * @param existingVersionIds set of already processed version IDs
     * @return true if should skip, false if should process
     */
    public boolean shouldSkipBrochureVersionId(String brochureVersionId, Set<String> existingVersionIds) {
        if (brochureVersionId == null || brochureVersionId.trim().isEmpty()) {
            return false; // Don't skip if no version ID
        }

        return existingVersionIds.contains(brochureVersionId.trim());
    }

    /**
     * Generates incremental file name based on base name and current date
     * @param baseName base file name (e.g., "IA_FIRM_SEC_DATA")
     * @param date date string (e.g., "20250107")
     * @param extension file extension (e.g., ".csv")
     * @return incremental file name (e.g., "IA_FIRM_SEC_DATA_20250107_incremental.csv")
     */
    public String generateIncrementalFileName(String baseName, String date, String extension) {
        return baselineDataReader.generateIncrementalFileName(baseName, date, extension);
    }

    /**
     * Validates the structure of a baseline CSV file
     * @param baselineFile the baseline file to validate
     * @return true if the file has valid structure for incremental processing
     */
    public boolean validateBaselineFileStructure(Path baselineFile) {
        return baselineDataReader.validateFileStructure(baselineFile);
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

    /**
     * Gets the baseline data reader instance
     * @return the baseline data reader
     */
    public BaselineDataReader getBaselineDataReader() {
        return baselineDataReader;
    }
}
