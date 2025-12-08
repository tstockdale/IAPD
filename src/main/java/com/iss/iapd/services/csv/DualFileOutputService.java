package com.iss.iapd.services.csv;

import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import com.iss.iapd.config.ProcessingLogger;

/**
 * Service class responsible for managing dual file output strategy:
 * - Creates dated files (IAPD_Data_YYYYMMDD.csv) for each execution
 * - Maintains a master file (IAPD_Data.csv) with cumulative data
 */
public class DualFileOutputService {
    
    private static final String MASTER_FILE_NAME = "IAPD_Data.csv";
    
    /**
     * Processes the dual file output strategy after brochure processing is complete
     * @param datedFilePath path to the dated file created during processing
     * @return path to the master file, or null if processing failed
     */
    public Path processDualFileOutput(Path datedFilePath) {
        try {
            ProcessingLogger.logInfo("=== DUAL FILE OUTPUT PROCESSING ===");
            ProcessingLogger.logInfo("Processing dated file: " + datedFilePath);
            
            // Determine master file path (same directory as dated file)
            Path masterFilePath = datedFilePath.getParent().resolve(MASTER_FILE_NAME);
            ProcessingLogger.logInfo("Master file path: " + masterFilePath);
            
            if (!Files.exists(masterFilePath)) {
                // Master file doesn't exist - create it and copy all records from dated file
                createMasterFileFromDatedFile(datedFilePath, masterFilePath);
            } else {
                // Master file exists - append only new records
                appendNewRecordsToMasterFile(datedFilePath, masterFilePath);
            }
            
            ProcessingLogger.logInfo("Dual file output processing completed successfully.");
            return masterFilePath;
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error in dual file output processing: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Creates master file by copying all records from the dated file
     */
    private void createMasterFileFromDatedFile(Path datedFilePath, Path masterFilePath) throws Exception {
        ProcessingLogger.logInfo("Master file does not exist. Creating new master file...");
        
        // Simply copy the dated file to create the master file
        Files.copy(datedFilePath, masterFilePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Count records for logging
        int recordCount = com.iss.iapd.utils.CsvUtils.countRecordsInFile(datedFilePath);
        
        ProcessingLogger.logInfo("Created master file: " + masterFilePath);
        ProcessingLogger.logInfo("Copied " + recordCount + " records from dated file to master file.");
    }
    
    /**
     * Appends only new records from dated file to master file (avoiding duplicates)
     */
    private void appendNewRecordsToMasterFile(Path datedFilePath, Path masterFilePath) throws Exception {
        ProcessingLogger.logInfo("Master file exists. Appending new records...");
        
        // Read existing brochureVersionIds from master file
        Set<String> existingVersionIds = getBrochureVersionIdsFromFile(masterFilePath);
        ProcessingLogger.logInfo("Found " + existingVersionIds.size() + " existing records in master file.");
        
        // Read records from dated file and append new ones
        int newRecordsAdded = 0;
        int duplicatesSkipped = 0;
        
        try (Reader reader = Files.newBufferedReader(datedFilePath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(masterFilePath, StandardCharsets.UTF_8, 
                     java.nio.file.StandardOpenOption.APPEND)) {
            
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                String brochureVersionId = record.get("brochureVersionId");
                
                if (brochureVersionId != null && !brochureVersionId.isEmpty()) {
                    if (!existingVersionIds.contains(brochureVersionId)) {
                        // New record - append to master file
                        writeRecordToFile(writer, record);
                        newRecordsAdded++;
                    } else {
                        // Duplicate record - skip
                        duplicatesSkipped++;
                    }
                } else {
                    // Record without brochureVersionId - append anyway (shouldn't happen in normal processing)
                    writeRecordToFile(writer, record);
                    newRecordsAdded++;
                }
            }
        }
        
        ProcessingLogger.logInfo("Appended " + newRecordsAdded + " new records to master file.");
        ProcessingLogger.logInfo("Skipped " + duplicatesSkipped + " duplicate records.");
    }
    
    /**
     * Extracts brochureVersionIds from a CSV file
     */
    private Set<String> getBrochureVersionIdsFromFile(Path csvFile) throws Exception {
        Set<String> versionIds = new HashSet<>();
        
        try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                String brochureVersionId = record.get("brochureVersionId");
                if (brochureVersionId != null && !brochureVersionId.isEmpty()) {
                    versionIds.add(brochureVersionId);
                }
            }
        }
        
        return versionIds;
    }
    
    /**
     * Writes a CSV record to a writer
     */
    private void writeRecordToFile(BufferedWriter writer, CSVRecord record) throws Exception {
        StringBuilder line = new StringBuilder();
        
        for (int i = 0; i < record.size(); i++) {
            if (i > 0) {
                line.append(",");
            }
            String value = record.get(i);
            if (value != null && (value.contains(",") || value.contains("\"") || value.contains("\n"))) {
                line.append("\"").append(value.replaceAll("\"", "\"\"")).append("\"");
            } else {
                line.append(value != null ? value : "");
            }
        }
        
        writer.write(line.toString());
        writer.write(System.lineSeparator());
    }
    
}
