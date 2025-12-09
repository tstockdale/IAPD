package com.iss.iapd.services.brochure;

import java.io.BufferedWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import com.iss.iapd.config.Config;
import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.core.ProcessingContext;
import com.iss.iapd.exceptions.BrochureProcessingException;
import com.iss.iapd.services.download.FileDownloadService;
import com.iss.iapd.utils.PatternMatchers;
import com.iss.iapd.utils.RateLimiter;
import com.iss.iapd.utils.RetryUtils;


/**
 * Service class responsible for downloading brochure PDF files
 * Reads a CSV file with firm data and brochure URLs, downloads the PDFs,
 * and outputs an updated CSV with download status.
 */
public class BrochureDownloadService {
    
    private final FileDownloadService fileDownloadService;
    
    public BrochureDownloadService(FileDownloadService fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }
    
    /**
     * Downloads brochures based on URLs in the input CSV file
     * @param inputFilePath path to CSV file containing firm data with brochure URLs
     * @param context processing context for configuration and state tracking
     * @return path to updated CSV file with download status
     * @throws BrochureProcessingException if downloading fails
     */
    public Path downloadBrochures(Path inputFilePath, ProcessingContext context) throws BrochureProcessingException {
        ProcessingLogger.logInfo("Starting brochure download from file: " + inputFilePath);
        context.setCurrentProcessingFile(inputFilePath.getFileName().toString());
        
        return downloadBrochuresStandard(inputFilePath, context);
    }
    
