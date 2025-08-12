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
        
        // Check if resume downloads is enabled
        if (context.isResumeDownloads()) {
            return downloadBrochuresWithResume(inputFilePath, context);
        } else {
            return downloadBrochuresStandard(inputFilePath, context);
        }
    }
    
    /**
     * Downloads brochures in standard mode (no resume)
     */
    private Path downloadBrochuresStandard(Path inputFilePath, ProcessingContext context) throws BrochureProcessingException {
        // Create output file path
        String inputFileName = inputFilePath.getFileName().toString();
        String outputFileName = inputFileName.replace(".csv", "_with_downloads.csv");
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
                Thread.sleep(1000);
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
     * Downloads brochures with resume capability
     */
    private Path downloadBrochuresWithResume(Path inputFilePath, ProcessingContext context) throws BrochureProcessingException {
        ResumeStateManager resumeManager = new ResumeStateManager();
        
        // Create output file path
        String inputFileName = inputFilePath.getFileName().toString();
        String outputFileName = inputFileName.replace(".csv", "_with_downloads.csv");
        Path outputFilePath = Paths.get(Config.BROCHURE_INPUT_PATH, outputFileName);
        
        // Load existing download status if resume file exists
        Map<String, String> existingStatus = resumeManager.getDownloadStatus(outputFilePath);
        
        // Calculate resume statistics
        int totalFirms = countRecordsInFile(inputFilePath);
        ResumeStateManager.ResumeStats stats = resumeManager.calculateDownloadResumeStats(totalFirms, existingStatus, true);
        
        // Log resume statistics
        logResumeStats(stats, outputFilePath);
        
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
            int skippedCount = 0;
            
            for (CSVRecord csvRecord : records) {
                String firmCrdNb = csvRecord.get("FirmCrdNb");
                String existingDownloadStatus = existingStatus.get(firmCrdNb);
                
                String downloadStatus;
                if (existingDownloadStatus != null && !resumeManager.shouldRetryDownload(existingDownloadStatus)) {
                    // Skip download - already completed successfully
                    downloadStatus = existingDownloadStatus;
                    skippedCount++;
                } else {
                    // Download or retry download
                    downloadStatus = downloadSingleBrochure(csvRecord, context);
                }
                
                writeFirmRecordWithDownloadStatus(writer, csvRecord, downloadStatus);
                processedCount++;
                
                // Log progress periodically if verbose
                if (context.isVerbose() && processedCount % 100 == 0) {
                    ProcessingLogger.logInfo("Processed " + processedCount + " records (" + skippedCount + " skipped, " + 
                            (processedCount - skippedCount) + " downloaded)...");
                    context.logCurrentState();
                }
                
                // Rate limiting (only for actual downloads)
                if (existingDownloadStatus == null || resumeManager.shouldRetryDownload(existingDownloadStatus)) {
                    Thread.sleep(1000);
                }
            }
            
            ProcessingLogger.logInfo("Resume brochure download completed. Processed " + processedCount + " records.");
            ProcessingLogger.logInfo("Skipped " + skippedCount + " already completed downloads.");
            ProcessingLogger.logInfo("Downloaded " + (processedCount - skippedCount) + " new/retry downloads.");
            ProcessingLogger.logInfo("Output file: " + outputFilePath);
            
            return outputFilePath;
            
        } catch (Exception e) {
            context.setLastError("Error in resume brochure download from file: " + inputFilePath + " - " + e.getMessage());
            throw new BrochureProcessingException("Error in resume brochure download from file: " + inputFilePath, e);
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
     * Gets brochure URL for a firm without downloading (used by XML processing)
     * @param firmCrdNb the firm CRD number
     * @param context processing context
     * @return brochure URL or null if not found
     */
    public String getBrochureURL(String firmCrdNb, ProcessingContext context) {
        try {
            String url = String.format(Config.FIRM_API_URL_FORMAT, firmCrdNb);
            String response = HttpUtils.getHTTPSResponse(url);
            
            if (response != null) {
                Matcher matcher = PatternMatchers.API_BRCHR_VERSION_ID_PATTERN.matcher(response);
                if (matcher.find()) {
                    return Config.BROCHURE_URL_BASE + matcher.group(1);
                } else {
                    ProcessingLogger.logWarning("No brochure version ID found in API response for firm: " + firmCrdNb);
                    return null;
                }
            } else {
                ProcessingLogger.logWarning("No response received from API for firm: " + firmCrdNb);
                return null;
            }
        } catch (Exception e) {
            ProcessingLogger.logError("Error getting brochure URL for firm " + firmCrdNb, e);
            return null;
        }
    }
    
    /**
     * Counts the number of records in a CSV file
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
            
            for (CSVRecord record : records) {
                count++;
            }
        } catch (Exception e) {
            ProcessingLogger.logError("Error counting records in file: " + csvFile, e);
        }
        return count;
    }
    
    /**
     * Logs resume statistics in a formatted way
     */
    private void logResumeStats(ResumeStateManager.ResumeStats stats, Path resumeFile) {
        ProcessingLogger.logInfo("=== RESUME DOWNLOAD MODE ===");
        ProcessingLogger.logInfo("Resume File: " + resumeFile + " (checking existing downloads)");
        ProcessingLogger.logInfo("Download Resume Analysis:");
        ProcessingLogger.logInfo("  - Total firms: " + stats.getTotalFirms());
        ProcessingLogger.logInfo("  - Already completed: " + stats.getAlreadyCompleted() + " (skipped)");
        ProcessingLogger.logInfo("  - Failed/retry needed: " + stats.getFailed() + " (will retry)");
        ProcessingLogger.logInfo("  - Corrupted files: " + stats.getCorrupted() + " (will re-download)");
        ProcessingLogger.logInfo("  - Remaining to download: " + stats.getRemaining());
    }
}
