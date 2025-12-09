package com.iss.iapd.services.incremental;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import com.iss.iapd.config.Config;
import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.utils.CsvUtils;
import com.iss.iapd.utils.DateComparator;

/**
 * Service for reading baseline/historical data from IAPD output files
 * Consolidates logic for reading filing dates, brochure version IDs, and other historical data
 */
public class BaselineDataReader {

    /**
     * Container for baseline data analysis results
     */
    public static class BaselineData {
        private final Path sourceFile;
        private final Map<String, String> filingDates;
        private final Set<String> brochureVersionIds;
        private final String maxDateSubmitted;
        private final int totalRecords;
        private final boolean hasData;

        public BaselineData(Path sourceFile, Map<String, String> filingDates,
                          Set<String> brochureVersionIds, String maxDateSubmitted,
                          int totalRecords, boolean hasData) {
            this.sourceFile = sourceFile;
            this.filingDates = filingDates != null ? filingDates : new HashMap<>();
            this.brochureVersionIds = brochureVersionIds != null ? brochureVersionIds : new HashSet<>();
            this.maxDateSubmitted = maxDateSubmitted;
            this.totalRecords = totalRecords;
            this.hasData = hasData;
        }

        public Path getSourceFile() { return sourceFile; }
        public Map<String, String> getFilingDates() { return filingDates; }
        public Set<String> getBrochureVersionIds() { return brochureVersionIds; }
        public String getMaxDateSubmitted() { return maxDateSubmitted; }
        public int getTotalRecords() { return totalRecords; }
        public boolean hasData() { return hasData; }

        @Override
        public String toString() {
            return String.format("BaselineData{sourceFile=%s, filingDates=%d, brochureVersionIds=%d, maxDateSubmitted=%s, totalRecords=%d, hasData=%s}",
                    sourceFile != null ? sourceFile.getFileName() : "null",
                    filingDates.size(), brochureVersionIds.size(), maxDateSubmitted, totalRecords, hasData);
        }
    }

