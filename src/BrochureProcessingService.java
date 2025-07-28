import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
     * @throws BrochureProcessingException if processing fails
     */
    public void processBrochures(String inputFilePath) throws BrochureProcessingException {
        try (Reader reader = Files.newBufferedReader(Paths.get(inputFilePath), StandardCharsets.UTF_8);
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
                processSingleBrochure(csvRecord, writer);
            }
        } catch (Exception e) {
            throw new BrochureProcessingException("Error processing brochures from file: " + inputFilePath, e);
        }
    }
    
    /**
     * Processes downloaded brochures using the default input file
     * @throws BrochureProcessingException if processing fails
     */
    public void processDefaultBrochures() throws BrochureProcessingException {
        String inputFilePath = Config.BROCHURE_INPUT_PATH + "/" + Config.OUTPUT_FILE_NAME;
        processBrochures(inputFilePath);
    }
    
    /**
     * Processes a single brochure record
     */
    private void processSingleBrochure(CSVRecord csvRecord, Writer writer) throws Exception {
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
        
        File brochureFile = new File(Config.BROCHURE_OUTPUT_PATH + "/" + firmCrdNb + "_" + matcher.group(1) + ".pdf");
        if (!brochureFile.exists()) {
            return;
        }
        
        try (FileInputStream stream = new FileInputStream(brochureFile)) {
            String text = PdfTextExtractor.getCleanedBrochureText(stream);
            BrochureAnalysis analysis = brochureAnalyzer.analyzeBrochureContent(text);
            csvWriterService.writeBrochureAnalysis(writer, recordMap, analysis, brochureFile.getName(), brochureURL);
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
            System.out.println("Extracted text length: " + text.length());
            
            BrochureAnalysis analysis = brochureAnalyzer.analyzeBrochureContent(text);
            System.out.println("Proxy Providers: " + analysis.getProxyProvider().toString());
            System.out.println("Class Action Providers: " + analysis.getClassActionProvider().toString());
            System.out.println("ESG Providers: " + analysis.getEsgProvider().toString());
            System.out.println("Email addresses found: " + analysis.getEmailSet().size());
        } catch (Exception e) {
            throw new BrochureProcessingException("Error processing single PDF: " + pdfPath, e);
        }
    }
}
