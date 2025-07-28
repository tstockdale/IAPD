import java.io.FileWriter;
import java.io.Writer;
import java.util.Map;

/**
 * Service class responsible for CSV writing operations
 */
public class CSVWriterService {
    
    /**
     * Writes brochure analysis results to CSV
     */
    public void writeBrochureAnalysis(Writer writer, Map<String, String> recordMap, 
                                     BrochureAnalysis analysis, String fileName, String brochureURL) throws Exception {
        writer.write(recordMap.get("SECRgmCD") + ",");
        writer.write(recordMap.get("FirmCrdNb") + ",");
        writer.write(recordMap.get("SECMb") + ",");
        writer.write('"' + recordMap.get("Business Name") + '"' + ",");
        writer.write('"' + recordMap.get("Street 1") + '"' + ",");
        writer.write('"' + recordMap.get("Street 2") + '"' + ",");
        writer.write('"' + recordMap.get("City") + '"' + ",");
        writer.write('"' + recordMap.get("State") + '"' + ",");
        writer.write('"' + recordMap.get("Country") + '"' + ",");
        writer.write('"' + recordMap.get("Postal Code") + '"' + ",");
        writer.write('"' + recordMap.get("Telephone #") + '"' + ",");
        writer.write('"' + recordMap.get("Filing Date") + '"' + ",");
        writer.write('"' + "$" + recordMap.get("AUM") + '"' + ",");
        writer.write(recordMap.get("Total Accounts") + ",");
        writer.write(recordMap.get("Total Employees") + ",");
        writer.write(analysis.getProxyProvider().toString() + ',');
        writer.write(analysis.getClassActionProvider().toString() + ',');
        writer.write(analysis.getEsgProvider().toString() + ',');
        writer.write(analysis.getEsgInvestmentLanguage().toString() + ',');
        writer.write(fileName + ",");
        writer.write(brochureURL + ',');
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
     * Writes custodial services record to CSV
     */
    public void writeCustodialServicesRecord(Writer writer, Map<String, String> recordMap, 
                                            String custodialText, String fileName, String brochureURL) throws Exception {
        writer.write('"' + custodialText.replaceAll(",", " ").replaceAll("\"", "'") + '"' + ",");
        writer.write(recordMap.get("SECRgmCD") + ",");
        writer.write(recordMap.get("FirmCrdNb") + ",");
        writer.write(recordMap.get("SECMb") + ",");
        writer.write('"' + recordMap.get("Business Name") + '"' + ",");
        writer.write('"' + recordMap.get("Street 1") + '"' + ",");
        writer.write('"' + recordMap.get("Street 2") + '"' + ",");
        writer.write('"' + recordMap.get("City") + '"' + ",");
        writer.write('"' + recordMap.get("State") + '"' + ",");
        writer.write('"' + recordMap.get("Country") + '"' + ",");
        writer.write('"' + recordMap.get("Postal Code") + '"' + ",");
        writer.write('"' + recordMap.get("Telephone #") + '"' + ",");
        writer.write('"' + recordMap.get("Filing Date") + '"' + ",");
        writer.write('"' + "$" + recordMap.get("AUM") + '"' + ",");
        writer.write(recordMap.get("Total Accounts") + ",");
        writer.write(recordMap.get("Total Employees") + ",");
        writer.write(fileName + ",");
        writer.write(brochureURL);
        writer.write(System.lineSeparator());
        writer.flush();
    }
    
    /**
     * Helper method to safely get value from record map
     */
    private String getRecordValue(Map<String, String> recordMap, String key) {
        String value = recordMap.get(key);
        return value != null ? value : "";
    }
    
    /**
     * Helper method to sanitize string values for CSV output
     */
    private String sanitizeValue(String value) {
        return value != null ? value.replaceAll("\"", "") : "";
    }
}
