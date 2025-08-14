package com.iss.iapd.utils;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.iss.iapd.config.Config;
import com.iss.iapd.config.ProcessingLogger;
import com.iss.iapd.utils.RetryUtils;
import com.iss.iapd.utils.RateLimiter;

/**
 * Centralized HTTP utility class with enforced timeouts, user-agent, and retry/backoff logic.
 * Provides exponential backoff and proper handling of 429 (Too Many Requests) and 5xx server errors.
 */
public class HttpUtils {
    
    // HTTP Configuration Constants
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 30000; // 30 seconds
    private static final int DEFAULT_READ_TIMEOUT_MS = 60000;    // 60 seconds
    private static final int MAX_RETRIES = 5;
    private static final long BASE_RETRY_DELAY_MS = 1000;        // 1 second base delay
    private static final double BACKOFF_MULTIPLIER = 2.0;       // Exponential backoff multiplier
    private static final long MAX_RETRY_DELAY_MS = 60000;       // Maximum 60 seconds between retries
    private static final double JITTER_FACTOR = 0.1;           // 10% jitter to avoid thundering herd
    
    // Rate limiter for general HTTP requests (configurable)
    private static RateLimiter defaultRateLimiter = RateLimiter.perSecond(10); // 10 requests per second default
    
    /**
     * HTTP Response wrapper containing status code, headers, and body
     */
    public static class HttpResponse {
        private final int statusCode;
        private final String body;
        private final java.util.Map<String, java.util.List<String>> headers;
        
        public HttpResponse(int statusCode, String body, java.util.Map<String, java.util.List<String>> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
        public java.util.Map<String, java.util.List<String>> getHeaders() { return headers; }
        
        public boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }
        public boolean isRetryable() { return statusCode == 429 || (statusCode >= 500 && statusCode < 600); }
    }
    
    /**
     * HTTP Request configuration
     */
    public static class HttpRequestConfig {
        private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
        private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;
        private int maxRetries = MAX_RETRIES;
        private String userAgent = Config.USER_AGENT;
        private boolean useRateLimiter = true;
        private RateLimiter customRateLimiter = null;
        private java.util.Map<String, String> headers = new java.util.HashMap<>();
        
        public HttpRequestConfig connectTimeout(int timeoutMs) {
            this.connectTimeoutMs = timeoutMs;
            return this;
        }
        
        public HttpRequestConfig readTimeout(int timeoutMs) {
            this.readTimeoutMs = timeoutMs;
            return this;
        }
        
        public HttpRequestConfig maxRetries(int retries) {
            this.maxRetries = retries;
            return this;
        }
        
        public HttpRequestConfig userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public HttpRequestConfig disableRateLimiting() {
            this.useRateLimiter = false;
            return this;
        }
        
        public HttpRequestConfig rateLimiter(RateLimiter rateLimiter) {
            this.customRateLimiter = rateLimiter;
            return this;
        }
        
        public HttpRequestConfig header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }
        
