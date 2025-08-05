import java.io.File;
import java.nio.file.Path;

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
        	ProcessingLogger.logInfo("Working Directory = " + System.getProperty("user.dir"));
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
            ProcessingLogger.logInfo("Downloading latest IAPD data from SEC website...");
            
            _setUpDirectories();
            
            // Download the latest ZIP file
            File zipFile = fileDownloadService.downloadLatestIAPDData(Config.FIRM_FILE_PATH);
            
            // Extract the ZIP file
            File extractedFile = fileDownloadService.extractGZFile(zipFile, Config.FIRM_FILE_PATH);
            
            if (extractedFile != null && extractedFile.exists()) {
                ProcessingLogger.logInfo("Successfully downloaded and extracted IAPD data: " + extractedFile.getName());
                ProcessingLogger.logInfo("IAPD data file location: " + extractedFile.getAbsolutePath());
                
                // Process the XML file and get the output file path
                Path outputFilePath = xmlProcessingService.processXMLFile(extractedFile);
                
                if (outputFilePath != null) {
                    ProcessingLogger.logInfo("XML processing completed. Output file: " + outputFilePath);
                    
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
    
    private void _setUpDirectories() {   
        _checkOrMakeDirs(Config.FIRM_FILE_PATH);
        _checkOrMakeDirs(Config.BROCHURE_OUTPUT_PATH);
        _checkOrMakeDirs(Config.BROCHURE_INPUT_PATH);
        _checkOrMakeDirs(Config.DOWNLOAD_PATH);
        _checkOrMakeDirs(Config.LOG_PATH);
    }
    
    private void _checkOrMakeDirs(String path) {
    	File parentFolder = new File(path);
        if (!parentFolder.exists()) {
            parentFolder.mkdirs();
        } 	
    }
 
}
