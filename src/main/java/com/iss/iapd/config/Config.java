package com.iss.iapd.config;

/**
 * Configuration constants for the IAPD Parser application
 */
public final class Config {
    
    // File and path constants;
    public static final String ENCODING = "ISO-8859-1";
    public static final String FIRM_FILE_PATH = "./Data/FirmFiles";
    public static final String DOWNLOAD_PATH = "./Data/Downloads";
    public static final String BROCHURE_OUTPUT_PATH = "./Data/Output"; 
    public static final String BROCHURE_INPUT_PATH = "./Data/Input";
    public static final String LOG_PATH = "./Data/Logs";
    
    // HTTP constants
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36";
    public static final int BUFFER_SIZE = 4096;
    
    // URL constants
    public static final String FIRM_URL_BASE = "https://adviserinfo.sec.gov/Firm/167688";
    public static final String FIRM_API_URL_FORMAT = "https://api.adviserinfo.sec.gov/search/firm/%s?hl=true&nrows=12&query=&start=0&wt=json";
    public static final String BROCHURE_URL_BASE = "https://files.adviserinfo.sec.gov/IAPD/Content/Common/crd_iapd_Brochure.aspx?BRCHR_VRSN_ID=";
    public static final String BROCHURE_MONTHLY_URL_FORMAT = "https://reports.adviserinfo.sec.gov/reports/foia/advBrochures/2025/ADV_Brochures_2025_%s.zip";

    // Output file naming
    public static final String OUTPUT_FILE_BASE_NAME = "IAPD_Data";
    
    // CSV headers - Standardized to include all XML data fields
    // Base firm data fields (extracted from XML) - used in IAPD_SEC_DATA files
    public static final String FIRM_HEADER = "dateAdded,SECRgmCD,FirmCrdNb,SECMb,Business Name,Legal Name,Street 1,Street 2,City,State,Country,Postal Code,Telephone #,Fax #,Registration Firm Type,Registration State,Registration Date,Filing Date,Filing Version,Total Employees,AUM,Total Accounts,BrochureURL";
    
    // Complete IAPD data fields (firm data + FilesToDownload fields + brochure analysis) - used in final IAPD_DATA files
    public static final String IAPD_DATA_HEADER = "dateAdded,SECRgmCD,FirmCrdNb,SECMb,Business Name,Legal Name,Street 1,Street 2,City,State,Country,Postal Code,Telephone #,Fax #,Registration Firm Type,Registration State,Registration Date,Filing Date,Filing Version,Total Employees,AUM,Total Accounts,BrochureURL,brochureVersionId,brochureName,dateSubmitted,dateConfirmed,File Name,Proxy Provider,Class Action Provider,ESG Provider,ESG Investment Language,Email -- Compliance,Email -- Proxy,Email -- Brochure,Email -- Item 17,Email -- All,Does Not Vote String";
    
    // Legacy alias for backward compatibility
    public static final String FOUND_FILE_HEADER = IAPD_DATA_HEADER;
    
    /**
     * Gets the current date in mm/dd/yyyy format for the dateAdded field
     * @return current date as mm/dd/yyyy string
     */
    public static String getCurrentDateString() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return String.format("%02d/%02d/%04d", now.getMonthValue(), now.getDayOfMonth(), now.getYear());
    }
    
    // Private constructor to prevent instantiation
    private Config() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
