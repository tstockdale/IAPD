package com.iss.iapd.services.incremental;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import com.iss.iapd.config.Config;
import com.iss.iapd.config.ProcessingLogger;

/**
 * Service for reading output data files to determine incremental processing parameters
 * Specifically designed to find the maximum dateSubmitted from existing IAPD_DATA files
 */
public class OutputDataReaderService {
    
    private static final String DATE_FORMAT = "MM/dd/yyyy";
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
    
    /**
     * Container for output data analysis results
     */
    public static class OutputDataAnalysis {
        private final Path latestFile;
        private final String maxDateSubmitted;
        private final Date maxDateSubmittedParsed;
        private final Set<String> existingBrochureVersionIds;
        private final int totalRecords;
        private final boolean hasExistingData;
        
        public OutputDataAnalysis(Path latestFile, String maxDateSubmitted, 
                                Date maxDateSubmittedParsed, Set<String> existingBrochureVersionIds,
                                int totalRecords, boolean hasExistingData) {
            this.latestFile = latestFile;
            this.maxDateSubmitted = maxDateSubmitted;
            this.maxDateSubmittedParsed = maxDateSubmittedParsed;
            this.existingBrochureVersionIds = existingBrochureVersionIds != null ? existingBrochureVersionIds : new HashSet<>();
            this.totalRecords = totalRecords;
            this.hasExistingData = hasExistingData;
        }
        
        public Path getLatestFile() { return latestFile; }
        public String getMaxDateSubmitted() { return maxDateSubmitted; }
        public Date getMaxDateSubmittedParsed() { return maxDateSubmittedParsed; }
        public Set<String> getExistingBrochureVersionIds() { return existingBrochureVersionIds; }
        public int getTotalRecords() { return totalRecords; }
        public boolean hasExistingData() { return hasExistingData; }
        
        @Override
        public String toString() {
            return String.format("OutputDataAnalysis{latestFile=%s, maxDateSubmitted=%s, brochureVersionIds=%d, totalRecords=%d, hasExistingData=%s}",
                    latestFile, maxDateSubmitted, existingBrochureVersionIds.size(), totalRecords, hasExistingData);
        }
    }
    
    /**
     * Analyzes the output directory to find the latest IAPD_DATA file and extract brochure version IDs
     * 
     * @param outputDirectory the directory containing IAPD_DATA files
     * @return OutputDataAnalysis containing the analysis results
     */
    public OutputDataAnalysis analyzeOutputDirectory(Path outputDirectory) {
        ProcessingLogger.logInfo("Analyzing output directory for incremental processing: " + outputDirectory);
        
        if (!Files.exists(outputDirectory) || !Files.isDirectory(outputDirectory)) {
            ProcessingLogger.logInfo("Output directory does not exist - treating as first run");
            return new OutputDataAnalysis(null, null, null, new HashSet<>(), 0, false);
        }
        
        Optional<Path> latestFile = findLatestOutputFile(outputDirectory);
        
        if (!latestFile.isPresent()) {
            ProcessingLogger.logInfo("No IAPD_DATA files found in output directory - treating as first run");
            return new OutputDataAnalysis(null, null, null, new HashSet<>(), 0, false);
        }
        
        Path file = latestFile.get();
        ProcessingLogger.logInfo("Found latest output file: " + file.getFileName());
        
        String maxDateSubmitted = getMaxDateSubmitted(file);
        Date maxDateSubmittedParsed = parseDate(maxDateSubmitted);
        Set<String> existingBrochureVersionIds = getBrochureVersionIds(file);
        int totalRecords = countRecords(file);
        
        ProcessingLogger.logInfo("Output data analysis complete:");
        ProcessingLogger.logInfo("  - Latest file: " + file.getFileName());
        ProcessingLogger.logInfo("  - Max dateSubmitted: " + maxDateSubmitted);
        ProcessingLogger.logInfo("  - Existing brochure version IDs: " + existingBrochureVersionIds.size());
        ProcessingLogger.logInfo("  - Total records: " + totalRecords);
        
        return new OutputDataAnalysis(file, maxDateSubmitted, maxDateSubmittedParsed, existingBrochureVersionIds, totalRecords, true);
    }
    