    /**
     * Reads baseline data from a specified file
     * @param baselineFile path to the baseline CSV file
     * @return BaselineData containing all historical information
     */
    public BaselineData readBaselineData(Path baselineFile) {
        ProcessingLogger.logInfo("Reading baseline data from: " + baselineFile);

        if (!Files.exists(baselineFile)) {
            ProcessingLogger.logInfo("Baseline file does not exist - treating as first run");
            return new BaselineData(null, new HashMap<>(), new HashSet<>(), null, 0, false);
        }

        if (!validateFileStructure(baselineFile)) {
            ProcessingLogger.logWarning("Baseline file structure invalid - treating as first run");
            return new BaselineData(null, new HashMap<>(), new HashSet<>(), null, 0, false);
        }

        Map<String, String> filingDates = new HashMap<>();
        Set<String> brochureVersionIds = new HashSet<>();
        String maxDateSubmitted = null;
        java.util.Date maxDate = null;

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
                    // Extract FirmCrdNb and filing date
                    String firmCrdNb = getFieldValue(record, "FirmCrdNb");
                    String filingDate = getFieldValue(record, "Filing Date", "Filing_Date", "filingDate");

                    if (firmCrdNb != null && !firmCrdNb.trim().isEmpty()) {
                        filingDates.put(firmCrdNb.trim(), filingDate != null ? filingDate.trim() : "");

                        // Track max date
                        if (filingDate != null && !filingDate.trim().isEmpty()) {
                            java.util.Date currentDate = DateComparator.parseFilingDate(filingDate.trim());
                            if (currentDate != null) {
                                if (maxDate == null || currentDate.after(maxDate)) {
                                    maxDate = currentDate;
                                    maxDateSubmitted = filingDate.trim();
                                }
                            }
                        }
                    }

                    // Extract brochure version ID
                    String brochureVersionId = getFieldValue(record, "brochureVersionId", "BrochureVersionId", "brochure_version_id");
                    if (brochureVersionId != null && !brochureVersionId.trim().isEmpty()) {
                        brochureVersionIds.add(brochureVersionId.trim());
                    }

                } catch (Exception e) {
                    ProcessingLogger.logWarning("Skipping malformed record in baseline file: " + e.getMessage());
                }
            }

            int totalRecords = CsvUtils.countRecordsInFile(baselineFile);

            ProcessingLogger.logInfo("Baseline data loaded successfully:");
            ProcessingLogger.logInfo("  - Source file: " + baselineFile.getFileName());
            ProcessingLogger.logInfo("  - Filing dates: " + filingDates.size() + " firms");
            ProcessingLogger.logInfo("  - Brochure version IDs: " + brochureVersionIds.size());
            ProcessingLogger.logInfo("  - Max date submitted: " + maxDateSubmitted);
            ProcessingLogger.logInfo("  - Total records: " + totalRecords);

            return new BaselineData(baselineFile, filingDates, brochureVersionIds,
                    maxDateSubmitted, totalRecords, true);

        } catch (Exception e) {
            ProcessingLogger.logError("Error reading baseline data from file: " + baselineFile, e);
            return new BaselineData(null, new HashMap<>(), new HashSet<>(), null, 0, false);
        }
    }

    /**
     * Finds the latest IAPD_DATA file in the specified directory
     * @param outputDirectory the directory to search
     * @return Optional containing the latest file, or empty if none found
     */
    public Optional<Path> findLatestOutputFile(Path outputDirectory) {
        ProcessingLogger.logInfo("Searching for latest output file in: " + outputDirectory);

        if (!Files.exists(outputDirectory) || !Files.isDirectory(outputDirectory)) {
            ProcessingLogger.logInfo("Output directory does not exist");
            return Optional.empty();
        }

        try (Stream<Path> files = Files.list(outputDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(Config.OUTPUT_FILE_BASE_NAME))
                    .filter(path -> path.getFileName().toString().endsWith(".csv"))
                    .max((path1, path2) -> {
                        // Extract date from filename for comparison
                        String date1 = extractDateFromFilename(path1.getFileName().toString());
                        String date2 = extractDateFromFilename(path2.getFileName().toString());
                        return date1.compareTo(date2);
                    });
        } catch (Exception e) {
            ProcessingLogger.logError("Error finding latest output file in directory: " + outputDirectory, e);
            return Optional.empty();
        }
    }

    /**
     * Reads baseline data from the latest file in the output directory
     * @param outputDirectory the directory containing IAPD_DATA files
     * @return BaselineData from the latest file, or empty baseline if none found
     */
    public BaselineData readLatestBaselineData(Path outputDirectory) {
        Optional<Path> latestFile = findLatestOutputFile(outputDirectory);

        if (!latestFile.isPresent()) {
            ProcessingLogger.logInfo("No IAPD_DATA files found in output directory - treating as first run");
            return new BaselineData(null, new HashMap<>(), new HashSet<>(), null, 0, false);
        }

        return readBaselineData(latestFile.get());
    }

    /**
     * Validates that a baseline file has the expected structure for incremental processing
     * @param baselineFile the file to validate
     * @return true if the file has valid structure
     */
    public boolean validateFileStructure(Path baselineFile) {
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

            // Check for required columns (at least FirmCrdNb and one date column)
            boolean hasFirmCrdNb = headerRecord.toMap().containsKey("FirmCrdNb");
            boolean hasDateColumn = headerRecord.toMap().containsKey("Filing Date") ||
                                  headerRecord.toMap().containsKey("Filing_Date") ||
                                  headerRecord.toMap().containsKey("filingDate") ||
                                  headerRecord.toMap().containsKey("dateSubmitted") ||
                                  headerRecord.toMap().containsKey("Date Submitted");

            if (!hasFirmCrdNb) {
                ProcessingLogger.logWarning("Baseline file missing required column: FirmCrdNb");
                return false;
            }

            if (!hasDateColumn) {
                ProcessingLogger.logWarning("Baseline file missing date column for incremental processing");
                return false;
            }

            return true;

        } catch (Exception e) {
            ProcessingLogger.logError("Error validating baseline file structure: " + baselineFile, e);
            return false;
        }
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
     * Gets a field value from a CSV record, trying multiple possible column names
     * @param record the CSV record
     * @param fieldNames possible field names to try
     * @return the field value, or null if not found
     */
    private String getFieldValue(CSVRecord record, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                if (record.isMapped(fieldName)) {
                    String value = record.get(fieldName);
                    if (value != null && !value.trim().isEmpty()) {
                        return value;
                    }
                }
            } catch (Exception e) {
                // Try next field name
            }
        }
        return null;
    }

    /**
     * Extracts date string from output filename for comparison
     * Expected format: IAPD_Data_YYYYMMDD.csv
     * @param filename the filename to parse
     * @return date string for comparison, or empty string if not found
     */
    private String extractDateFromFilename(String filename) {
        try {
            String patternString = Config.OUTPUT_FILE_BASE_NAME + "_(\\d{8})\\.csv";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternString);
            java.util.regex.Matcher matcher = pattern.matcher(filename);

            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            ProcessingLogger.logWarning("Could not extract date from filename: " + filename);
        }
        return "";
    }
}
