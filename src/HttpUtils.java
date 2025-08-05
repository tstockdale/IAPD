import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility class for HTTP operations including file downloads and API calls
 */
public class HttpUtils {
    
    /**
     * Makes HTTPS request and returns response body
     */
    public static String getHTTPSResponse(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpsURLConnection connection = null;
        InputStream inputStream = null;
        
        try {
            // Configure SSL to trust all certificates (for development only)
            configureTrustAllSSL();
            
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", Config.USER_AGENT);
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            connection.setRequestMethod("GET");
            
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                java.util.Scanner scanner = new java.util.Scanner(inputStream, "UTF-8");
                scanner.useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                
                ProcessingLogger.logInfo("API Response received for: " + urlString);
                return responseBody;
            } else {
                System.err.println("HTTP request failed. Response code: " + connection.getResponseCode());
                throw new Exception("HTTP request failed with response code: " + connection.getResponseCode());
            }
        } finally {
            if (inputStream != null) inputStream.close();
            if (connection != null) connection.disconnect();
        }
    }
    
    /**
     * Downloads a file from HTTPS URL
     */
    public static File downloadHTTPSFile(String urlString, String fileName) throws Exception {
        String[] urlFields = urlString.split("\\?");
        URL url = new URL(urlFields[0]);
        
        HttpsURLConnection connection = null;
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        File downloadFile = null;
        
        try {
            // Configure SSL to trust all certificates (for development only)
            configureTrustAllSSL();
            
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", Config.USER_AGENT);
            connection.setDoOutput(true);
            
            if (urlFields.length > 1) {
                DataOutputStream writer = null;
                try {
                    writer = new DataOutputStream(connection.getOutputStream());
                    writer.writeBytes(urlFields[1]);
                    writer.flush();
                } finally {
                    if (writer != null) writer.close();
                }
            }
            
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                logDownloadInfo(connection, fileName);
                
                inputStream = connection.getInputStream();
                File parentFolder = new File(Config.DOWNLOAD_PATH);
                if (!parentFolder.exists()) {
                    parentFolder.mkdirs();
                }
                
                downloadFile = new File(parentFolder, fileName);
                outputStream = new FileOutputStream(downloadFile);
                
                byte[] buffer = new byte[Config.BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                ProcessingLogger.logInfo("File downloaded: " + fileName);
            } else {
                System.err.println("Download failed. Server response: " + connection.getResponseCode());
            }
        } finally {
            closeResources(outputStream, inputStream, connection);
        }
        
        return downloadFile;
    }
    
    /**
     * Configures SSL to trust all certificates (for development only)
     */
    private static void configureTrustAllSSL() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }
    
    /**
     * Logs download information
     */
    private static void logDownloadInfo(HttpsURLConnection connection, String fileName) {
        ProcessingLogger.logInfo("Content-Type = " + connection.getContentType());
        ProcessingLogger.logInfo("Content-Disposition = " + connection.getHeaderField("Content-Disposition"));
        ProcessingLogger.logInfo("Content-Length = " + connection.getContentLength());
        ProcessingLogger.logInfo("fileName = " + fileName);
    }
    
    /**
     * Closes resources safely
     */
    private static void closeResources(FileOutputStream outputStream, InputStream inputStream, HttpsURLConnection connection) {
        try {
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (connection != null) connection.disconnect();
        } catch (Exception e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
    
    /**
     * Downloads the latest IAPD XML data file from SEC website
     * @param outputPath The directory where the file should be saved
     * @return The downloaded file
     * @throws Exception if download fails
     */
    public static File downloadLatestIAPDData(String outputPath) throws Exception {
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
                File downloadedFile = downloadHTTPSFile(downloadUrl, fileName);
                
                if (downloadedFile != null && downloadedFile.exists()) {
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
     * Downloads a specific IAPD data file by date
     * @param year The year (e.g., 2025)
     * @param month The month (1-12)
     * @param outputPath The directory where the file should be saved
     * @return The downloaded file
     * @throws Exception if download fails
     */
    public static File downloadIAPDDataByDate(int year, int month, String outputPath) throws Exception {
        String monthStr = String.format("%02d", month);
        String yearStr = String.valueOf(year);
        
        // Format: ia070125.zip (July 2025)
        String fileName = String.format("IA_FIRM_SEC_Feed_ia%s01%s.xml.gz", monthStr, yearStr.substring(2));
        String downloadUrl = String.format("https://reports.adviserinfo.sec.gov/reports/CompilationReports/%s", fileName);
        
        ProcessingLogger.logInfo("Downloading IAPD data: " + downloadUrl);
        File downloadedFile = downloadHTTPSFile(downloadUrl, fileName);
        
        if (downloadedFile != null && downloadedFile.exists()) {
            ProcessingLogger.logInfo("Successfully downloaded IAPD data: " + fileName);
            return downloadedFile;
        } else {
            throw new Exception("Failed to download IAPD data file: " + fileName);
        }
    }
    
    /**
     * Extracts a GZ file to the specified directory
     * @param gzFile The GZ file to extract
     * @param extractToDir The directory to extract to
     * @return The extracted file
     * @throws Exception if extraction fails
     */
    public static File extractGZFile(File gzFile, String extractToDir) throws Exception {
        File extractDir = new File(extractToDir);
        if (!extractDir.exists()) {
            extractDir.mkdirs();
        }
        
        java.util.zip.GZIPInputStream gzIn = null;
        java.io.BufferedOutputStream bos = null;
        File extractedFile = null;
        
        try {
            gzIn = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(gzFile));
            
            // Determine the output file name by removing .gz extension
            String originalFileName = gzFile.getName();
            String extractedFileName = originalFileName.endsWith(".gz") ? 
                originalFileName.substring(0, originalFileName.length() - 3) : 
                originalFileName + ".extracted";
            
            String filePath = extractToDir + File.separator + extractedFileName;
            extractedFile = new File(filePath);
            
            bos = new java.io.BufferedOutputStream(new java.io.FileOutputStream(extractedFile));
            
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
    
    // Private constructor to prevent instantiation
    private HttpUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