    /**
     * Downloads brochures in standard mode (no resume)
     */
    private Path downloadBrochuresStandard(Path inputFilePath, ProcessingContext context) throws BrochureProcessingException {
        // Create output file path
        String inputFileName = inputFilePath.getFileName().toString();
        String outputFileName = inputFileName.replace(".csv", "_with_status.csv");
        Path outputFilePath = Paths.get(Config.BROCHURE_INPUT_PATH, outputFileName);
        
        try (Reader reader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8)) {
            
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            // Write header with additional download status column
            writer.write(Config.FIRM_HEADER + ",DownloadStatus" + System.lineSeparator());
            
            int processedCount = 0;
            RateLimiter limiter = RateLimiter.perSecond(context.getDownloadRatePerSecond());
            for (CSVRecord csvRecord : records) {
                String downloadStatus = downloadSingleBrochure(csvRecord, context);
                writeFirmRecordWithDownloadStatus(writer, csvRecord, downloadStatus);
                
                processedCount++;
                
                // Log progress periodically if verbose
                if (context.isVerbose() && processedCount % 100 == 0) {
                    ProcessingLogger.logInfo("Downloaded " + processedCount + " brochures so far...");
                    context.logCurrentState();
                }
                
                // Rate limiting
                limiter.acquire();
            }
            
            ProcessingLogger.logInfo("Brochure download completed. Processed " + processedCount + " records.");
            ProcessingLogger.logInfo("Output file: " + outputFilePath);
            
            return outputFilePath;
            
        } catch (Exception e) {
            context.setLastError("Error downloading brochures from file: " + inputFilePath + " - " + e.getMessage());
            throw new BrochureProcessingException("Error downloading brochures from file: " + inputFilePath, e);
        }
    }
    
   
    
    /**
     * Downloads a single brochure PDF file
     * @param csvRecord the CSV record containing firm data
     * @param context processing context for state tracking
     * @return download status string
     */
    private String downloadSingleBrochure(CSVRecord csvRecord, ProcessingContext context) {
        try {
            Map<String, String> recordMap = csvRecord.toMap();
            String brochureURL = recordMap.get("BrochureURL");
            String firmCrdNb = recordMap.get("FirmCrdNb");
            
            if (brochureURL == null || brochureURL.isEmpty()) {
                return "NO_URL";
            }
            
            // Skip download if configured
            if (context.isSkipBrochureDownload()) {
                return "SKIPPED";
            }
            
            // Extract brochure version ID from URL
            Matcher matcher = PatternMatchers.BRCHR_VERSION_ID_PATTERN.matcher(brochureURL);
            if (!matcher.find()) {
                ProcessingLogger.logWarning("Could not extract version ID from brochure URL for firm: " + firmCrdNb);
                return "INVALID_URL";
            }
            
            String versionId = matcher.group(1);
            String fileName = firmCrdNb + "_" + versionId + ".pdf";
            
            // Use retry logic for downloading
            String downloadResult = RetryUtils.executeWithRetry(() -> {
                try {
                    fileDownloadService.downloadBrochure(brochureURL, fileName);
                    return "SUCCESS";
                } catch (Exception e) {
                    if (RetryUtils.isTransientException(e)) {
                        throw new RuntimeException("Transient error downloading brochure for firm " + firmCrdNb, e);
                    } else {
                        ProcessingLogger.logError("Non-transient error downloading brochure for firm " + firmCrdNb, e);
                        return "FAILED: " + e.getMessage();
                    }
                }
            }, "Download brochure for firm " + firmCrdNb);
            
            if ("SUCCESS".equals(downloadResult)) {
                context.incrementSuccessfulDownloads();
                ProcessingLogger.incrementFirmsWithBrochures();
            } else {
                context.incrementFailedDownloads();
                ProcessingLogger.incrementBrochureDownloadFailures();
            }
            
            return downloadResult;
            
        } catch (Exception e) {
            context.incrementFailedDownloads();
            ProcessingLogger.logError("Error processing brochure download record", e);
            return "ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Writes a firm record with download status to the output CSV
     */
    private void writeFirmRecordWithDownloadStatus(BufferedWriter writer, CSVRecord csvRecord, String downloadStatus) throws Exception {
        // Write timestamp as first column
        writer.write(Config.getCurrentDateString() + ",");
        
        // Write all original columns (skip the first one if it's already dateAdded from input)
        int startIndex = 0;
        if (csvRecord.size() > 0 && csvRecord.get(0) != null && csvRecord.get(0).matches("\\d{2}/\\d{2}/\\d{4}")) {
            startIndex = 1; // Skip existing dateAdded column from input
        }
        
        for (int i = startIndex; i < csvRecord.size(); i++) {
            String value = csvRecord.get(i);
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                writer.write("\"" + value.replaceAll("\"", "\"\"") + "\"");
            } else {
                writer.write(value);
            }
            
            if (i < csvRecord.size() - 1) {
                writer.write(",");
            }
        }
        
        // Add download status column
        writer.write("," + downloadStatus);
        writer.write(System.lineSeparator());
    }
    

    /**
     * Downloads brochures from FilesToDownload with resume capability
     * @param filesToDownloadPath path to FilesToDownload CSV file
     * @param outputFilePath path to existing FilesToDownload_with_status.csv file to append to
     * @param startIndex index to start downloading from (0-based)
     * @param context processing context for configuration and state tracking
     * @return path to updated FilesToDownload file with download status and filename
     * @throws BrochureProcessingException if downloading fails
     */
    public Path downloadBrochuresFromFilesToDownloadWithResume(Path filesToDownloadPath, Path outputFilePath, 
                                                              int startIndex, ProcessingContext context) throws BrochureProcessingException {
        ProcessingLogger.logInfo("Starting resume brochure download from FilesToDownload: " + filesToDownloadPath);
        ProcessingLogger.logInfo("Resuming from index: " + startIndex);
        ProcessingLogger.logInfo("Appending to existing file: " + outputFilePath);
        context.setCurrentProcessingFile(filesToDownloadPath.getFileName().toString());
        
        try (Reader reader = Files.newBufferedReader(filesToDownloadPath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8, 
                     java.nio.file.StandardOpenOption.APPEND)) {
            
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            int currentIndex = 0;
            int processedCount = 0;
            RateLimiter limiter = RateLimiter.perSecond(context.getDownloadRatePerSecond());
            
            for (CSVRecord csvRecord : records) {
                // Skip records until we reach the start index
                if (currentIndex < startIndex) {
                    currentIndex++;
                    continue;
                }
                
                String downloadStatus = "FAILED";
                String fileName = "";
                
                try {
                    String firmId = csvRecord.get("firmId");
                    String brochureVersionId = csvRecord.get("brochureVersionId");
                    
                    if (brochureVersionId != null && !brochureVersionId.isEmpty()) {
                        // Skip download if configured
                        if (context.isSkipBrochureDownload()) {
                            downloadStatus = "SKIPPED";
                        } else {
                            // Construct brochure URL and filename
                            String brochureURL = Config.BROCHURE_URL_BASE + brochureVersionId;
                            final String finalFileName = firmId + "_" + brochureVersionId + ".pdf";
                            
                            // Use retry logic for downloading
                            String downloadResult = RetryUtils.executeWithRetry(() -> {
                                try {
                                    fileDownloadService.downloadBrochure(brochureURL, finalFileName);
                                    return "SUCCESS";
                                } catch (Exception e) {
                                    if (RetryUtils.isTransientException(e)) {
                                        throw new RuntimeException("Transient error downloading brochure for firm " + firmId, e);
                                    } else {
                                        ProcessingLogger.logError("Non-transient error downloading brochure for firm " + firmId, e);
                                        return "FAILED: " + e.getMessage();
                                    }
                                }
                            }, "Download brochure for firm " + firmId);
                            
                            downloadStatus = downloadResult;
                            
                            if ("SUCCESS".equals(downloadResult)) {
                                context.incrementSuccessfulDownloads();
                                ProcessingLogger.incrementBrochuresDownloadedCount();
                                fileName = finalFileName;
                            } else {
                                context.incrementFailedDownloads();
                                ProcessingLogger.incrementBrochureDownloadFailures();
                                fileName = ""; // Clear filename on failure
                            }
                        }
                    } else {
                        downloadStatus = "NO_VERSION_ID";
                    }
                    
                } catch (Exception e) {
                    context.incrementFailedDownloads();
                    ProcessingLogger.logError("Error processing brochure download record", e);
                    downloadStatus = "ERROR: " + e.getMessage();
                    fileName = "";
                }
                
                // Write record with download status and filename (append mode)
                writeFilesToDownloadRecordWithStatus(writer, csvRecord, downloadStatus, fileName);
                processedCount++;
                currentIndex++;
                
                // Log progress periodically if verbose
                if (context.isVerbose() && processedCount % 100 == 0) {
                    ProcessingLogger.logInfo("Downloaded " + processedCount + " brochures so far (index " + currentIndex + ")...");
                    context.logCurrentState();
                }
                
                // Rate limiting (only for actual downloads)
                if (!"SKIPPED".equals(downloadStatus) && !"NO_VERSION_ID".equals(downloadStatus)) {
                    limiter.acquire();
                }
            }
            
            ProcessingLogger.logInfo("Resume brochure download completed. Processed " + processedCount + " new records.");
            ProcessingLogger.logInfo("Output file: " + outputFilePath);
            
            return outputFilePath;
            
        } catch (Exception e) {
            context.setLastError("Error in resume brochure download from FilesToDownload: " + filesToDownloadPath + " - " + e.getMessage());
            throw new BrochureProcessingException("Error in resume brochure download from FilesToDownload: " + filesToDownloadPath, e);
        }
    }
    
    /**
     * Downloads brochures based on FilesToDownload CSV file
     * @param filesToDownloadPath path to FilesToDownload CSV file containing brochure information
     * @param context processing context for configuration and state tracking
     * @return path to updated FilesToDownload file with download status and filename
     * @throws BrochureProcessingException if downloading fails
     */
    public Path downloadBrochuresFromFilesToDownload(Path filesToDownloadPath, ProcessingContext context) throws BrochureProcessingException {
        ProcessingLogger.logInfo("Starting brochure download from FilesToDownload: " + filesToDownloadPath);
        context.setCurrentProcessingFile(filesToDownloadPath.getFileName().toString());
        
        // Create output file path
        String inputFileName = filesToDownloadPath.getFileName().toString();
        String outputFileName = inputFileName.replace(".csv", "_with_status.csv");
        Path outputFilePath = Paths.get(Config.BROCHURE_INPUT_PATH, outputFileName);
        
        try (Reader reader = Files.newBufferedReader(filesToDownloadPath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8)) {
            
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            // Write header with additional columns for download status and filename
            writer.write("firmId,firmName,brochureVersionId,brochureName,dateSubmitted,dateConfirmed,downloadStatus,fileName" + System.lineSeparator());
            
            int processedCount = 0;
            RateLimiter limiter = RateLimiter.perSecond(context.getDownloadRatePerSecond());
            
            for (CSVRecord csvRecord : records) {
                String downloadStatus = "FAILED";
                String fileName = "";
                
                try {
                    String firmId = csvRecord.get("firmId");
                    String brochureVersionId = csvRecord.get("brochureVersionId");
                    
                    if (brochureVersionId != null && !brochureVersionId.isEmpty()) {
                        // Skip download if configured
                        if (context.isSkipBrochureDownload()) {
                            downloadStatus = "SKIPPED";
                        } else {
                            // Construct brochure URL and filename
                            String brochureURL = Config.BROCHURE_URL_BASE + brochureVersionId;
                            final String finalFileName = firmId + "_" + brochureVersionId + ".pdf";
                            
                            // Use retry logic for downloading
                            String downloadResult = RetryUtils.executeWithRetry(() -> {
                                try {
                                    fileDownloadService.downloadBrochure(brochureURL, finalFileName);
                                    return "SUCCESS";
                                } catch (Exception e) {
                                    if (RetryUtils.isTransientException(e)) {
                                        throw new RuntimeException("Transient error downloading brochure for firm " + firmId, e);
                                    } else {
                                        ProcessingLogger.logError("Non-transient error downloading brochure for firm " + firmId, e);
                                        return "FAILED: " + e.getMessage();
                                    }
                                }
                            }, "Download brochure for firm " + firmId);
                            
                            downloadStatus = downloadResult;
                            
                            if ("SUCCESS".equals(downloadResult)) {
                                context.incrementSuccessfulDownloads();
                                ProcessingLogger.incrementBrochuresDownloadedCount();
                                fileName = finalFileName;
                            } else {
                                context.incrementFailedDownloads();
                                ProcessingLogger.incrementBrochureDownloadFailures();
                                fileName = ""; // Clear filename on failure
                            }
                        }
                    } else {
                        downloadStatus = "NO_VERSION_ID";
                    }
                    
                } catch (Exception e) {
                    context.incrementFailedDownloads();
                    ProcessingLogger.logError("Error processing brochure download record", e);
                    downloadStatus = "ERROR: " + e.getMessage();
                    fileName = "";
                }
                
                // Write record with download status and filename
                writeFilesToDownloadRecordWithStatus(writer, csvRecord, downloadStatus, fileName);
                processedCount++;
                
                // Log progress periodically if verbose
                if (context.isVerbose() && processedCount % 100 == 0) {
                    ProcessingLogger.logInfo("Downloaded " + processedCount + " brochures so far...");
                    context.logCurrentState();
                }
                
                // Rate limiting (only for actual downloads)
                if (!"SKIPPED".equals(downloadStatus) && !"NO_VERSION_ID".equals(downloadStatus)) {
                    limiter.acquire();
                }
            }
            
            ProcessingLogger.logInfo("Brochure download from FilesToDownload completed. Processed " + processedCount + " records.");
            ProcessingLogger.logInfo("Output file: " + outputFilePath);
            
            return outputFilePath;
            
        } catch (Exception e) {
            context.setLastError("Error downloading brochures from FilesToDownload: " + filesToDownloadPath + " - " + e.getMessage());
            throw new BrochureProcessingException("Error downloading brochures from FilesToDownload: " + filesToDownloadPath, e);
        }
    }
    
    /**
     * Writes a FilesToDownload record with download status and filename to the output CSV
     */
    private void writeFilesToDownloadRecordWithStatus(BufferedWriter writer, CSVRecord csvRecord, String downloadStatus, String fileName) throws Exception {
        // Write all original columns
        for (int i = 0; i < csvRecord.size(); i++) {
            String value = csvRecord.get(i);
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                writer.write("\"" + value.replaceAll("\"", "\"\"") + "\"");
            } else {
                writer.write(value);
            }
            writer.write(",");
        }
        
        // Add download status and filename columns
        writer.write(downloadStatus + "," + fileName);
        writer.write(System.lineSeparator());
    }

}
