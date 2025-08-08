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
