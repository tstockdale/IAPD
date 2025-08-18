package com.iss.iapd.services.csv;

import java.io.Writer;
import java.util.Map;

import com.iss.iapd.config.Config;
import com.iss.iapd.services.brochure.BrochureAnalysis;

/**
 * Service class responsible for CSV writing operations
 */
public class CSVWriterService {
    
    /**
     * Writes brochure analysis results to CSV with complete XML data and FilesToDownload fields
     * Updated to include all XML fields and FilesToDownload fields from IAPD_DATA_HEADER
     */
    public void writeBrochureAnalysis(Writer writer, Map<String, String> recordMap, 
                                     BrochureAnalysis analysis, String fileName, String brochureURL) throws Exception {
        writer.write(Config.getCurrentDateString() + ",");
        writer.write(recordMap.get("SECRgmCD") + ",");
        writer.write(recordMap.get("FirmCrdNb") + ",");
        writer.write(recordMap.get("SECMb") + ",");
        writer.write('"' + recordMap.get("Business Name") + '"' + ",");
        writer.write('"' + recordMap.get("Legal Name") + '"' + ",");  // Added Legal Name
        writer.write('"' + recordMap.get("Street 1") + '"' + ",");
        writer.write('"' + recordMap.get("Street 2") + '"' + ",");
        writer.write('"' + recordMap.get("City") + '"' + ",");
        writer.write('"' + recordMap.get("State") + '"' + ",");
        writer.write('"' + recordMap.get("Country") + '"' + ",");
        writer.write('"' + recordMap.get("Postal Code") + '"' + ",");
        writer.write('"' + recordMap.get("Telephone #") + '"' + ",");
        writer.write('"' + recordMap.get("Fax #") + '"' + ",");  // Added Fax Number
        writer.write('"' + recordMap.get("Registration Firm Type") + '"' + ",");  // Added Registration Firm Type
        writer.write('"' + recordMap.get("Registration State") + '"' + ",");  // Added Registration State
        writer.write('"' + recordMap.get("Registration Date") + '"' + ",");  // Added Registration Date
        writer.write('"' + recordMap.get("Filing Date") + '"' + ",");
        writer.write('"' + recordMap.get("Filing Version") + '"' + ",");  // Added Filing Version
        writer.write(recordMap.get("Total Employees") + ",");
        writer.write('"' + "$" + recordMap.get("AUM") + '"' + ",");
        writer.write(recordMap.get("Total Accounts") + ",");
        writer.write(brochureURL + ',');  // BrochureURL field (position 22)
        // FilesToDownload fields
        writer.write('"' + recordMap.get("brochureVersionId") + '"' + ",");
        writer.write('"' + recordMap.get("brochureName") + '"' + ",");
        writer.write('"' + recordMap.get("dateSubmitted") + '"' + ",");
        writer.write('"' + recordMap.get("dateConfirmed") + '"' + ",");
        writer.write(fileName + ",");
        writer.write(analysis.getProxyProvider().toString() + ',');
        writer.write(analysis.getClassActionProvider().toString() + ',');
        writer.write(analysis.getEsgProvider().toString() + ',');
        writer.write(analysis.getEsgInvestmentLanguage().toString() + ',');
        writer.write('"' + analysis.sanitizeForCSV(analysis.getEmailComplianceSentence().toString()) + '"' + ',');
        writer.write('"' + analysis.sanitizeForCSV(analysis.getEmailProxySentence().toString()) + '"' + ',');
        writer.write('"' + analysis.sanitizeForCSV(analysis.getEmailBrochureSentence().toString()) + '"' + ',');
        writer.write('"' + analysis.sanitizeForCSV(analysis.getEmailSentence().toString()) + '"' + ',');
        writer.write(analysis.getFormattedEmailSetString() + ',');
        writer.write('"' + analysis.sanitizeForCSV(analysis.getNoVoteString().toString()) + '"');
        writer.write(System.lineSeparator());
        writer.flush();
    }
    
    /**
     * Writes custodial services record to CSV with complete XML data
     * Updated to include all XML fields for consistency
     */
    public void writeCustodialServicesRecord(Writer writer, Map<String, String> recordMap, 
                                            String custodialText, String fileName, String brochureURL) throws Exception {
        writer.write(Config.getCurrentDateString() + ",");
        writer.write(recordMap.get("SECRgmCD") + ",");
        writer.write(recordMap.get("FirmCrdNb") + ",");
        writer.write(recordMap.get("SECMb") + ",");
        writer.write('"' + recordMap.get("Business Name") + '"' + ",");
        writer.write('"' + recordMap.get("Legal Name") + '"' + ",");  // Added Legal Name
        writer.write('"' + recordMap.get("Street 1") + '"' + ",");
        writer.write('"' + recordMap.get("Street 2") + '"' + ",");
        writer.write('"' + recordMap.get("City") + '"' + ",");
        writer.write('"' + recordMap.get("State") + '"' + ",");
        writer.write('"' + recordMap.get("Country") + '"' + ",");
        writer.write('"' + recordMap.get("Postal Code") + '"' + ",");
        writer.write('"' + recordMap.get("Telephone #") + '"' + ",");
        writer.write('"' + recordMap.get("Fax #") + '"' + ",");  // Added Fax Number
        writer.write('"' + recordMap.get("Registration Firm Type") + '"' + ",");  // Added Registration Firm Type
        writer.write('"' + recordMap.get("Registration State") + '"' + ",");  // Added Registration State
        writer.write('"' + recordMap.get("Registration Date") + '"' + ",");  // Added Registration Date
        writer.write('"' + recordMap.get("Filing Date") + '"' + ",");
        writer.write('"' + recordMap.get("Filing Version") + '"' + ",");  // Added Filing Version
        writer.write(recordMap.get("Total Employees") + ",");
        writer.write('"' + "$" + recordMap.get("AUM") + '"' + ",");
        writer.write(recordMap.get("Total Accounts") + ",");
        // Custodial services specific fields (empty for brochure analysis fields)
        writer.write(",");  // Proxy Provider
        writer.write(",");  // Class Action Provider
        writer.write(",");  // ESG Provider
        writer.write(",");  // ESG Investment Language
        writer.write(fileName + ",");
        writer.write(brochureURL + ",");
        writer.write(",");  // Email -- Compliance
        writer.write(",");  // Email -- Proxy
        writer.write(",");  // Email -- Brochure
        writer.write(",");  // Email -- Item 17
        writer.write(",");  // Email -- All
        writer.write('"' + custodialText.replaceAll(",", " ").replaceAll("\"", "'") + '"');  // Custodial text in final field
        writer.write(System.lineSeparator());
        writer.flush();
    }
}
