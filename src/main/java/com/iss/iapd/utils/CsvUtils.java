package com.iss.iapd.utils;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import com.iss.iapd.config.ProcessingLogger;

/**
 * Utility class for CSV file operations.
 * Provides common CSV processing functions used across the application.
 */
public class CsvUtils {
    
    /**
     * Counts the number of records in a CSV file (excluding header)
     * 
     * @param csvFile the path to the CSV file to count records from
     * @return the number of records in the file, or 0 if an error occurs
     */
    public static int countRecordsInFile(Path csvFile) {
        try (Reader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            long count = java.util.stream.StreamSupport.stream(records.spliterator(), false).count();
            return (int) count;
        } catch (Exception e) {
            ProcessingLogger.logError("Error counting records in file: " + csvFile, e);
            return 0;
        }
    }
    
    // Private constructor to prevent instantiation
    private CsvUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
