import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private final BrochureURLExtractionService brochureURLExtractionService;
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
        this.brochureURLExtractionService = new BrochureURLExtractionService();
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
            Path logDir = Paths.get(System.getProperty("user.dir")).resolve(logPath);
            try {
                Files.createDirectories(logDir);
                System.out.println("Created log directory: " + logDir);
            } catch (Exception e) {
                System.err.println("Failed to create log directory: " + logDir + " - " + e.getMessage());
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
     * Processes IAPD data in four distinct steps:
     * 1. Download and parse XML to extract firm data (without brochure URLs)
     * 2. Extract brochure URLs from FIRM API and create FilesToDownload
     * 3. Download brochure PDF files based on FilesToDownload
     * 4. Process and analyze the downloaded brochures, merge data and save as IAPD_Data
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
                // Standard mode: Four-step processing
                
                // Step 1: Download XML and extract firm data (without brochure URLs)
                Path firmDataFile = downloadAndParseXMLData(context);
                if (firmDataFile == null) {
                    return; // Error already logged
                }
                
                // Step 2: Extract brochure URLs and create FilesToDownload
                Path filesToDownload = extractBrochureURLs(firmDataFile, context);
                if (filesToDownload == null) {
                    return; // Error already logged
                }
                
                // Step 3: Download brochure PDF files
                Path filesToDownloadWithStatus = downloadBrochures(filesToDownload, context);
                if (filesToDownloadWithStatus == null) {
                    return; // Error already logged
                }
                
                // Step 4: Process and analyze brochures, merge data and save as IAPD_Data
                processBrochures(firmDataFile, filesToDownloadWithStatus, context);
            }
            
        } catch (Exception e) {
            context.setLastError("Error in IAPD data processing: " + e.getMessage());
            context.setCurrentPhase(ProcessingPhase.ERROR);
            System.err.println("Error in IAPD data processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Step 1: Downloads XML data and extracts firm information (without brochure URLs)
     * @param context processing context
     * @return path to CSV file with firm data (without brochure URLs), or null if failed
     */
    private Path downloadAndParseXMLData(ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.DOWNLOADING_XML);
            ProcessingLogger.logInfo("=== STEP 1: Downloading and parsing XML data ===");
            ProcessingLogger.logInfo("Downloading latest IAPD data from SEC website...");
            
            // Download the latest ZIP file (FileDownloadService still returns File, so convert to Path)
            java.io.File zipFileObj = fileDownloadService.downloadLatestIAPDData(Config.FIRM_FILE_PATH);
            Path zipFile = zipFileObj.toPath();
            context.incrementSuccessfulDownloads();
            
            // Extract the ZIP file
            java.io.File extractedFileObj = fileDownloadService.extractGZFile(zipFileObj, Config.FIRM_FILE_PATH);
            Path extractedFile = extractedFileObj != null ? extractedFileObj.toPath() : null;
            
            if (extractedFile != null && Files.exists(extractedFile)) {
                ProcessingLogger.logInfo("Successfully downloaded and extracted IAPD data: " + extractedFile.getFileName());
                ProcessingLogger.logInfo("IAPD data file location: " + extractedFile.toAbsolutePath());
                
                context.setCurrentProcessingFile(extractedFile.getFileName().toString());
                context.setCurrentPhase(ProcessingPhase.PARSING_XML);
                
                // Process the XML file and get the output file path (without brochure URLs)
                Path outputFilePath = xmlProcessingService.processXMLFile(extractedFile.toFile(), context);
                
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
     * Step 2: Extracts brochure URLs from FIRM API and creates FilesToDownload
     * @param firmDataFile path to CSV file with firm data (without brochure URLs)
     * @param context processing context
     * @return path to FilesToDownload CSV file, or null if failed
     */
    private Path extractBrochureURLs(Path firmDataFile, ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.EXTRACTING_BROCHURE_URLS);
            ProcessingLogger.logInfo("=== STEP 2: Extracting brochure URLs from FIRM API ===");
            
            Path outputFilePath = brochureURLExtractionService.processFirmDataForBrochures(firmDataFile.toFile(), context);
            
            if (outputFilePath != null) {
                ProcessingLogger.logInfo("Brochure URL extraction completed. FilesToDownload file: " + outputFilePath);
                return outputFilePath;
            } else {
                context.setLastError("Failed to extract brochure URLs");
                context.setCurrentPhase(ProcessingPhase.ERROR);
                System.err.println("Failed to extract brochure URLs");
                return null;
            }
            
        } catch (Exception e) {
            context.setLastError("Error in brochure URL extraction step: " + e.getMessage());
            context.setCurrentPhase(ProcessingPhase.ERROR);
            System.err.println("Error in brochure URL extraction step: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Step 3: Downloads brochure PDF files based on FilesToDownload
     * @param filesToDownload path to FilesToDownload CSV file
     * @param context processing context
     * @return path to updated FilesToDownload file with download status, or null if failed
     */
    private Path downloadBrochures(Path filesToDownload, ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.DOWNLOADING_BROCHURES);
            ProcessingLogger.logInfo("=== STEP 3: Downloading brochure PDF files ===");
            
            Path outputFilePath = brochureDownloadService.downloadBrochuresFromFilesToDownload(filesToDownload, context);
            
            if (outputFilePath != null) {
                ProcessingLogger.logInfo("Brochure download completed. Updated FilesToDownload file: " + outputFilePath);
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
     * Step 4: Processes and analyzes the downloaded brochures, merges data and saves as IAPD_Data
     * @param firmDataFile path to original firm data CSV file
     * @param filesToDownloadWithStatus path to FilesToDownload file with download status
     * @param context processing context
     */
    private void processBrochures(Path firmDataFile, Path filesToDownloadWithStatus, ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.PROCESSING_BROCHURES);
            ProcessingLogger.logInfo("=== STEP 4: Processing and analyzing brochures ===");
            
            brochureProcessingService.processBrochuresWithMerge(firmDataFile, filesToDownloadWithStatus, context);
            
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
        try {
            Path parentFolder = Paths.get(path);
            Files.createDirectories(parentFolder);
        } catch (Exception e) {
            System.err.println("Failed to create directory: " + path + " - " + e.getMessage());
        }
    }
 
}
