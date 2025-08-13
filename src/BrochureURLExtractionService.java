import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.QuoteMode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service class responsible for extracting brochure URLs from FIRM_API JSON responses
 * and creating FilesToDownload output file
 */
public class BrochureURLExtractionService {
    
    private static final String FILES_TO_DOWNLOAD_HEADER = "firmId,firmName,brochureVersionId,brochureName,dateSubmitted,dateConfirmed";
    private final ObjectMapper objectMapper;
    
    public BrochureURLExtractionService() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Processes a CSV file containing firm data (without brochureURL field) and 
     * extracts brochure information from FIRM_API JSON responses
     * 
     * @param inputCsvFile the CSV file containing firm data from parseXML
     * @param context processing context containing configuration and runtime state
     * @return the Path of the FilesToDownload output file
     * @throws BrochureProcessingException if processing fails
     */
    public Path processFirmDataForBrochures(File inputCsvFile, ProcessingContext context) throws BrochureProcessingException {
        ProcessingLogger.logInfo("Starting brochure URL extraction for file: " + inputCsvFile.getName());
        
        // Initialize processing statistics
        ProcessingStats stats = new ProcessingStats();
        long startTime = System.currentTimeMillis();
        
        String outputFileName = constructFilesToDownloadFileName(inputCsvFile.getName());
        Path outputFilePath = Paths.get(Config.BROCHURE_INPUT_PATH, outputFileName);
        
        List<BrochureDownloadRecord> downloadRecords = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(inputCsvFile);
             CSVParser parser = CSVFormat.EXCEL
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(new java.io.InputStreamReader(fis, StandardCharsets.UTF_8))) {
            
            RateLimiter limiter = RateLimiter.perSecond(context.getURLRatePerSecond());
            
            for (CSVRecord record : parser) {
                stats.totalFirmsInFile++;
                
               /*  if (context.hasReachedIndexLimit()) {
                    ProcessingLogger.logInfo("Reached index limit. Stopping brochure URL extraction.");
                    break;
                } */
                
                String firmCrdNb = record.get("FirmCrdNb");
                String firmName = record.get("Business Name");
                
                if (firmCrdNb != null && !firmCrdNb.trim().isEmpty()) {
                    stats.firmsProcessed++;
                    List<BrochureDownloadRecord> firmBrochures = extractBrochuresFromFirmAPI(firmCrdNb, firmName, context);
                    
                    if (firmBrochures.isEmpty()) {
                        stats.firmsWithNoBrochures++;
                    } else {
                        stats.firmsWithBrochures++;
                        stats.totalBrochuresFound += firmBrochures.size();
                        
                        // Track brochures per firm statistics
                        if (firmBrochures.size() == 1) {
                            stats.firmsWithOneBrochure++;
                        } else if (firmBrochures.size() <= 5) {
                            stats.firmsWithMultipleBrochures++;
                        } else {
                            stats.firmsWithManyBrochures++;
                        }
                    }
                    
                    downloadRecords.addAll(firmBrochures);
                    context.incrementProcessedFirms();
                    
                    // Log progress periodically if verbose
                    if (context.isVerbose() && stats.firmsProcessed % 100 == 0) {
                        logProgressStats(stats, downloadRecords.size());
                    }
                    
                    // Rate limiting
                    limiter.acquire();
                } else {
                    stats.firmsSkipped++;
                }
            }
            
        } catch (Exception e) {
            stats.processingErrors++;
            context.setLastError("Error reading input CSV file: " + inputCsvFile.getName() + " - " + e.getMessage());
            ProcessingLogger.logError("Error reading input CSV file: " + inputCsvFile.getName(), e);
            throw new BrochureProcessingException("Error reading input CSV file: " + inputCsvFile.getName(), e);
        }
        
