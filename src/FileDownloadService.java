import java.io.File;

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
    public File downloadBrochure(String brochureURL, String fileName) {
        try {
            return HttpUtils.downloadHTTPSFile(brochureURL, fileName);
        } catch (Exception e) {
            System.err.println("Error downloading brochure " + fileName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Downloads the latest IAPD data file from SEC website
     * @param outputPath The directory where the file should be saved
     * @return The downloaded file
     * @throws FileDownloadException if download fails
     */
    public File downloadLatestIAPDData(String outputPath) throws FileDownloadException {
        try {
            return HttpUtils.downloadLatestIAPDData(outputPath);
        } catch (Exception e) {
            throw new FileDownloadException("Failed to download latest IAPD data", e);
        }
    }
    
    /**
     * Downloads a specific IAPD data file by date
     * @param year The year (e.g., 2025)
     * @param month The month (1-12)
     * @param outputPath The directory where the file should be saved
     * @return The downloaded file
     * @throws FileDownloadException if download fails
     */
    public File downloadIAPDDataByDate(int year, int month, String outputPath) throws FileDownloadException {
        try {
            return HttpUtils.downloadIAPDDataByDate(year, month, outputPath);
        } catch (Exception e) {
            throw new FileDownloadException("Failed to download IAPD data for " + month + "/" + year, e);
        }
    }
    
    /**
     * Extracts a GZ file to the specified directory
     * @param gzFile The GZ file to extract
     * @param extractToDir The directory to extract to
     * @return The extracted file
     * @throws FileDownloadException if extraction fails
     */
    public File extractGZFile(File gzFile, String extractToDir) throws FileDownloadException {
        try {
            return HttpUtils.extractGZFile(gzFile, extractToDir);
        } catch (Exception e) {
            throw new FileDownloadException("Failed to extract GZ file: " + gzFile.getName(), e);
        }
    }
}
