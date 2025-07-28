import java.io.File;

/**
 * Refactored Investment Adviser Public Disclosure (IAPD) Parser
 * 
 * This application parses SEC IAPD XML feed files and processes PDF brochures
 * to extract relevant information about investment advisory firms.
 * 
 * Refactored version with improved architecture using service layer pattern.
 */
public class IAFirmSECParserRefactored {
    
    private final XMLProcessingService xmlProcessingService;
    private final BrochureProcessingService brochureProcessingService;
    private final FileDownloadService fileDownloadService;
    
    public IAFirmSECParserRefactored() {
        // Initialize services with dependency injection
        this.fileDownloadService = new FileDownloadService();
        this.xmlProcessingService = new XMLProcessingService(fileDownloadService);
        
        BrochureAnalyzer brochureAnalyzer = new BrochureAnalyzer();
        CSVWriterService csvWriterService = new CSVWriterService();
        this.brochureProcessingService = new BrochureProcessingService(brochureAnalyzer, csvWriterService);
    }
    
    /**
     * Main entry point for the application
     */
    public static void main(String[] args) {
        IAFirmSECParserRefactored parser = new IAFirmSECParserRefactored();
        try {
            parser.downloadAndProcessLatestIAPDData();
        } catch (Exception e) {
            System.err.println("Error in main execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Downloads the latest IAPD data file and processes it
     */
    public void downloadAndProcessLatestIAPDData() {
        try {
            System.out.println("Downloading latest IAPD data from SEC website...");
            
            // Download the latest ZIP file
            File zipFile = fileDownloadService.downloadLatestIAPDData(Config.OUTPUT_FILE_PATH);
            
            // Extract the ZIP file
            File extractedFile = fileDownloadService.extractGZFile(zipFile, Config.OUTPUT_FILE_PATH);
            
            if (extractedFile != null && extractedFile.exists()) {
                System.out.println("Successfully downloaded and extracted IAPD data: " + extractedFile.getName());
                System.out.println("IAPD data file location: " + extractedFile.getAbsolutePath());
                
                // Process the XML file and get the output file path
                String outputFilePath = xmlProcessingService.processXMLFile(extractedFile);
                
                if (outputFilePath != null) {
                    System.out.println("XML processing completed. Output file: " + outputFilePath);
                    
                    // Process brochures using the output from XML processing
                    brochureProcessingService.processBrochures(outputFilePath);
                } else {
                    System.err.println("Failed to process XML file");
                }
            } else {
                System.err.println("Failed to extract IAPD data file");
            }
            
        } catch (Exception e) {
            System.err.println("Error downloading and processing IAPD data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
 

    /**
     * Processes the XML file containing firm data using the default input file
     */
    public void processXMLFile() {
        try {
            String outputFilePath = xmlProcessingService.processDefaultXMLFile();
            if (outputFilePath != null) {
                System.out.println("XML processing completed. Output file: " + outputFilePath);
            } else {
                System.err.println("Failed to process XML file");
            }
        } catch (XMLProcessingException e) {
            System.err.println("Error processing XML file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Processes downloaded brochures and extracts information using the default input file
     */
    public void processBrochures() {
        try {
            brochureProcessingService.processDefaultBrochures();
            System.out.println("Brochure processing completed.");
        } catch (BrochureProcessingException e) {
            System.err.println("Error processing brochures: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Processes downloaded brochures and extracts information
     * @param inputFilePath the path to the CSV file containing firm data
     */
    public void processBrochures(String inputFilePath) {
        try {
            brochureProcessingService.processBrochures(inputFilePath);
            System.out.println("Brochure processing completed for file: " + inputFilePath);
        } catch (BrochureProcessingException e) {
            System.err.println("Error processing brochures: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    /**
     * Processes a single PDF brochure for testing purposes
     */
    public void processOnePDF() {
        String path = "C:/Users/stoctom/Work/IAPD/Output/283630_902136.pdf";
        try {
            brochureProcessingService.processOnePDF(path);
        } catch (BrochureProcessingException e) {
            System.err.println("Error processing single PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Processes a single PDF brochure for testing purposes
     * @param pdfPath the path to the PDF file to process
     */
    public void processOnePDF(String pdfPath) {
        try {
            brochureProcessingService.processOnePDF(pdfPath);
        } catch (BrochureProcessingException e) {
            System.err.println("Error processing single PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
