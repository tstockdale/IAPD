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
    private final IncrementalUpdateManager incrementalUpdateManager;
    private final ResumeStateManager resumeStateManager;
    private final MonthlyDownloadService monthlyDownloadService;
    
    public IAFirmSECParserRefactored() {
        // Initialize services with dependency injection
        this.fileDownloadService = new FileDownloadService();
        this.xmlProcessingService = new XMLProcessingService();
        this.brochureDownloadService = new BrochureDownloadService(fileDownloadService);
        this.configurationManager = new ConfigurationManager();
        this.incrementalUpdateManager = new IncrementalUpdateManager();
        this.resumeStateManager = new ResumeStateManager();
        this.monthlyDownloadService = new MonthlyDownloadService(fileDownloadService);
        
        BrochureAnalyzer brochureAnalyzer = new BrochureAnalyzer();
        CSVWriterService csvWriterService = new CSVWriterService();
        this.brochureProcessingService = new BrochureProcessingService(brochureAnalyzer, csvWriterService);
    }
    
    /**
     * Initializes the logging system early to ensure log files are created in the correct location
     * This must be called before any logging operations occur
     */
    private static void initializeLoggingSystem() {
        try {
            // Set up log path system property early
            String logPath = Config.LOG_PATH.startsWith("./") ? Config.LOG_PATH.substring(2) : Config.LOG_PATH;
            System.setProperty("log.path", logPath);
            
            // Create log directory if it doesn't exist
            String logDir = System.getProperty("user.dir") + File.separator + logPath;
            File logDirectory = new File(logDir);
            if (!logDirectory.exists()) {
                boolean created = logDirectory.mkdirs();
                if (created) {
                    System.out.println("Created log directory: " + logDir);
                } else {
                    System.err.println("Failed to create log directory: " + logDir);
                }
            }
            
            // Force ProcessingLogger initialization to ensure static block runs
            ProcessingLogger.initialize();
            
            System.out.println("Logging system initialized. Log path: " + System.getProperty("log.path"));
            
        } catch (Exception e) {
            System.err.println("Failed to initialize logging system: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Main entry point for the application
     */
    public static void main(String[] args) {
        // Initialize logging system early - MUST be first to ensure log files are created properly
        initializeLoggingSystem();
        
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
            
            // Check if monthly mode is enabled
            if (context.getMonthName() != null && (context.isIncrementalUpdates() || 
                context.isIncrementalDownloads() || context.isIncrementalProcessing())) {
                
                // Monthly mode: Download and extract monthly brochure data
                Path monthlyDataPath = monthlyDownloadService.downloadAndExtractMonthlyData(context.getMonthName(), context);
                if (monthlyDataPath == null) {
                    return; // Error already logged
                }
                
                // Process the monthly brochure data directly
                processMonthlyBrochures(monthlyDataPath, context);
                
            } else {
                // Standard mode: Three-step processing
                
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
            }
            
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
    
    /**
     * Processes monthly brochure data that has been downloaded and extracted
     * @param monthlyDataPath path to the extracted monthly data directory
     * @param context processing context
     */
    private void processMonthlyBrochures(Path monthlyDataPath, ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.PROCESSING_BROCHURES);
            ProcessingLogger.logInfo("=== MONTHLY BROCHURE PROCESSING ===");
            ProcessingLogger.logInfo("Processing monthly brochure data from: " + monthlyDataPath);
            ProcessingLogger.logInfo("Monthly brochure files are now available in: " + monthlyDataPath);
            ProcessingLogger.logInfo("You can now process the brochure files from the extracted directory.");
            
            // For now, we'll just log the completion since the monthly data has been downloaded and extracted
            // The user can then process the brochures from the monthly directory as needed
            ProcessingLogger.logInfo("Monthly download and extraction completed successfully.");
            ProcessingLogger.logInfo("Brochure files are available for processing in: " + monthlyDataPath);
            
        } catch (Exception e) {
            context.setLastError("Error in monthly brochure processing: " + e.getMessage());
            context.setCurrentPhase(ProcessingPhase.ERROR);
            System.err.println("Error in monthly brochure processing: " + e.getMessage());
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
