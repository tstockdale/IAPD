import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for downloading and extracting monthly brochure ZIP files
 * when incremental mode is used with a specific month
 */
public class MonthlyDownloadService {
    
    private final FileDownloadService fileDownloadService;
    
    public MonthlyDownloadService(FileDownloadService fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }
    
    /**
     * Downloads and extracts monthly brochure data for the specified month
     * @param monthName the month name (e.g., "january", "february")
     * @param context processing context for logging and state tracking
     * @return path to the extracted directory, or null if failed
     */
    public Path downloadAndExtractMonthlyData(String monthName, ProcessingContext context) {
        try {
            ProcessingLogger.logInfo("=== MONTHLY DOWNLOAD MODE ===");
            ProcessingLogger.logInfo("Downloading monthly brochure data for: " + monthName);
            
            // Create the monthly URL using the format from Config
            String monthlyUrl = String.format(Config.BROCHURE_MONTHLY_URL_FORMAT, monthName);
            ProcessingLogger.logInfo("Monthly download URL: " + monthlyUrl);
            
            // Create download directory if it doesn't exist
            Path downloadDir = Paths.get(Config.DOWNLOAD_PATH);
            if (!Files.exists(downloadDir)) {
                Files.createDirectories(downloadDir);
            }
            
            // Download the ZIP file
            String zipFileName = "ADV_Brochures_2025_" + monthName + ".zip";
            Path zipFilePath = downloadDir.resolve(zipFileName);
            
            ProcessingLogger.logInfo("Downloading ZIP file to: " + zipFilePath);
            boolean downloadSuccess = downloadZipFile(monthlyUrl, zipFilePath.toFile());
            
            if (!downloadSuccess) {
                ProcessingLogger.logWarning("Failed to download monthly ZIP file for " + monthName);
                context.setLastError("Failed to download monthly ZIP file for " + monthName);
                return null;
            }
            
            context.incrementSuccessfulDownloads();
            ProcessingLogger.logInfo("Successfully downloaded monthly ZIP file: " + zipFilePath);
            
            // Extract the ZIP file to Data/Downloads/[monthName]
            Path extractDir = Paths.get(Config.DOWNLOAD_PATH, monthName);
            boolean extractSuccess = extractZipFile(zipFilePath.toFile(), extractDir);
            
            if (!extractSuccess) {
                ProcessingLogger.logWarning("Failed to extract monthly ZIP file for " + monthName);
                context.setLastError("Failed to extract monthly ZIP file for " + monthName);
                return null;
            }
            
            ProcessingLogger.logInfo("Successfully extracted monthly data to: " + extractDir);
            ProcessingLogger.logInfo("Monthly download and extraction completed for: " + monthName);
            
            return extractDir;
            
        } catch (Exception e) {
            ProcessingLogger.logError("Error in monthly download for " + monthName, e);
            context.setLastError("Error in monthly download for " + monthName + ": " + e.getMessage());
            context.incrementFailedDownloads();
            return null;
        }
    }
    
    /**
     * Downloads a ZIP file from the given URL
     * @param url the URL to download from
     * @param destinationFile the file to save to
     * @return true if successful, false otherwise
     */
    private boolean downloadZipFile(String url, File destinationFile) {
        try {
            // Use HttpUtils.downloadHTTPSFile to download the ZIP file
            File downloadedFile = HttpUtils.downloadHTTPSFile(url, destinationFile.getName());
            
            if (downloadedFile != null && downloadedFile.exists()) {
                // Move the file to the correct location if needed
                if (!downloadedFile.getAbsolutePath().equals(destinationFile.getAbsolutePath())) {
                    Files.move(downloadedFile.toPath(), destinationFile.toPath());
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            ProcessingLogger.logError("Error downloading ZIP file from " + url, e);
            return false;
        }
    }
    
    /**
     * Extracts a ZIP file to the specified directory
     * @param zipFile the ZIP file to extract
     * @param extractDir the directory to extract to
     * @return true if successful, false otherwise
     */
    private boolean extractZipFile(File zipFile, Path extractDir) {
        try {
            // Create extraction directory if it doesn't exist
            if (!Files.exists(extractDir)) {
                Files.createDirectories(extractDir);
            }
            
            ProcessingLogger.logInfo("Extracting ZIP file: " + zipFile.getName() + " to: " + extractDir);
            
            try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry = zipIn.getNextEntry();
                int extractedFiles = 0;
                
                while (entry != null) {
                    Path filePath = extractDir.resolve(entry.getName());
                    
                    if (!entry.isDirectory()) {
                        // Create parent directories if they don't exist
                        Files.createDirectories(filePath.getParent());
                        
                        // Extract file
                        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                            byte[] buffer = new byte[Config.BUFFER_SIZE];
                            int len;
                            while ((len = zipIn.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        extractedFiles++;
                        
                        if (extractedFiles % 100 == 0) {
                            ProcessingLogger.logInfo("Extracted " + extractedFiles + " files...");
                        }
                    } else {
                        // Create directory
                        Files.createDirectories(filePath);
                    }
                    
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
                
                ProcessingLogger.logInfo("Successfully extracted " + extractedFiles + " files from ZIP archive");
                return true;
            }
            
        } catch (IOException e) {
            ProcessingLogger.logError("Error extracting ZIP file: " + zipFile.getName(), e);
            return false;
        }
    }
    
    /**
     * Checks if monthly data already exists for the given month
     * @param monthName the month name to check
     * @return true if data exists, false otherwise
     */
    public boolean monthlyDataExists(String monthName) {
        Path monthDir = Paths.get(Config.DOWNLOAD_PATH, monthName);
        return Files.exists(monthDir) && Files.isDirectory(monthDir);
    }
    
    /**
     * Gets the path to the monthly data directory for the given month
     * @param monthName the month name
     * @return path to the monthly data directory
     */
    public Path getMonthlyDataPath(String monthName) {
        return Paths.get(Config.DOWNLOAD_PATH, monthName);
    }
}