    /**
     * Finds the latest IAPD_DATA file in the output directory based on filename date
     * 
     * @param outputDirectory the directory to search
     * @return Optional containing the latest file, or empty if none found
     */
    public Optional<Path> findLatestOutputFile(Path outputDirectory) {
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
     * Extracts the maximum dateSubmitted value from an IAPD_DATA CSV file
     * 
     * @param outputFile the CSV file to analyze
     * @return the maximum dateSubmitted value as a string, or null if none found
     */
    public String getMaxDateSubmitted(Path outputFile) {
        if (!Files.exists(outputFile)) {
            ProcessingLogger.logWarning("Output file does not exist: " + outputFile);
            return null;
        }
        
        String maxDateSubmitted = null;
        Date maxDate = null;
        int recordsProcessed = 0;
        int validDates = 0;
        
        try (Reader reader = Files.newBufferedReader(outputFile, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                recordsProcessed++;
                try {
                    // Look for dateSubmitted column - it might be in different positions
                    String dateSubmitted = null;
                    
                    // Try common column names for dateSubmitted
                    if (record.isMapped("dateSubmitted")) {
                        dateSubmitted = record.get("dateSubmitted");
                    } else if (record.isMapped("Date Submitted")) {
                        dateSubmitted = record.get("Date Submitted");
                    } else if (record.isMapped("Filing Date")) {
                        dateSubmitted = record.get("Filing Date");
                    }
                    
                    if (dateSubmitted != null && !dateSubmitted.trim().isEmpty()) {
                        Date currentDate = parseDate(dateSubmitted.trim());
                        if (currentDate != null) {
                            validDates++;
                            if (maxDate == null || currentDate.after(maxDate)) {
                                maxDate = currentDate;
                                maxDateSubmitted = dateSubmitted.trim();
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip malformed records
                    ProcessingLogger.logWarning("Skipping malformed record in output file: " + e.getMessage());
                }
            }
            
            ProcessingLogger.logInfo("Processed " + recordsProcessed + " records from output file");
            ProcessingLogger.logInfo("Found " + validDates + " valid dateSubmitted values");
            ProcessingLogger.logInfo("Maximum dateSubmitted: " + maxDateSubmitted);
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error reading maximum dateSubmitted from output file: " + outputFile, e);
        }
        
        return maxDateSubmitted;
    }
    
    /**
     * Extracts all brochureVersionIds from an IAPD_DATA CSV file
     * 
     * @param outputFile the CSV file to analyze
     * @return Set of brochureVersionIds found in the file
     */
    public Set<String> getBrochureVersionIds(Path outputFile) {
        Set<String> brochureVersionIds = new HashSet<>();
        
        if (!Files.exists(outputFile)) {
            ProcessingLogger.logWarning("Output file does not exist: " + outputFile);
            return brochureVersionIds;
        }
        
        int recordsProcessed = 0;
        int validVersionIds = 0;
        
        try (Reader reader = Files.newBufferedReader(outputFile, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                recordsProcessed++;
                try {
                    // Look for brochureVersionId column - it might be in different positions
                    String brochureVersionId = null;
                    
                    // Try common column names for brochureVersionId
                    if (record.isMapped("brochureVersionId")) {
                        brochureVersionId = record.get("brochureVersionId");
                    } else if (record.isMapped("BrochureVersionId")) {
                        brochureVersionId = record.get("BrochureVersionId");
                    } else if (record.isMapped("brochure_version_id")) {
                        brochureVersionId = record.get("brochure_version_id");
                    }
                    
                    if (brochureVersionId != null && !brochureVersionId.trim().isEmpty()) {
                        brochureVersionIds.add(brochureVersionId.trim());
                        validVersionIds++;
                    }
                } catch (Exception e) {
                    // Skip malformed records
                    ProcessingLogger.logWarning("Skipping malformed record in output file: " + e.getMessage());
                }
            }
            
            ProcessingLogger.logInfo("Processed " + recordsProcessed + " records from output file");
            ProcessingLogger.logInfo("Found " + validVersionIds + " valid brochureVersionId values");
            ProcessingLogger.logInfo("Unique brochureVersionIds: " + brochureVersionIds.size());
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error reading brochureVersionIds from output file: " + outputFile, e);
        }
        
        return brochureVersionIds;
    }
    
    /**
     * Parses a date string in MM/dd/yyyy format
     * 
     * @param dateString the date string to parse
     * @return parsed Date object, or null if parsing fails
     */
    public Date parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return dateFormatter.parse(dateString.trim());
        } catch (ParseException e) {
            ProcessingLogger.logWarning("Failed to parse date: " + dateString + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Counts the total number of records in a CSV file
     * 
     * @param csvFile the file to count
     * @return the number of records (excluding header)
     */
    private int countRecords(Path csvFile) {
        int count = 0;
        try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                count++;
            }
        } catch (Exception e) {
            ProcessingLogger.logError("Error counting records in file: " + csvFile, e);
        }
        return count;
    }
    
    /**
     * Extracts date string from output filename for comparison
     * Expected format: IAPD_Data_YYYYMMDD.csv
     * 
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
    
    /**
     * Checks if a dateSubmitted is more recent than the maximum found in existing data
     * 
     * @param dateSubmitted the date to check
     * @param maxDateSubmitted the maximum date from existing data
     * @return true if dateSubmitted is more recent, false otherwise
     */
    public boolean isDateMoreRecent(String dateSubmitted, String maxDateSubmitted) {
        if (maxDateSubmitted == null || maxDateSubmitted.trim().isEmpty()) {
            return true; // No existing data, so any date is "more recent"
        }
        
        if (dateSubmitted == null || dateSubmitted.trim().isEmpty()) {
            return false; // No date to compare
        }
        
        Date currentDate = parseDate(dateSubmitted);
        Date maxDate = parseDate(maxDateSubmitted);
        
        if (currentDate == null || maxDate == null) {
            ProcessingLogger.logWarning("Date parsing failed - including conservatively. Current: " + 
                    dateSubmitted + ", Max: " + maxDateSubmitted);
            return true; // Conservative approach - include if we can't parse
        }
        
        return currentDate.after(maxDate);
    }
    
    /**
     * Validates that an output file has the expected structure for incremental processing
     * 
     * @param outputFile the file to validate
     * @return true if the file has valid structure
     */
    public boolean validateOutputFileStructure(Path outputFile) {
        if (!Files.exists(outputFile)) {
            return false;
        }
        
        try (Reader reader = Files.newBufferedReader(outputFile, StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(false)
                    .build();
            
            Iterable<CSVRecord> records = format.parse(reader);
            CSVRecord headerRecord = records.iterator().next();
            
            // Check for at least one date column
            boolean hasDateColumn = headerRecord.toMap().containsKey("dateSubmitted") ||
                                  headerRecord.toMap().containsKey("Date Submitted") ||
                                  headerRecord.toMap().containsKey("Filing Date");
            
            if (!hasDateColumn) {
                ProcessingLogger.logWarning("Output file missing date column for incremental processing");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error validating output file structure: " + outputFile, e);
            return false;
        }
    }
}
