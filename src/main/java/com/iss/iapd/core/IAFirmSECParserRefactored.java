package com.iss.iapd.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.iss.iapd.config.CommandLineOptions;
import com.iss.iapd.config.Config;
import com.iss.iapd.config.ConfigurationManager;
import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.model.ProcessingPhase;
import com.iss.iapd.services.brochure.BrochureDownloadService;
import com.iss.iapd.services.brochure.BrochureProcessingService;
import com.iss.iapd.services.brochure.BrochureURLExtractionService;
import com.iss.iapd.services.download.FileDownloadService;
import com.iss.iapd.services.xml.XMLProcessingService;

/**
 * Refactored Investment Adviser Public Disclosure (IAPD) Parser
 * 
 * This application parses SEC IAPD XML feed files and processes PDF brochures
 * to extract relevant information about investment advisory firms.
 * 
 * Refactored version with improved architecture using ProcessingContext pattern.
 */
public class IAFirmSECParserRefactored {
    
    @com.google.inject.Inject
    private XMLProcessingService xmlProcessingService;
    @com.google.inject.Inject
    private BrochureURLExtractionService brochureURLExtractionService;
    @com.google.inject.Inject
    private BrochureDownloadService brochureDownloadService;
    @com.google.inject.Inject
    private BrochureProcessingService brochureProcessingService;
    @com.google.inject.Inject
    private FileDownloadService fileDownloadService;
    @com.google.inject.Inject
    private ConfigurationManager configurationManager;
    
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

        com.google.inject.Injector injector = com.google.inject.Guice.createInjector(new com.iss.iapd.config.IapdModule());
        IAFirmSECParserRefactored parser = injector.getInstance(IAFirmSECParserRefactored.class);

        try {
            // Build processing context from all configuration sources
            ProcessingContext context = parser.configurationManager.buildContext(args);

            // Check if help was requested from the built context
            if (context.isShowHelp()) {
                CommandLineOptions.printUsage();
                return;
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
     * Handles force restart by renaming the existing Data directory with a timestamp
     * @param context processing context containing configuration
     */
    private void handleForceRestart(ProcessingContext context) {
        if (!context.isForceRestart()) {
            return; // Force restart not requested
        }
        
        try {
            Path dataDirectory = Paths.get("./Data");
            
            if (!Files.exists(dataDirectory)) {
                ProcessingLogger.logInfo("Force restart requested, but Data directory does not exist. Proceeding with fresh start.");
                return;
            }
            
            // Generate timestamp for backup directory name
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = now.format(formatter);
            String backupDirName = "Data_" + timestamp;
            Path backupDirectory = Paths.get(backupDirName);
            
            // Rename the existing Data directory
            Files.move(dataDirectory, backupDirectory);

            // Assert and log that Data directory no longer exists
            if (Files.exists(dataDirectory)) {
                ProcessingLogger.logError("Data directory still exists after rename!", null);
                System.err.println("ERROR: Data directory still exists after rename!");
            } else {
                ProcessingLogger.logInfo("Data directory successfully removed after rename.");
                System.out.println("Data directory successfully removed after rename.");
            }

            ProcessingLogger.logInfo("=== FORCE RESTART EXECUTED ===");
            ProcessingLogger.logInfo("Existing Data directory renamed to: " + backupDirName);
            ProcessingLogger.logInfo("Starting fresh with new Data directory");
            System.out.println("Force restart: Renamed existing Data directory to " + backupDirName);
            
        } catch (Exception e) {
            String errorMsg = "CRITICAL ERROR: Failed to rename Data directory during force restart.";
            String detailMsg = "Cannot proceed with force restart - existing data could not be backed up safely.";
            String actionMsg = "Please manually resolve the issue with the Data directory and try again.";
            
            ProcessingLogger.logError(errorMsg, e);
            ProcessingLogger.logError("Details: " + e.getMessage(), null);
            ProcessingLogger.logError(detailMsg, null);
            ProcessingLogger.logError(actionMsg, null);
            
            System.err.println("=== FORCE RESTART FAILED ===");
            System.err.println(errorMsg);
            System.err.println("Details: " + e.getMessage());
            System.err.println(detailMsg);
            System.err.println(actionMsg);
            System.err.println("Application will now exit.");
            
            // Exit immediately - force restart must succeed or fail completely
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
            // Handle force restart before setting up directories
            handleForceRestart(context);
            
            _setUpDirectories();

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
             Path zipFile = fileDownloadService.downloadLatestIAPDData(Config.FIRM_FILE_PATH);
            context.incrementSuccessfulDownloads();
            
            // Extract the ZIP file
            Path extractedFile =  fileDownloadService.extractGZFile(zipFile, Config.FIRM_FILE_PATH);
            
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
