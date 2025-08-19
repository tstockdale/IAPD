package com.iss.iapd.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.iss.iapd.config.CommandLineOptions;
import com.iss.iapd.config.Config;
import com.iss.iapd.config.ConfigurationManager;
import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.model.ProcessingPhase;
import com.iss.iapd.services.brochure.BrochureAnalyzer;
import com.iss.iapd.services.brochure.BrochureDownloadService;
import com.iss.iapd.services.brochure.BrochureProcessingService;
import com.iss.iapd.services.brochure.BrochureURLExtractionService;
import com.iss.iapd.services.csv.CSVWriterService;
import com.iss.iapd.services.download.FileDownloadService;
import com.iss.iapd.services.download.MonthlyDownloadService;
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
    
    private final XMLProcessingService xmlProcessingService;
    private final BrochureURLExtractionService brochureURLExtractionService;
    private final BrochureDownloadService brochureDownloadService;
    private final BrochureProcessingService brochureProcessingService;
    private final FileDownloadService fileDownloadService;
    private final ConfigurationManager configurationManager;
    private final MonthlyDownloadService monthlyDownloadService;
    
    public IAFirmSECParserRefactored() {
        // Initialize services with dependency injection
        this.fileDownloadService = new FileDownloadService();
        this.xmlProcessingService = new XMLProcessingService();
        this.brochureURLExtractionService = new BrochureURLExtractionService();
        this.brochureDownloadService = new BrochureDownloadService(fileDownloadService);
        this.configurationManager = new ConfigurationManager();
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
                
            } else if (context.isResumeDownloads()) {
                // Resume downloads mode: Skip Steps 1 & 2, resume from Step 3
                Path filesToDownloadWithStatus = handleResumeDownloads(context);
                if (filesToDownloadWithStatus == null) {
                    return; // Error already logged or no work to do
                }
                
                // Step 4: Process and analyze brochures, merge data and save as IAPD_Data
                // Find the corresponding firm data file for processing
                com.iss.iapd.services.incremental.ResumeDownloadsService resumeService = new com.iss.iapd.services.incremental.ResumeDownloadsService();
                Path filesToDownload = resumeService.findFilesToDownload();
                if (filesToDownload != null) {
                    // For resume mode, we need to find the original firm data file
                    // This is a simplified approach - in practice you might need more sophisticated matching
                    Path firmDataFile = findCorrespondingFirmDataFile();
                    if (firmDataFile != null) {
                        processBrochures(firmDataFile, filesToDownloadWithStatus, context);
                    } else {
                        ProcessingLogger.logWarning("Could not find corresponding firm data file for brochure processing");
                        ProcessingLogger.logInfo("Resume downloads completed, but skipping brochure processing step");
                    }
                } else {
                    ProcessingLogger.logWarning("Could not find FilesToDownload file for brochure processing");
                }
            } else if (context.isResumeURLExtraction()) {
                // Resume URL extraction mode: Skip Step 1, resume from Step 2
                Path filesToDownload = handleResumeURLExtraction(context);
                if (filesToDownload == null) {
                    return; // Error already logged or no work to do
                }
                
                // Step 3: Download brochure PDF files
                Path filesToDownloadWithStatus = downloadBrochures(filesToDownload, context);
                if (filesToDownloadWithStatus == null) {
                    return; // Error already logged
                }
                
                // Step 4: Process and analyze brochures, merge data and save as IAPD_Data
                // Find the corresponding firm data file for processing
                Path firmDataFile = findCorrespondingFirmDataFileForURLExtraction();
                if (firmDataFile != null) {
                    processBrochures(firmDataFile, filesToDownloadWithStatus, context);
                } else {
                    ProcessingLogger.logWarning("Could not find corresponding firm data file for brochure processing");
                    ProcessingLogger.logInfo("Resume URL extraction completed, but skipping brochure processing step");
                }
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
    
    /**
     * Handles resume URL extraction functionality
     * @param context processing context
     * @return path to FilesToDownload file, or null if failed/no work
     */
    private Path handleResumeURLExtraction(ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.EXTRACTING_BROCHURE_URLS);
            ProcessingLogger.logInfo("=== RESUME URL EXTRACTION MODE ===");
            
            com.iss.iapd.services.incremental.ResumeURLExtractionService resumeService = new com.iss.iapd.services.incremental.ResumeURLExtractionService();
            
            // Check if resume is possible
            com.iss.iapd.services.incremental.ResumeURLExtractionService.ResumeInfo resumeInfo = resumeService.checkResumeCapability();
            if (resumeInfo == null) {
                ProcessingLogger.logInfo("Resume URL extraction not possible or not needed");
                return null;
            }
            
            // Log resume statistics
            resumeService.logResumeStats(resumeInfo);
            
            // Find the corresponding firm data file to continue processing
            Path firmDataFile = resumeInfo.getFirmDataPath();
            if (firmDataFile == null || !Files.exists(firmDataFile)) {
                ProcessingLogger.logError("Could not find corresponding firm data file for resume URL extraction", null);
                return null;
            }
            
            // Resume URL extraction from the determined point
            Path outputFilePath = brochureURLExtractionService.processFirmDataForBrochures(firmDataFile.toFile(), context);
            
            if (outputFilePath != null) {
                ProcessingLogger.logInfo("Resume URL extraction completed. FilesToDownload file: " + outputFilePath);
                return outputFilePath;
            } else {
                context.setLastError("Failed to resume URL extraction");
                context.setCurrentPhase(ProcessingPhase.ERROR);
                System.err.println("Failed to resume URL extraction");
                return null;
            }
            
        } catch (Exception e) {
            context.setLastError("Error in resume URL extraction: " + e.getMessage());
            context.setCurrentPhase(ProcessingPhase.ERROR);
            System.err.println("Error in resume URL extraction: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Handles resume downloads functionality
     * @param context processing context
     * @return path to updated FilesToDownload file with download status, or null if failed/no work
     */
    private Path handleResumeDownloads(ProcessingContext context) {
        try {
            context.setCurrentPhase(ProcessingPhase.DOWNLOADING_BROCHURES);
            ProcessingLogger.logInfo("=== RESUME DOWNLOADS MODE ===");
            
            com.iss.iapd.services.incremental.ResumeDownloadsService resumeService = new com.iss.iapd.services.incremental.ResumeDownloadsService();
            
            // Check if resume is possible
            com.iss.iapd.services.incremental.ResumeDownloadsService.ResumeInfo resumeInfo = resumeService.checkResumeCapability();
            if (resumeInfo == null) {
                ProcessingLogger.logInfo("Resume downloads not possible or not needed");
                return null;
            }
            
            // Log resume statistics
            resumeService.logResumeStats(resumeInfo);
            
            // Resume downloads from the determined point
            Path outputFilePath = brochureDownloadService.downloadBrochuresFromFilesToDownloadWithResume(
                resumeInfo.getFilesToDownloadPath(), 
                resumeInfo.getFilesToDownloadWithDownloadsPath(), 
                resumeInfo.getResumeIndex(), 
                context);
            
            if (outputFilePath != null) {
                ProcessingLogger.logInfo("Resume brochure download completed. Updated FilesToDownload file: " + outputFilePath);
                return outputFilePath;
            } else {
                context.setLastError("Failed to resume brochure downloads");
                context.setCurrentPhase(ProcessingPhase.ERROR);
                System.err.println("Failed to resume brochure downloads");
                return null;
            }
            
        } catch (Exception e) {
            context.setLastError("Error in resume downloads: " + e.getMessage());
            context.setCurrentPhase(ProcessingPhase.ERROR);
            System.err.println("Error in resume downloads: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Finds the corresponding firm data file for URL extraction resume processing
     * Uses the ResumeURLExtractionService to get the correct firm data file
     * @return path to firm data file, or null if not found
     */
    private Path findCorrespondingFirmDataFileForURLExtraction() {
        try {
            com.iss.iapd.services.incremental.ResumeURLExtractionService resumeService = new com.iss.iapd.services.incremental.ResumeURLExtractionService();
            com.iss.iapd.services.incremental.ResumeURLExtractionService.ResumeInfo resumeInfo = resumeService.checkResumeCapability();
            
            if (resumeInfo != null) {
                Path firmDataFile = resumeInfo.getFirmDataPath();
                if (firmDataFile != null && Files.exists(firmDataFile)) {
                    ProcessingLogger.logInfo("Found firm data file for URL extraction resume: " + firmDataFile.getFileName());
                    return firmDataFile;
                }
            }
            
            ProcessingLogger.logWarning("Could not find firm data file for URL extraction resume");
            return null;
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error finding corresponding firm data file for URL extraction", e);
            return null;
        }
    }
    
    /**
     * Finds the corresponding firm data file for brochure processing
     * This is a simplified implementation that looks for the most recent IAPD_SEC_DATA file
     * @return path to firm data file, or null if not found
     */
    private Path findCorrespondingFirmDataFile() {
        try {
            java.io.File inputDir = new java.io.File(Config.BROCHURE_INPUT_PATH);
            if (!inputDir.exists() || !inputDir.isDirectory()) {
                ProcessingLogger.logWarning("Input directory does not exist: " + Config.BROCHURE_INPUT_PATH);
                return null;
            }
            
            // Look for IAPD_SEC_DATA files (these contain firm data without brochure URLs)
            java.io.File[] files = inputDir.listFiles((dir, name) -> 
                name.startsWith("IAPD_SEC_DATA") && name.endsWith(".csv"));
            
            if (files == null || files.length == 0) {
                ProcessingLogger.logWarning("No IAPD_SEC_DATA files found for brochure processing");
                return null;
            }
            
            // Find the most recent file by modification time
            java.io.File mostRecent = files[0];
            for (java.io.File file : files) {
                if (file.lastModified() > mostRecent.lastModified()) {
                    mostRecent = file;
                }
            }
            
            ProcessingLogger.logInfo("Found firm data file for brochure processing: " + mostRecent.getName());
            return mostRecent.toPath();
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error finding corresponding firm data file", e);
            return null;
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
