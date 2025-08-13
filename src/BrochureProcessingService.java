import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.Writer;
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
 * Service class responsible for brochure processing operations
 */
public class BrochureProcessingService {
    
    private final BrochureAnalyzer brochureAnalyzer;
    private final CSVWriterService csvWriterService;
    
    public BrochureProcessingService(BrochureAnalyzer brochureAnalyzer, CSVWriterService csvWriterService) {
        this.brochureAnalyzer = brochureAnalyzer;
        this.csvWriterService = csvWriterService;
    }
    
    /**
     * Processes downloaded brochures and extracts information
     * @param inputFilePath the path to the CSV file containing firm data
     * @param context processing context containing configuration and runtime state
     * @throws BrochureProcessingException if processing fails
     */
    public void processBrochures(Path inputFilePath, ProcessingContext context) throws BrochureProcessingException {
        ProcessingLogger.logInfo("Starting brochure processing from file: " + inputFilePath);
        context.setCurrentProcessingFile(inputFilePath.getFileName().toString());
        
        // Check if resume processing is enabled
        if (context.isResumeProcessing()) {
            processBrochuresWithResume(inputFilePath, context);
        } else {
            processBrochuresStandard(inputFilePath, context);
        }
    }
    
    /**
     * Processes brochures in standard mode (no resume)
     */
    private void processBrochuresStandard(Path inputFilePath, ProcessingContext context) throws BrochureProcessingException {
        try (Reader reader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(Config.BROCHURE_OUTPUT_PATH + "/" + "IAPD_Found.csv"), StandardCharsets.UTF_8)) {
            
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            writer.write(Config.FOUND_FILE_HEADER + System.lineSeparator());
            
            for (CSVRecord csvRecord : records) {
                processSingleBrochure(csvRecord, writer, context);
                
                // Log progress periodically if verbose
                if (context.isVerbose() && context.getBrochuresProcessed() % 50 == 0) {
                    context.logCurrentState();
                }
            }
            
            ProcessingLogger.logInfo("Brochure processing completed. Processed " + context.getBrochuresProcessed() + " brochures.");
            
        } catch (Exception e) {
            context.setLastError("Error processing brochures from file: " + inputFilePath + " - " + e.getMessage());
            throw new BrochureProcessingException("Error processing brochures from file: " + inputFilePath, e);
        }
    }
    
    /**
     * Processes brochures with resume capability
     */
    private void processBrochuresWithResume(Path inputFilePath, ProcessingContext context) throws BrochureProcessingException {
        ResumeStateManager resumeManager = new ResumeStateManager();
        Path outputFilePath = Paths.get(Config.BROCHURE_OUTPUT_PATH + "/" + "IAPD_Found.csv");
        
        // Load existing processed firms if resume file exists
        java.util.Set<String> processedFirms = resumeManager.getProcessedFirms(outputFilePath);
        
        // Calculate resume statistics
        int totalFirms = countRecordsInFile(inputFilePath);
        ResumeStateManager.ResumeStats stats = resumeManager.calculateProcessingResumeStats(totalFirms, processedFirms);
        
        // Log resume statistics
        logProcessingResumeStats(stats, outputFilePath);
        
        try (Reader reader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8, 
                     java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
            
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            // Write header only if file is empty (new file)
            if (processedFirms.isEmpty()) {
                writer.write(Config.FOUND_FILE_HEADER + System.lineSeparator());
            }
            
            int processedCount = 0;
            int skippedCount = 0;
            
            for (CSVRecord csvRecord : records) {
                String firmCrdNb = csvRecord.get("FirmCrdNb");
                
                if (processedFirms.contains(firmCrdNb)) {
                    // Skip processing - already completed
                    skippedCount++;
                } else {
                    // Process brochure
                    processSingleBrochure(csvRecord, writer, context);
                }
                
                processedCount++;
                
                // Log progress periodically if verbose
                if (context.isVerbose() && processedCount % 50 == 0) {
                    ProcessingLogger.logInfo("Processed " + processedCount + " records (" + skippedCount + " skipped, " + 
                            (processedCount - skippedCount) + " analyzed)...");
                    context.logCurrentState();
                }
            }
            
            ProcessingLogger.logInfo("Resume brochure processing completed. Processed " + processedCount + " records.");
            ProcessingLogger.logInfo("Skipped " + skippedCount + " already processed firms.");
            ProcessingLogger.logInfo("Analyzed " + (processedCount - skippedCount) + " new brochures.");
            
        } catch (Exception e) {
            context.setLastError("Error in resume brochure processing from file: " + inputFilePath + " - " + e.getMessage());
            throw new BrochureProcessingException("Error in resume brochure processing from file: " + inputFilePath, e);
        }
    }
    

    
    /**
     * Processes a single brochure record
     */
    private void processSingleBrochure(CSVRecord csvRecord, Writer writer, ProcessingContext context) throws Exception {
        Map<String, String> recordMap = csvRecord.toMap();
        String brochureURL = recordMap.get("BrochureURL");
        String firmCrdNb = recordMap.get("FirmCrdNb");
        
        if (brochureURL == null || brochureURL.isEmpty()) {
            return;
        }
        
        Matcher matcher = PatternMatchers.BRCHR_VERSION_ID_PATTERN.matcher(brochureURL);
        if (!matcher.find()) {
            return;
        }
        
        File parentFolder = new File(Config.DOWNLOAD_PATH);
        String fileName = firmCrdNb + "_" + matcher.group(1) + ".pdf";
        
        File brochureFile = new File(parentFolder, fileName);
        if (!brochureFile.exists()) {
            return;
        }
        
        try (FileInputStream stream = new FileInputStream(brochureFile)) {
            String text = PdfTextExtractor.getCleanedBrochureText(stream);
            BrochureAnalysis analysis = brochureAnalyzer.analyzeBrochureContent(text, firmCrdNb);
            csvWriterService.writeBrochureAnalysis(writer, recordMap, analysis, brochureFile.getName(), brochureURL);
            
            // Update context with successful brochure processing
            context.incrementBrochuresProcessed();
            
        } catch (Exception e) {
            // Log error but continue processing other brochures
            ProcessingLogger.logError("Error processing brochure for firm " + firmCrdNb + ": " + e.getMessage(), e);
            if (context.isVerbose()) {
                context.setLastError("Error processing brochure for firm " + firmCrdNb + ": " + e.getMessage());
            }
        }
    }
    
    
    /**
     * Processes a single PDF brochure for testing purposes
     * @param pdfPath the path to the PDF file to process
     * @throws BrochureProcessingException if processing fails
     */
    public void processOnePDF(String pdfPath) throws BrochureProcessingException {
        File file = new File(pdfPath);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            String text = PdfTextExtractor.getBrochureText(fis);
            ProcessingLogger.logInfo("Extracted text length: " + text.length());
            
            BrochureAnalysis analysis = brochureAnalyzer.analyzeBrochureContent(text, "Test");
            ProcessingLogger.logInfo("Proxy Providers: " + analysis.getProxyProvider().toString());
            ProcessingLogger.logInfo("Class Action Providers: " + analysis.getClassActionProvider().toString());
            ProcessingLogger.logInfo("ESG Providers: " + analysis.getEsgProvider().toString());
            ProcessingLogger.logInfo("Email addresses found: " + analysis.getEmailSet().size());
        } catch (Exception e) {
            throw new BrochureProcessingException("Error processing single PDF: " + pdfPath, e);
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
     * Processes brochures with data merging - processes each record in filesToDownloadWithStatus
     * and outputs all fields from filesToDownloadWithStatus plus brochure analysis fields
     * @param firmDataFile path to original firm data CSV file (not used in current implementation)
     * @param filesToDownloadWithStatus path to FilesToDownload file with download status
     * @param context processing context containing configuration and runtime state
     * @throws BrochureProcessingException if processing fails
     */
    public void processBrochuresWithMerge(Path firmDataFile, Path filesToDownloadWithStatus, ProcessingContext context) throws BrochureProcessingException {
        ProcessingLogger.logInfo("Starting brochure processing with data merge");
        ProcessingLogger.logInfo("Processing records from: " + filesToDownloadWithStatus);
        
        // Create output file path for IAPD_Data
        String outputFileName = "IAPD_Data_" + java.time.LocalDate.now().toString().replace("-", "") + ".csv";
        Path outputFilePath = Paths.get(Config.BROCHURE_OUTPUT_PATH, outputFileName);
        
        try (Reader reader = Files.newBufferedReader(filesToDownloadWithStatus, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8)) {
            
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            // Write header for IAPD_Data (FilesToDownload fields + brochure analysis fields)
            writeIAPDDataHeader(writer);
            
            int processedCount = 0;
            int totalRecords = 0;
            
            for (CSVRecord csvRecord : records) {
                totalRecords++;
                String downloadStatus = csvRecord.get("downloadStatus");
                String fileName = csvRecord.get("fileName");
                
                // Process only successfully downloaded brochures
                if ("SUCCESS".equals(downloadStatus) && fileName != null && !fileName.isEmpty()) {
                    processSingleBrochureFromFilesToDownload(csvRecord, writer, context);
                    processedCount++;
                    
                    // Log progress periodically if verbose
                    if (context.isVerbose() && processedCount % 50 == 0) {
                        ProcessingLogger.logInfo("Processed " + processedCount + " brochures so far...");
                        context.logCurrentState();
                    }
                } else {
                    // Log skipped records if verbose
                    if (context.isVerbose()) {
                        String firmId = csvRecord.get("firmId");
                        ProcessingLogger.logInfo("Skipping firm " + firmId + " - download status: " + downloadStatus + ", fileName: " + fileName);
                    }
                }
            }
            
            ProcessingLogger.logInfo("Brochure processing with merge completed.");
            ProcessingLogger.logInfo("Total records in FilesToDownload: " + totalRecords);
            ProcessingLogger.logInfo("Successfully processed brochures: " + processedCount);
            ProcessingLogger.logInfo("Output file: " + outputFilePath);
            
        } catch (Exception e) {
            context.setLastError("Error in brochure processing with merge: " + e.getMessage());
            throw new BrochureProcessingException("Error in brochure processing with merge", e);
        }
    }
    
    /**
     * Loads firm data from CSV file into a map keyed by FirmCrdNb
     */
    private java.util.Map<String, CSVRecord> loadFirmDataMap(Path firmDataFile) throws Exception {
        java.util.Map<String, CSVRecord> firmDataMap = new java.util.HashMap<>();
        
        try (Reader reader = Files.newBufferedReader(firmDataFile, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                String firmCrdNb = record.get("FirmCrdNb");
                if (firmCrdNb != null && !firmCrdNb.isEmpty()) {
                    firmDataMap.put(firmCrdNb, record);
                }
            }
        }
        
        return firmDataMap;
    }
    
    /**
     * Loads download information from FilesToDownload CSV file into a map
     */
    private java.util.Map<String, DownloadInfo> loadDownloadInfoMap(Path filesToDownloadWithStatus) throws Exception {
        java.util.Map<String, DownloadInfo> downloadInfoMap = new java.util.HashMap<>();
        
        try (Reader reader = Files.newBufferedReader(filesToDownloadWithStatus, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord record : records) {
                String firmId = record.get("firmId");
                String downloadStatus = record.get("downloadStatus");
                String fileName = record.get("fileName");
                String brochureVersionId = record.get("brochureVersionId");
                
                if (firmId != null && !firmId.isEmpty()) {
                    DownloadInfo info = new DownloadInfo(firmId, downloadStatus, fileName, brochureVersionId);
                    downloadInfoMap.put(firmId, info);
                }
            }
        }
        
        return downloadInfoMap;
    }
    
    /**
     * Processes a single brochure with data merging
     */
    private void processSingleBrochureWithMerge(CSVRecord firmRecord, DownloadInfo downloadInfo, Writer writer, ProcessingContext context) throws Exception {
        File brochureFile = new File(Config.DOWNLOAD_PATH, downloadInfo.fileName);
        
        if (!brochureFile.exists()) {
            ProcessingLogger.logWarning("Brochure file not found: " + downloadInfo.fileName);
            return;
        }
        
        try (FileInputStream stream = new FileInputStream(brochureFile)) {
            String text = PdfTextExtractor.getCleanedBrochureText(stream);
            BrochureAnalysis analysis = brochureAnalyzer.analyzeBrochureContent(text, downloadInfo.firmId);
            
            // Write merged data to IAPD_Data format
            writeMergedBrochureAnalysis(writer, firmRecord, analysis, downloadInfo);
            
            // Update context with successful brochure processing
            context.incrementBrochuresProcessed();
            
        } catch (Exception e) {
            // Log error but continue processing other brochures
            ProcessingLogger.logError("Error processing brochure for firm " + downloadInfo.firmId + ": " + e.getMessage(), e);
            if (context.isVerbose()) {
                context.setLastError("Error processing brochure for firm " + downloadInfo.firmId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Writes merged brochure analysis data in IAPD_Data format
     */
    private void writeMergedBrochureAnalysis(Writer writer, CSVRecord firmRecord, BrochureAnalysis analysis, DownloadInfo downloadInfo) throws Exception {
        // Build the IAPD_Data record by combining firm data and brochure analysis
        StringBuilder record = new StringBuilder();
        
        // Add timestamp
        record.append(Config.getCurrentDateString()).append(",");
        
        // Add firm data fields (using correct field names from CSV)
        record.append(csvEscape(firmRecord.get("SECRgmCD"))).append(",");
        record.append(csvEscape(firmRecord.get("FirmCrdNb"))).append(",");
        record.append(csvEscape(firmRecord.get("SECMb"))).append(",");
        record.append(csvEscape(firmRecord.get("Business Name"))).append(",");
        record.append(csvEscape(firmRecord.get("Street 1"))).append(",");
        record.append(csvEscape(firmRecord.get("Street 2"))).append(",");
        record.append(csvEscape(firmRecord.get("City"))).append(",");
        record.append(csvEscape(firmRecord.get("State"))).append(",");
        record.append(csvEscape(firmRecord.get("Country"))).append(",");
        record.append(csvEscape(firmRecord.get("Postal Code"))).append(",");
        record.append(csvEscape(firmRecord.get("Telephone #"))).append(",");
        record.append(csvEscape(firmRecord.get("Filing Date"))).append(",");
        record.append(csvEscape(firmRecord.get("AUM"))).append(",");
        record.append(csvEscape(firmRecord.get("Total Accounts"))).append(",");
        record.append(csvEscape(firmRecord.get("Total Employees"))).append(",");
        
        // Add brochure analysis fields
        record.append(csvEscape(analysis.getProxyProvider().toString())).append(",");
        record.append(csvEscape(analysis.getClassActionProvider().toString())).append(",");
        record.append(csvEscape(analysis.getEsgProvider().toString())).append(",");
        record.append(csvEscape(analysis.getEsgInvestmentLanguage().toString())).append(",");
        record.append(csvEscape(downloadInfo.fileName)).append(",");
        record.append(csvEscape(Config.BROCHURE_URL_BASE + downloadInfo.brochureVersionId)).append(",");
        
        // Add email fields
        record.append(csvEscape(analysis.getEmailComplianceSentence().toString())).append(",");
        record.append(csvEscape(analysis.getEmailProxySentence().toString())).append(",");
        record.append(csvEscape(analysis.getEmailBrochureSentence().toString())).append(",");
        record.append(csvEscape(analysis.getEmailSentence().toString())).append(",");
        record.append(csvEscape(analysis.getFormattedEmailSetString())).append(",");
        record.append(csvEscape(analysis.getNoVoteString().toString()));
        
        writer.write(record.toString());
        writer.write(System.lineSeparator());
    }
    
    /**
     * Writes the header for IAPD_Data CSV file (FilesToDownload fields + brochure analysis fields)
     */
    private void writeIAPDDataHeader(Writer writer) throws Exception {
        StringBuilder header = new StringBuilder();
        
        // Add timestamp column
        header.append("dateAdded,");
        
        // Add all FilesToDownload fields
        header.append("firmId,firmName,brochureVersionId,brochureName,dateSubmitted,dateConfirmed,downloadStatus,fileName,");
        
        // Add brochure analysis fields
        header.append("proxyProvider,classActionProvider,esgProvider,esgInvestmentLanguage,brochureURL,");
        header.append("complianceEmail,proxyEmail,brochureEmail,generalEmail,allEmails,doesNotVote");
        
        writer.write(header.toString());
        writer.write(System.lineSeparator());
    }
    
    /**
     * Processes a single brochure from FilesToDownload record
     */
    private void processSingleBrochureFromFilesToDownload(CSVRecord csvRecord, Writer writer, ProcessingContext context) throws Exception {
        String firmId = csvRecord.get("firmId");
        String fileName = csvRecord.get("fileName");
        String brochureVersionId = csvRecord.get("brochureVersionId");
        
        File brochureFile = new File(Config.DOWNLOAD_PATH, fileName);
        
        if (!brochureFile.exists()) {
            ProcessingLogger.logWarning("Brochure file not found: " + fileName);
            return;
        }
        
        try (FileInputStream stream = new FileInputStream(brochureFile)) {
            String text = PdfTextExtractor.getCleanedBrochureText(stream);
            BrochureAnalysis analysis = brochureAnalyzer.analyzeBrochureContent(text, firmId);
            
            // Write record with all FilesToDownload fields + brochure analysis fields
            writeFilesToDownloadWithAnalysis(writer, csvRecord, analysis, brochureVersionId);
            
            // Update context with successful brochure processing
            context.incrementBrochuresProcessed();
            
        } catch (Exception e) {
            // Log error but continue processing other brochures
            ProcessingLogger.logError("Error processing brochure for firm " + firmId + ": " + e.getMessage(), e);
            if (context.isVerbose()) {
                context.setLastError("Error processing brochure for firm " + firmId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Writes a record with all FilesToDownload fields plus brochure analysis fields
     */
    private void writeFilesToDownloadWithAnalysis(Writer writer, CSVRecord csvRecord, BrochureAnalysis analysis, String brochureVersionId) throws Exception {
        StringBuilder record = new StringBuilder();
        
        // Add timestamp
        record.append(Config.getCurrentDateString()).append(",");
        
        // Add all FilesToDownload fields
        record.append(csvEscape(csvRecord.get("firmId"))).append(",");
        record.append(csvEscape(csvRecord.get("firmName"))).append(",");
        record.append(csvEscape(csvRecord.get("brochureVersionId"))).append(",");
        record.append(csvEscape(csvRecord.get("brochureName"))).append(",");
        record.append(csvEscape(csvRecord.get("dateSubmitted"))).append(",");
        record.append(csvEscape(csvRecord.get("dateConfirmed"))).append(",");
        record.append(csvEscape(csvRecord.get("downloadStatus"))).append(",");
        record.append(csvEscape(csvRecord.get("fileName"))).append(",");
        
        // Add brochure analysis fields
        record.append(csvEscape(analysis.getProxyProvider().toString())).append(",");
        record.append(csvEscape(analysis.getClassActionProvider().toString())).append(",");
        record.append(csvEscape(analysis.getEsgProvider().toString())).append(",");
        record.append(csvEscape(analysis.getEsgInvestmentLanguage().toString())).append(",");
        record.append(csvEscape(Config.BROCHURE_URL_BASE + brochureVersionId)).append(",");
        
        // Add email fields
        record.append(csvEscape(analysis.getEmailComplianceSentence().toString())).append(",");
        record.append(csvEscape(analysis.getEmailProxySentence().toString())).append(",");
        record.append(csvEscape(analysis.getEmailBrochureSentence().toString())).append(",");
        record.append(csvEscape(analysis.getEmailSentence().toString())).append(",");
        record.append(csvEscape(analysis.getFormattedEmailSetString())).append(",");
        record.append(csvEscape(analysis.getNoVoteString().toString()));
        
        writer.write(record.toString());
        writer.write(System.lineSeparator());
    }
    
    /**
     * Helper method to escape CSV values
     */
    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replaceAll("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * Inner class to hold download information
     */
    private static class DownloadInfo {
        final String firmId;
        final String downloadStatus;
        final String fileName;
        final String brochureVersionId;
        
        DownloadInfo(String firmId, String downloadStatus, String fileName, String brochureVersionId) {
            this.firmId = firmId;
            this.downloadStatus = downloadStatus != null ? downloadStatus : "";
            this.fileName = fileName != null ? fileName : "";
            this.brochureVersionId = brochureVersionId != null ? brochureVersionId : "";
        }
    }
    
    /**
     * Logs processing resume statistics in a formatted way
     */
    private void logProcessingResumeStats(ResumeStateManager.ResumeStats stats, Path resumeFile) {
        ProcessingLogger.logInfo("=== RESUME PROCESSING MODE ===");
        ProcessingLogger.logInfo("Resume File: " + resumeFile + " (checking already processed firms)");
        ProcessingLogger.logInfo("Processing Resume Analysis:");
        ProcessingLogger.logInfo("  - Total firms: " + stats.getTotalFirms());
        ProcessingLogger.logInfo("  - Already processed: " + stats.getAlreadyCompleted() + " (skipped)");
        ProcessingLogger.logInfo("  - Remaining to process: " + stats.getRemaining());
    }
}
