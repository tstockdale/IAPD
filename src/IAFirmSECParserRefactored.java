import java.io.File;
import java.nio.file.Path;

/**
 * Refactored Investment Adviser Public Disclosure (IAPD) Parser
 * 
 * This application parses SEC IAPD XML feed files and processes PDF brochures
 * to extract relevant information about investment advisory firms.
 * 
 * Refactored version with improved architecture using ProcessingContext pattern.
 */
public class IAFirmSECParserRefactored {
    
    private final XMLProcessingService xmlProcessingService;
    private final BrochureDownloadService brochureDownloadService;
    private final BrochureProcessingService brochureProcessingService;
    private final FileDownloadService fileDownloadService;
    private final ConfigurationManager configurationManager;
    
    public IAFirmSECParserRefactored() {
        // Initialize services with dependency injection
        this.fileDownloadService = new FileDownloadService();
        this.xmlProcessingService = new XMLProcessingService();
        this.brochureDownloadService = new BrochureDownloadService(fileDownloadService);
        this.configurationManager = new ConfigurationManager();
        
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
            // Build processing context from all configuration sources
            ProcessingContext context = parser.configurationManager.buildContext(args);
            
            // Show help if requested (check command line directly for immediate response)
            try {
                CommandLineOptions options = CommandLineOptions.parseArgs(args);
                if (options.isShowHelp()) {
                    CommandLineOptions.printUsage();
                    return;
                }
            } catch (IllegalArgumentException e) {
                // Continue with context-based processing
            }
            
            // Validate configuration
            if (!parser.configurationManager.validateConfiguration(context)) {
                System.err.println("Configuration validation failed. Exiting.");
                System.exit(1);
            }
            
            // Log startup information
            ProcessingLogger.logInfo("Working Directory = " + System.getProperty("user.dir"));
            parser.configurationManager.printEffectiveConfiguration(context);
            
            // Start processing with three distinct steps
            context.setCurrentPhase(ProcessingPhase.INITIALIZATION);
            parser.processIAPDDataInSteps(context);
            
            // Mark completion
            context.setCurrentPhase(ProcessingPhase.COMPLETED);
            context.logCurrentState();
            
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            CommandLineOptions.printUsage();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error in main execution: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Processes IAPD data in three distinct steps:
     * 1. Download and parse XML to extract firm data with brochure URLs
     * 2. Download brochure PDF files based on URLs from step 1
     * 3. Process and analyze the downloaded brochures
     * @param context processing context containing configuration and runtime state
     */
    public void processIAPDDataInSteps(ProcessingContext context) {
        try {
            _setUpDirectories();
            
            // Step 1: Download XML and extract firm data with brochure URLs
            Path firmDataFile = downloadAndParseXMLData(context);
            if (firmDataFile == null) {
                return; // Error already logged
            }
            
            // Step 2: Download brochure PDF files
            Path firmDataWithDownloads = downloadBrochures(firmDataFile, context);
            if (firmDataWithDownloads == null) {
                return; // Error already logged
            }
            
            // Step 3: Process and analyze brochures
            processBrochures(firmDataWithDownloads, context);
            
        } catch (Exception e) {
            context.setLastError("Error in IAPD data processing: " + e.getMessage());
            context.setCurrentPhase(ProcessingPhase.ERROR);
            System.err.println("Error in IAPD data processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Step 1: Downloads XML data and extracts firm information with brochure URLs
     * @param context processing context
     * @return path to CSV file with firm data and brochure URLs, or null if failed
     */
    private Path downloadAndParseXMLData(ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.DOWNLOADING_XML);
            ProcessingLogger.logInfo("=== STEP 1: Downloading and parsing XML data ===");
            ProcessingLogger.logInfo("Downloading latest IAPD data from SEC website...");
            
            // Download the latest ZIP file
            File zipFile = fileDownloadService.downloadLatestIAPDData(Config.FIRM_FILE_PATH);
            context.incrementSuccessfulDownloads();
            
            // Extract the ZIP file
            File extractedFile = fileDownloadService.extractGZFile(zipFile, Config.FIRM_FILE_PATH);
            
            if (extractedFile != null && extractedFile.exists()) {
                ProcessingLogger.logInfo("Successfully downloaded and extracted IAPD data: " + extractedFile.getName());
                ProcessingLogger.logInfo("IAPD data file location: " + extractedFile.getAbsolutePath());
                
                context.setCurrentProcessingFile(extractedFile.getName());
                context.setCurrentPhase(ProcessingPhase.PARSING_XML);
                
                // Process the XML file and get the output file path (with brochure URLs, no downloads yet)
                Path outputFilePath = xmlProcessingService.processXMLFile(extractedFile, context);
                
                if (outputFilePath != null) {
                    ProcessingLogger.logInfo("XML processing completed. Firm data file: " + outputFilePath);
                    return outputFilePath;
                } else {
                    context.setLastError("Failed to process XML file");
                    context.setCurrentPhase(ProcessingPhase.ERROR);
                    System.err.println("Failed to process XML file");
                    return null;
                }
            } else {
                context.setLastError("Failed to extract IAPD data file");
                context.setCurrentPhase(ProcessingPhase.ERROR);
                context.incrementFailedDownloads();
                System.err.println("Failed to extract IAPD data file");
                return null;
            }
            
        } catch (Exception e) {
            context.setLastError("Error in XML processing step: " + e.getMessage());
            context.setCurrentPhase(ProcessingPhase.ERROR);
            System.err.println("Error in XML processing step: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Step 2: Downloads brochure PDF files based on URLs in the firm data CSV
     * @param firmDataFile path to CSV file with firm data and brochure URLs
     * @param context processing context
     * @return path to updated CSV file with download status, or null if failed
     */
    private Path downloadBrochures(Path firmDataFile, ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.DOWNLOADING_BROCHURES);
            ProcessingLogger.logInfo("=== STEP 2: Downloading brochure PDF files ===");
            
            Path outputFilePath = brochureDownloadService.downloadBrochures(firmDataFile, context);
            
            if (outputFilePath != null) {
                ProcessingLogger.logInfo("Brochure download completed. Updated firm data file: " + outputFilePath);
                return outputFilePath;
            } else {
                context.setLastError("Failed to download brochures");
                context.setCurrentPhase(ProcessingPhase.ERROR);
                System.err.println("Failed to download brochures");
                return null;
            }
            
        } catch (Exception e) {
            context.setLastError("Error in brochure download step: " + e.getMessage());
            context.setCurrentPhase(ProcessingPhase.ERROR);
            System.err.println("Error in brochure download step: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Step 3: Processes and analyzes the downloaded brochures
     * @param firmDataWithDownloads path to CSV file with firm data and download status
     * @param context processing context
     */
    private void processBrochures(Path firmDataWithDownloads, ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.PROCESSING_BROCHURES);
            ProcessingLogger.logInfo("=== STEP 3: Processing and analyzing brochures ===");
            
            brochureProcessingService.processBrochures(firmDataWithDownloads, context);
            
            ProcessingLogger.logInfo("Brochure processing completed successfully.");
            
        } catch (Exception e) {
            context.setLastError("Error in brochure processing step: " + e.getMessage());
            context.setCurrentPhase(ProcessingPhase.ERROR);
            System.err.println("Error in brochure processing step: " + e.getMessage());
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