        // Getters
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public int getMaxRetries() { return maxRetries; }
        public String getUserAgent() { return userAgent; }
        public boolean isUseRateLimiter() { return useRateLimiter; }
        public RateLimiter getCustomRateLimiter() { return customRateLimiter; }
        public java.util.Map<String, String> getHeaders() { return headers; }
    }
    
    /**
     * Sets the default rate limiter for all HTTP requests
     */
    public static void setDefaultRateLimiter(RateLimiter rateLimiter) {
        defaultRateLimiter = rateLimiter;
    }
    
    /**
     * Makes HTTPS request with centralized configuration and retry logic
     */
    public static String getHTTPSResponse(String urlString) throws Exception {
        return getHTTPSResponse(urlString, new HttpRequestConfig());
    }
    
    /**
     * Makes HTTPS request with custom configuration
     */
    public static String getHTTPSResponse(String urlString, HttpRequestConfig config) throws Exception {
        HttpResponse response = executeHttpRequest(urlString, "GET", null, config);
        if (response.isSuccess()) {
            return response.getBody();
        } else {
            throw new Exception("HTTP request failed with status code: " + response.getStatusCode());
        }
    }
    
    /**
     * Downloads a file from HTTPS URL with centralized retry and timeout logic
     */
    public static Path downloadHTTPSFile(String urlString, String fileName) throws Exception {
        return downloadHTTPSFile(urlString, fileName, new HttpRequestConfig());
    }
    
    /**
     * Downloads a file from HTTPS URL with custom configuration
     */
    public static Path downloadHTTPSFile(String urlString, String fileName, HttpRequestConfig config) throws Exception {
        String[] urlFields = urlString.split("\\?");
        String postData = urlFields.length > 1 ? urlFields[1] : null;
        
        return RetryUtils.executeWithRetry(() -> {
            try {
                return downloadFileWithRetry(urlFields[0], fileName, postData, config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, config.getMaxRetries(), BASE_RETRY_DELAY_MS, "Download file: " + fileName);
    }
    
    /**
     * Core HTTP request execution with exponential backoff and proper error handling
     */
    private static HttpResponse executeHttpRequest(String urlString, String method, String postData, HttpRequestConfig config) throws Exception {
        return RetryUtils.executeWithRetry(() -> {
            try {
                return performHttpRequest(urlString, method, postData, config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, config.getMaxRetries(), BASE_RETRY_DELAY_MS, "HTTP " + method + ": " + urlString);
    }
    
    /**
     * Performs the actual HTTP request with proper configuration
     */
    private static HttpResponse performHttpRequest(String urlString, String method, String postData, HttpRequestConfig config) throws Exception {
        // Apply rate limiting if enabled
        if (config.isUseRateLimiter()) {
            RateLimiter rateLimiter = config.getCustomRateLimiter() != null ? 
                config.getCustomRateLimiter() : defaultRateLimiter;
            rateLimiter.acquire();
        }
        
        URL url = new URL(urlString);
        HttpsURLConnection connection = null;
        InputStream inputStream = null;
        
        try {
            // Configure SSL to trust all certificates (for development only)
            configureTrustAllSSL();
            
            connection = (HttpsURLConnection) url.openConnection();
            
            // Apply centralized configuration
            configureConnection(connection, config);
            connection.setRequestMethod(method);
            
            // Handle POST data if provided
            if (postData != null && ("POST".equals(method) || "PUT".equals(method))) {
                connection.setDoOutput(true);
                try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
                    writer.writeBytes(postData);
                    writer.flush();
                }
            }
            
            int statusCode = connection.getResponseCode();
            String responseBody = "";
            
            // Read response body
            try {
                if (statusCode >= 200 && statusCode < 400) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }
                
                if (inputStream != null) {
                    try (java.util.Scanner scanner = new java.util.Scanner(inputStream, "UTF-8")) {
                        scanner.useDelimiter("\\A");
                        responseBody = scanner.hasNext() ? scanner.next() : "";
                    }
                }
            } catch (Exception e) {
                ProcessingLogger.logWarning("Failed to read response body: " + e.getMessage());
            }
            
            HttpResponse response = new HttpResponse(statusCode, responseBody, connection.getHeaderFields());
            
            // Handle specific status codes
            if (statusCode == 429) {
                // Too Many Requests - extract retry-after header if available
                String retryAfter = connection.getHeaderField("Retry-After");
                long retryDelayMs = parseRetryAfter(retryAfter);
                ProcessingLogger.logWarning("Rate limited (429). Retry after: " + retryDelayMs + "ms");
                
                if (retryDelayMs > 0) {
                    Thread.sleep(retryDelayMs);
                }
                throw new Exception("Rate limited (429): " + responseBody);
            } else if (statusCode >= 500 && statusCode < 600) {
                // Server errors - retryable
                ProcessingLogger.logWarning("Server error (" + statusCode + "): " + responseBody);
                throw new Exception("Server error (" + statusCode + "): " + responseBody);
            } else if (statusCode >= 400 && statusCode < 500) {
                // Client errors - generally not retryable (except 429 handled above)
                ProcessingLogger.logError("Client error (" + statusCode + "): " + responseBody, null);
                throw new RuntimeException("Client error (" + statusCode + "): " + responseBody);
            }
            
            if (response.isSuccess()) {
                ProcessingLogger.logInfo("HTTP request successful: " + urlString);
            }
            
            return response;
            
        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (Exception e) { /* ignore */ }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Downloads file with retry logic
     */
    private static Path downloadFileWithRetry(String urlString, String fileName, String postData, HttpRequestConfig config) throws Exception {
        // Apply rate limiting if enabled
        if (config.isUseRateLimiter()) {
            RateLimiter rateLimiter = config.getCustomRateLimiter() != null ? 
                config.getCustomRateLimiter() : defaultRateLimiter;
            rateLimiter.acquire();
        }
        
        URL url = new URL(urlString);
        HttpsURLConnection connection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        Path downloadFile = null;
        
        try {
            // Configure SSL to trust all certificates (for development only)
            configureTrustAllSSL();
            
            connection = (HttpsURLConnection) url.openConnection();
            configureConnection(connection, config);
            
            if (postData != null) {
                connection.setDoOutput(true);
                try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
                    writer.writeBytes(postData);
                    writer.flush();
                }
            }
            
            int statusCode = connection.getResponseCode();
            
            // Handle error status codes with proper retry logic
            if (statusCode == 429) {
                String retryAfter = connection.getHeaderField("Retry-After");
                long retryDelayMs = parseRetryAfter(retryAfter);
                ProcessingLogger.logWarning("Download rate limited (429). Retry after: " + retryDelayMs + "ms");
                
                if (retryDelayMs > 0) {
                    Thread.sleep(retryDelayMs);
                }
                throw new Exception("Download rate limited (429)");
            } else if (statusCode >= 500 && statusCode < 600) {
                throw new Exception("Server error during download (" + statusCode + ")");
            } else if (statusCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Download failed with status code: " + statusCode);
            }
            
            logDownloadInfo(connection, fileName);
            
            inputStream = connection.getInputStream();
            Path parentFolder = Paths.get(Config.DOWNLOAD_PATH);
            Files.createDirectories(parentFolder);
            
            downloadFile = parentFolder.resolve(fileName);
            outputStream = Files.newOutputStream(downloadFile);
            
            byte[] buffer = new byte[Config.BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            ProcessingLogger.logInfo("File downloaded successfully: " + fileName);
            return downloadFile;
            
        } finally {
            closeResources(outputStream, inputStream, connection);
        }
    }
    
    /**
     * Configures HTTP connection with centralized settings
     */
    private static void configureConnection(HttpsURLConnection connection, HttpRequestConfig config) {
        // Set timeouts
        connection.setConnectTimeout(config.getConnectTimeoutMs());
        connection.setReadTimeout(config.getReadTimeoutMs());
        
        // Set user agent
        connection.setRequestProperty("User-Agent", config.getUserAgent());
        
        // Set default headers
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "keep-alive");
        
        // Set custom headers
        for (java.util.Map.Entry<String, String> header : config.getHeaders().entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
    }
    
    /**
     * Parses Retry-After header value
     */
    private static long parseRetryAfter(String retryAfter) {
        if (retryAfter == null || retryAfter.trim().isEmpty()) {
            return calculateExponentialBackoff(1); // Default backoff if no header
        }
        
        try {
            // Try parsing as seconds
            int seconds = Integer.parseInt(retryAfter.trim());
            return Math.min(seconds * 1000L, MAX_RETRY_DELAY_MS);
        } catch (NumberFormatException e) {
            // Could be HTTP date format, but for simplicity, use default backoff
            return calculateExponentialBackoff(1);
        }
    }
    
    /**
     * Calculates exponential backoff delay with jitter
     */
    public static long calculateExponentialBackoff(int attempt) {
        if (attempt <= 0) attempt = 1;
        
        long delay = (long) (BASE_RETRY_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
        delay = Math.min(delay, MAX_RETRY_DELAY_MS);
        
        // Add jitter to avoid thundering herd problem
        double jitter = 1.0 + (Math.random() - 0.5) * 2 * JITTER_FACTOR;
        delay = (long) (delay * jitter);
        
        return Math.max(delay, BASE_RETRY_DELAY_MS);
    }
    
    /**
     * Enhanced retry utils integration with exponential backoff
     */
    public static <T> T executeWithExponentialBackoff(Supplier<T> operation, int maxRetries, String operationName) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                T result = operation.get();
                if (attempt > 1) {
                    ProcessingLogger.logInfo("Operation '" + operationName + "' succeeded on attempt " + attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                
                // Check if this is a retryable exception
                boolean isRetryable = isRetryableException(e);
                
                if (attempt <= maxRetries && isRetryable) {
                    long backoffDelay = calculateExponentialBackoff(attempt);
                    ProcessingLogger.logWarning("Operation '" + operationName + "' failed on attempt " + attempt + 
                                               ", retrying in " + backoffDelay + "ms. Error: " + e.getMessage());
                    
                    try {
                        Thread.sleep(backoffDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        ProcessingLogger.logError("Retry interrupted for operation: " + operationName, ie);
                        break;
                    }
                } else {
                    if (!isRetryable) {
                        ProcessingLogger.logError("Operation '" + operationName + "' failed with non-retryable exception", e);
                    } else {
                        ProcessingLogger.logError("Operation '" + operationName + "' failed after " + (maxRetries + 1) + " attempts", e);
                    }
                    break;
                }
            }
        }
        
        // Re-throw the last exception
        if (lastException instanceof RuntimeException) {
            throw (RuntimeException) lastException;
        } else {
            throw new RuntimeException(lastException);
        }
    }
    
    /**
     * Determines if an exception is retryable (includes HTTP-specific logic)
     */
    private static boolean isRetryableException(Exception e) {
        if (e == null) return false;
        
        // Use existing RetryUtils logic as base
        if (RetryUtils.isTransientException(e)) {
            return true;
        }
        
        // Add HTTP-specific retryable conditions
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("rate limited") ||
                   lowerMessage.contains("429") ||
                   lowerMessage.contains("server error") ||
                   lowerMessage.contains("5xx") ||
                   lowerMessage.contains("503") ||
                   lowerMessage.contains("502") ||
                   lowerMessage.contains("504");
        }
        
        return false;
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
    private static void closeResources(OutputStream outputStream, InputStream inputStream, HttpsURLConnection connection) {
        try {
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (connection != null) connection.disconnect();
        } catch (Exception e) {
            ProcessingLogger.logWarning("Error closing resources: " + e.getMessage());
        }
    }
    
    /**
     * Downloads the latest IAPD XML data file from SEC website with enhanced retry logic
     */
    public static Path downloadLatestIAPDData(String outputPath) throws Exception {
        HttpRequestConfig config = new HttpRequestConfig()
            .maxRetries(MAX_RETRIES)
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
                Path downloadedFile = downloadHTTPSFile(downloadUrl, fileName, config);
                
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
    public static Path downloadIAPDDataByDate(int year, int month, String outputPath) throws Exception {
        HttpRequestConfig config = new HttpRequestConfig()
            .maxRetries(MAX_RETRIES)
            .connectTimeout(45000)
            .readTimeout(120000);
        
        String monthStr = String.format("%02d", month);
        String yearStr = String.valueOf(year);
        
        // Format: ia070125.zip (July 2025)
        String fileName = String.format("IA_FIRM_SEC_Feed_ia%s01%s.xml.gz", monthStr, yearStr.substring(2));
        String downloadUrl = String.format("https://reports.adviserinfo.sec.gov/reports/CompilationReports/%s", fileName);
        
        ProcessingLogger.logInfo("Downloading IAPD data: " + downloadUrl);
        Path downloadedFile = downloadHTTPSFile(downloadUrl, fileName, config);
        
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
    public static Path extractGZFile(Path gzFile, String extractToDir) throws Exception {
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
    
    // Private constructor to prevent instantiation
    private HttpUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
