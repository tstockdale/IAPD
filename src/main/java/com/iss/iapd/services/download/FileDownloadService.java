package com.iss.iapd.services.download;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.iss.iapd.config.Config;
import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.utils.HttpUtils;
import com.iss.iapd.utils.HttpUtils.HttpRequestConfig;    

/**
 * Service class responsible for file download operations
 */
public class FileDownloadService {
    
    /**
     * Downloads a brochure file from the given URL
     * @param brochureURL the URL to download from
     * @param fileName the name to save the file as
     * @return the downloaded file, or null if download failed
     */
    public Path downloadBrochure(String brochureURL, String fileName) {
        try {
            return HttpUtils.downloadHTTPSFile(brochureURL, fileName);
        } catch (Exception e) {
            System.err.println("Error downloading brochure " + fileName + ": " + e.getMessage());
            return null;
        }
    }
    
    

     /**
     * Downloads the latest IAPD XML data file from SEC website with enhanced retry logic
     */
    public Path downloadLatestIAPDData(String outputPath) throws Exception {
        HttpRequestConfig config = new HttpRequestConfig()
            .maxRetries(HttpUtils.MAX_RETRIES)
            .connectTimeout(45000)  // Longer timeout for large files
            .readTimeout(120000);   // 2 minutes for large downloads
        
        // Get current date to determine the latest available file
        java.util.Calendar currentDate = java.util.Calendar.getInstance();
        
        // Try current date first, then previous days if not available
        for (int dayOffset = 0; dayOffset <= 7; dayOffset++) {
            java.util.Calendar targetDate = (java.util.Calendar) currentDate.clone();
            targetDate.add(java.util.Calendar.DAY_OF_MONTH, -dayOffset);
            
            int month = targetDate.get(java.util.Calendar.MONTH) + 1; // Calendar months are 0-based
            int day = targetDate.get(java.util.Calendar.DAY_OF_MONTH);
            int year = targetDate.get(java.util.Calendar.YEAR);
            
            String monthStr = String.format("%02d", month);
            String dayStr = String.format("%02d", day);
            String yearStr = String.valueOf(year);
            
            // Format: IA_FIRM_SEC_Feed_07_28_2025.xml.gz
            String fileName = String.format("IA_FIRM_SEC_Feed_%s_%s_%s.xml.gz", monthStr, dayStr, yearStr);
            String downloadUrl = String.format("https://reports.adviserinfo.sec.gov/reports/CompilationReports/%s", fileName);
            
            try {
                ProcessingLogger.logInfo("Attempting to download: " + downloadUrl);
                Path downloadedFile = HttpUtils.downloadHTTPSFile(downloadUrl, fileName, config);
                
                if (downloadedFile != null && Files.exists(downloadedFile)) {
                    ProcessingLogger.logInfo("Successfully downloaded IAPD XML data: " + fileName);
                    return downloadedFile;
                }
            } catch (Exception e) {
                ProcessingLogger.logWarning("Failed to download " + fileName + ": " + e.getMessage());
                // Continue to try previous day
            }
        }
        
        throw new Exception("Could not download IAPD XML data file. No recent files available.");
    }
    


        /**
     * Downloads a specific IAPD data file by date with enhanced retry logic
     */
    public Path downloadIAPDDataByDate(int year, int month, String outputPath) throws Exception {
        HttpRequestConfig config = new HttpRequestConfig()
            .maxRetries(HttpUtils.MAX_RETRIES)
            .connectTimeout(45000)
            .readTimeout(120000);
        
        String monthStr = String.format("%02d", month);
        String yearStr = String.valueOf(year);
        
        // Format: ia070125.zip (July 2025)
        String fileName = String.format("IA_FIRM_SEC_Feed_ia%s01%s.xml.gz", monthStr, yearStr.substring(2));
        String downloadUrl = String.format("https://reports.adviserinfo.sec.gov/reports/CompilationReports/%s", fileName);
        
        ProcessingLogger.logInfo("Downloading IAPD data: " + downloadUrl);
        Path downloadedFile = HttpUtils.downloadHTTPSFile(downloadUrl, fileName, config);
        
        if (downloadedFile != null && Files.exists(downloadedFile)) {
            ProcessingLogger.logInfo("Successfully downloaded IAPD data: " + fileName);
            return downloadedFile;
        } else {
            throw new Exception("Failed to download IAPD data file: " + fileName);
        }
    }
    
    /**
     * Extracts a GZ file to the specified directory
     */
    public Path extractGZFile(Path gzFile, String extractToDir) throws Exception {
        Path extractDir = Paths.get(extractToDir);
        Files.createDirectories(extractDir);
        
        java.util.zip.GZIPInputStream gzIn = null;
        OutputStream bos = null;
        Path extractedFile = null;
        
        try {
            gzIn = new java.util.zip.GZIPInputStream(Files.newInputStream(gzFile));
            
            // Determine the output file name by removing .gz extension
            String originalFileName = gzFile.getFileName().toString();
            String extractedFileName = originalFileName.endsWith(".gz") ? 
                originalFileName.substring(0, originalFileName.length() - 3) : 
                originalFileName + ".extracted";
            
            extractedFile = extractDir.resolve(extractedFileName);
            bos = Files.newOutputStream(extractedFile);
            
            byte[] buffer = new byte[Config.BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = gzIn.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            
            ProcessingLogger.logInfo("Extracted GZ file: " + extractedFileName);
            return extractedFile;
            
        } finally {
            if (bos != null) bos.close();
            if (gzIn != null) gzIn.close();
        }
    }
    


}