        // Write FilesToDownload output file
        try {
            writeFilesToDownloadFile(outputFilePath, downloadRecords);
            
            // Calculate final statistics
            long endTime = System.currentTimeMillis();
            stats.processingTimeMs = endTime - startTime;
            
            // Log comprehensive final statistics
            logFinalStats(stats, downloadRecords.size(), outputFilePath);
            
            return outputFilePath;
            
        } catch (Exception e) {
            stats.processingErrors++;
            context.setLastError("Error writing FilesToDownload file: " + outputFilePath + " - " + e.getMessage());
            ProcessingLogger.logError("Error writing FilesToDownload file: " + outputFilePath, e);
            throw new BrochureProcessingException("Error writing FilesToDownload file: " + outputFilePath, e);
        }
    }
    
    /**
     * Extracts brochure information from FIRM_API JSON response for a single firm
     * Uses retry logic for network resilience
     */
    private List<BrochureDownloadRecord> extractBrochuresFromFirmAPI(String firmCrdNb, String firmName, ProcessingContext context) {
        List<BrochureDownloadRecord> brochures = new ArrayList<>();
        
        try {
            String url = String.format(Config.FIRM_API_URL_FORMAT, firmCrdNb);
            
            // Use retry logic for API calls
            String jsonResponse = RetryUtils.executeWithRetry(() -> {
                try {
                    String response = HttpUtils.getHTTPSResponse(url);
                    if (response == null || response.trim().isEmpty()) {
                        throw new RuntimeException("Empty or null response from FIRM_API for firm " + firmCrdNb);
                    }
                    return response;
                } catch (Exception e) {
                    if (RetryUtils.isTransientException(e)) {
                        throw new RuntimeException("Transient error calling FIRM_API for firm " + firmCrdNb, e);
                    } else {
                        ProcessingLogger.logError("Non-transient error calling FIRM_API for firm " + firmCrdNb, e);
                        throw new RuntimeException("Non-transient error calling FIRM_API for firm " + firmCrdNb, e);
                    }
                }
            }, "Extract brochures from FIRM_API for firm " + firmCrdNb);
            
            if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {
                brochures = parseFirmAPIResponse(jsonResponse, firmCrdNb, firmName);
            } else {
                ProcessingLogger.logWarning("Empty or null response from FIRM_API for firm: " + firmCrdNb);
            }
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error extracting brochures from FIRM_API for firm " + firmCrdNb, e);
        }
        
        return brochures;
    }
    
    /**
     * Parses the FIRM_API JSON response and extracts brochure information
     * Uses Jackson ObjectMapper for robust JSON parsing
     */
    private List<BrochureDownloadRecord> parseFirmAPIResponse(String jsonResponse, String firmCrdNb, String firmName) {
        List<BrochureDownloadRecord> brochures = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            // Navigate through the JSON structure: hits.hits[0]._source.iacontent
            JsonNode hitsNode = rootNode.path("hits");
            JsonNode hitsArrayNode = hitsNode.path("hits");
            
            if (hitsArrayNode.isArray() && hitsArrayNode.size() > 0) {
                JsonNode firstHit = hitsArrayNode.get(0);
                JsonNode sourceNode = firstHit.path("_source");
                String iaContentString = sourceNode.path("iacontent").asText();
                
                if (!iaContentString.isEmpty()) {
                    // Parse the nested JSON string
                    JsonNode iaContentNode = objectMapper.readTree(iaContentString);
                    
                    // Extract firmId and firmName from basicInformation
                    JsonNode basicInfoNode = iaContentNode.path("basicInformation");
                    String firmId = basicInfoNode.path("firmId").asText();
                    String apiFirmName = basicInfoNode.path("firmName").asText();
                    
                    // Use API firm name if available, otherwise use the one from CSV
                    String finalFirmName = !apiFirmName.isEmpty() ? apiFirmName : firmName;
                    String finalFirmId = !firmId.isEmpty() ? firmId : firmCrdNb;
                    
                    // Extract brochure details
                    JsonNode brochuresNode = iaContentNode.path("brochures");
                    JsonNode brochureDetailsNode = brochuresNode.path("brochuredetails");
                    
                    if (brochureDetailsNode.isArray()) {
                        for (JsonNode brochureNode : brochureDetailsNode) {
                            String brochureVersionId = brochureNode.path("brochureVersionID").asText();
                            String brochureName = brochureNode.path("brochureName").asText();
                            String dateSubmitted = brochureNode.path("dateSubmitted").asText();
                            String lastConfirmed = brochureNode.path("lastConfirmed").asText();
                            
                            // Only create record if we have the essential fields
                            if (!brochureVersionId.isEmpty() && !brochureName.isEmpty() && !dateSubmitted.isEmpty()) {
                                BrochureDownloadRecord record = new BrochureDownloadRecord(
                                    finalFirmId,
                                    finalFirmName,
                                    brochureVersionId,
                                    brochureName,
                                    dateSubmitted,
                                    lastConfirmed
                                );
                                brochures.add(record);
                            }
                        }
                    }
                }
            }
            
            if (brochures.isEmpty()) {
                ProcessingLogger.logWarning("No brochure details found in API response for firm: " + firmCrdNb);
            } else {
                ProcessingLogger.logInfo("Found " + brochures.size() + " brochures for firm: " + firmCrdNb);
            }
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error parsing FIRM_API JSON response for firm " + firmCrdNb, e);
        }
        
        return brochures;
    }
    
    /**
     * Writes the FilesToDownload CSV file
     */
    private void writeFilesToDownloadFile(Path outputFilePath, List<BrochureDownloadRecord> records) throws Exception {
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(outputFilePath.toFile(), false), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(osw, CSVFormat.EXCEL
                     .builder()
                     .setQuoteMode(QuoteMode.MINIMAL)
                     .setRecordSeparator(System.lineSeparator())
                     .build())) {
            
            // Write header
            String[] headers = FILES_TO_DOWNLOAD_HEADER.split(",");
            printer.printRecord((Object[]) headers);
            
            // Write records
            for (BrochureDownloadRecord record : records) {
                printer.printRecord(
                    record.getFirmId(),
                    sanitizeValue(record.getFirmName()),
                    record.getBrochureVersionId(),
                    sanitizeValue(record.getBrochureName()),
                    record.getDateSubmitted(),
                    record.getDateConfirmed()
                );
            }
        }
    }
    
    /**
     * Constructs the FilesToDownload output file name based on the input file name
     */
    private String constructFilesToDownloadFileName(String inputFileName) {
        try {
            // Extract date from input file name (e.g., "IA_FIRM_SEC_DATA_20250407.csv")
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("IA_FIRM_SEC_DATA_(\\d{8})\\.csv");
            java.util.regex.Matcher matcher = pattern.matcher(inputFileName);
            
            if (matcher.find()) {
                String dateString = matcher.group(1);
                return "FilesToDownload_" + dateString + ".csv";
            } else {
                ProcessingLogger.logWarning("Could not parse date from input file name: " + inputFileName + ". Using timestamp-based name.");
                return "FilesToDownload_" + System.currentTimeMillis() + ".csv";
            }
        } catch (Exception e) {
            ProcessingLogger.logError("Error constructing FilesToDownload file name for: " + inputFileName, e);
            return "FilesToDownload_" + System.currentTimeMillis() + ".csv";
        }
    }
    
    /**
     * Logs progress statistics during processing
     */
    private void logProgressStats(ProcessingStats stats, int totalBrochures) {
        ProcessingLogger.logInfo("=== BROCHURE URL EXTRACTION PROGRESS ===");
        ProcessingLogger.logInfo("Firms processed: " + stats.firmsProcessed + " / " + stats.totalFirmsInFile);
        ProcessingLogger.logInfo("Firms with brochures: " + stats.firmsWithBrochures);
        ProcessingLogger.logInfo("Firms with no brochures: " + stats.firmsWithNoBrochures);
        ProcessingLogger.logInfo("Total brochures found: " + totalBrochures);
        ProcessingLogger.logInfo("Average brochures per firm: " + 
            (stats.firmsWithBrochures > 0 ? String.format("%.2f", (double) totalBrochures / stats.firmsWithBrochures) : "0"));
    }
    
    /**
     * Logs comprehensive final statistics
     */
    private void logFinalStats(ProcessingStats stats, int totalBrochures, Path outputFile) {
        ProcessingLogger.logInfo("=== BROCHURE URL EXTRACTION COMPLETED ===");
        ProcessingLogger.logInfo("Input Statistics:");
        ProcessingLogger.logInfo("  - Total firms in file: " + stats.totalFirmsInFile);
        ProcessingLogger.logInfo("  - Firms processed: " + stats.firmsProcessed);
        ProcessingLogger.logInfo("  - Firms skipped (empty CRD): " + stats.firmsSkipped);
        
        ProcessingLogger.logInfo("Brochure Statistics:");
        ProcessingLogger.logInfo("  - Firms with brochures: " + stats.firmsWithBrochures);
        ProcessingLogger.logInfo("  - Firms with no brochures: " + stats.firmsWithNoBrochures);
        ProcessingLogger.logInfo("  - Total brochures found: " + totalBrochures);
        
        ProcessingLogger.logInfo("Brochure Distribution:");
        ProcessingLogger.logInfo("  - Firms with 1 brochure: " + stats.firmsWithOneBrochure);
        ProcessingLogger.logInfo("  - Firms with 2-5 brochures: " + stats.firmsWithMultipleBrochures);
        ProcessingLogger.logInfo("  - Firms with 6+ brochures: " + stats.firmsWithManyBrochures);
        
        ProcessingLogger.logInfo("Performance Statistics:");
        ProcessingLogger.logInfo("  - Processing time: " + formatDuration(stats.processingTimeMs));
        ProcessingLogger.logInfo("  - Average time per firm: " + 
            (stats.firmsProcessed > 0 ? String.format("%.2f ms", (double) stats.processingTimeMs / stats.firmsProcessed) : "0 ms"));
        ProcessingLogger.logInfo("  - Firms per second: " + 
            (stats.processingTimeMs > 0 ? String.format("%.2f", (double) stats.firmsProcessed * 1000 / stats.processingTimeMs) : "0"));
        
        if (stats.processingErrors > 0) {
            ProcessingLogger.logWarning("Processing errors encountered: " + stats.processingErrors);
        }
        
        ProcessingLogger.logInfo("Output file: " + outputFile);
        ProcessingLogger.logInfo("Success rate: " + String.format("%.1f%%", 
            stats.firmsProcessed > 0 ? (double) stats.firmsWithBrochures * 100 / stats.firmsProcessed : 0));
    }
    
    /**
     * Formats duration in milliseconds to human-readable format
     */
    private String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + " ms";
        } else if (durationMs < 60000) {
            return String.format("%.2f seconds", durationMs / 1000.0);
        } else {
            long minutes = durationMs / 60000;
            long seconds = (durationMs % 60000) / 1000;
            return minutes + "m " + seconds + "s";
        }
    }
    
    /**
     * Helper method to sanitize string values for CSV output
     */
    private String sanitizeValue(String value) {
        return value != null ? value.replaceAll("\"", "") : "";
    }
    
    /**
     * Inner class to track processing statistics
     */
    private static class ProcessingStats {
        int totalFirmsInFile = 0;
        int firmsProcessed = 0;
        int firmsSkipped = 0;
        int firmsWithBrochures = 0;
        int firmsWithNoBrochures = 0;
        int firmsWithOneBrochure = 0;
        int firmsWithMultipleBrochures = 0;
        int firmsWithManyBrochures = 0;
        int totalBrochuresFound = 0;
        int processingErrors = 0;
        long processingTimeMs = 0;
    }
    
    /**
     * Data class to hold brochure download record information
     */
    public static class BrochureDownloadRecord {
        private final String firmId;
        private final String firmName;
        private final String brochureVersionId;
        private final String brochureName;
        private final String dateSubmitted;
        private final String dateConfirmed;
        
        public BrochureDownloadRecord(String firmId, String firmName, String brochureVersionId, 
                                    String brochureName, String dateSubmitted, String dateConfirmed) {
            this.firmId = firmId;
            this.firmName = firmName;
            this.brochureVersionId = brochureVersionId;
            this.brochureName = brochureName;
            this.dateSubmitted = dateSubmitted;
            this.dateConfirmed = dateConfirmed != null && !dateConfirmed.isEmpty() ? dateConfirmed : "";
        }
        
        public String getFirmId() { return firmId; }
        public String getFirmName() { return firmName; }
        public String getBrochureVersionId() { return brochureVersionId; }
        public String getBrochureName() { return brochureName; }
        public String getDateSubmitted() { return dateSubmitted; }
        public String getDateConfirmed() { return dateConfirmed; }
    }
}
