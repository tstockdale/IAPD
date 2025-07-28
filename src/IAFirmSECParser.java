import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.regex.Matcher;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

/**
 * Investment Adviser Public Disclosure (IAPD) Parser
 * 
 * This application parses SEC IAPD XML feed files and processes PDF brochures
 * to extract relevant information about investment advisory firms.
 */
public class IAFirmSECParser {
    
    private final BrochureAnalyzer brochureAnalyzer;
    
    public IAFirmSECParser() {
        this.brochureAnalyzer = new BrochureAnalyzer();
    }
    
    /**
     * Main entry point for the application
     */
    public static void main(String[] args) {
        IAFirmSECParser parser = new IAFirmSECParser();
        try {
            // Uncomment the desired operation
            parser.downloadAndProcessLatestIAPDData();
            //parser.processXMLFile();
            //parser.processBrochures();
            // parser.processOnePDF();
            // parser.processCustodialServices();
        } catch (Exception e) {
            System.err.println("Error in main execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Downloads the latest IAPD data file and processes it
     */
    public void downloadAndProcessLatestIAPDData() {
        try {
            System.out.println("Downloading latest IAPD data from SEC website...");
            
            // Download the latest ZIP file
            File zipFile = HttpUtils.downloadLatestIAPDData(Config.OUTPUT_FILE_PATH);
            
            // Extract the ZIP file
            File extractedFile = HttpUtils.extractGZFile(zipFile, Config.OUTPUT_FILE_PATH);
            
            if (extractedFile != null && extractedFile.exists()) {
                System.out.println("Successfully downloaded and extracted IAPD data: " + extractedFile.getName());
                System.out.println("IAPD data file location: " + extractedFile.getAbsolutePath());
                
                // Process the XML file and get the output file path
                String outputFilePath = processXMLFile(extractedFile);
                
                if (outputFilePath != null) {
                    System.out.println("XML processing completed. Output file: " + outputFilePath);
                    
                    // Process brochures using the output from XML processing
                    processBrochures(outputFilePath);
                } else {
                    System.err.println("Failed to process XML file");
                }
            } else {
                System.err.println("Failed to extract IAPD data file");
            }
            
        } catch (Exception e) {
            System.err.println("Error downloading IAPD data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Downloads a specific IAPD data file by date and processes it
     */
    public void downloadIAPDDataByDate(int year, int month) {
        try {
            System.out.println("Downloading IAPD data for " + month + "/" + year + "...");
            
            // Download the specific ZIP file
            File zipFile = HttpUtils.downloadIAPDDataByDate(year, month, Config.OUTPUT_FILE_PATH);
            
            // Extract the ZIP file
            File extractedFile = HttpUtils.extractGZFile(zipFile, Config.OUTPUT_FILE_PATH);
            
            if (extractedFile != null && extractedFile.exists()) {
                System.out.println("Successfully downloaded and extracted IAPD data: " + extractedFile.getName());
                System.out.println("IAPD data file location: " + extractedFile.getAbsolutePath());
                System.out.println("Note: This is a CSV file from SEC, not the XML format expected by processXMLFile()");
            } else {
                System.err.println("Failed to extract IAPD data file");
            }
            
        } catch (Exception e) {
            System.err.println("Error downloading IAPD data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processes the XML file containing firm data
     * @param xmlFile the XML file to process
     * @return the path of the output file that was written
     */
    public String processXMLFile(File xmlFile) {
        FileWriter firmWriter = null;
        String outputFileName = constructOutputFileName(xmlFile.getName());
        String outputFilePath = Config.BROCHURE_INPUT_PATH + outputFileName;
        try {
            firmWriter = new FileWriter(new File(outputFilePath), false);
            firmWriter.write(Config.FIRM_HEADER + System.lineSeparator());
            parseXML(xmlFile, firmWriter);
            return outputFilePath;
        } catch (Exception e) {
            System.err.println("Error processing XML file: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (firmWriter != null) {
                try {
                    firmWriter.close();
                } catch (Exception e) {
                    System.err.println("Error closing file writer: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Constructs the output file name based on the input file name
     * Parses the date from input file name and creates "IA_FIRM_SEC_DATA" + date + ".csv"
     * @param inputFileName the name of the input XML file
     * @return the constructed output file name
     */
    private String constructOutputFileName(String inputFileName) {
        try {
            // Extract date from input file name (e.g., "IA_FIRM_SEC_Feed_04_07_2025.xml")
            // Pattern matches: IA_FIRM_SEC_Feed_MM_DD_YYYY.xml
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("IA_FIRM_SEC_Feed_(\\d{2})_(\\d{2})_(\\d{4})\\.xml");
            java.util.regex.Matcher matcher = pattern.matcher(inputFileName);
            
            if (matcher.find()) {
                String month = matcher.group(1);
                String day = matcher.group(2);
                String year = matcher.group(3);
                
                // Construct output file name as "IA_FIRM_SEC_DATA_" + YYYYMMDD + ".csv"
                return "IA_FIRM_SEC_DATA" + year + month + day + ".csv";
            } else {
                // Fallback to default naming if pattern doesn't match
                System.err.println("Warning: Could not parse date from input file name: " + inputFileName);
                System.err.println("Using default output file name format");
                return "IA_FIRM_SEC_DATA_" + System.currentTimeMillis() + ".csv";
            }
        } catch (Exception e) {
            System.err.println("Error constructing output file name: " + e.getMessage());
            return "IA_FIRM_SEC_DATA_" + System.currentTimeMillis() + ".csv";
        }
    }

    /**
     * Processes the XML file containing firm data using the default input file
     * @return the path of the output file that was written
     */
    public String processXMLFile() {
        File xmlFile = new File(Config.INPUT_FILE);
        return processXMLFile(xmlFile);
    }
    
    /**
     * Parses the XML file and extracts firm information
     */
    private void parseXML(File xmlFile, FileWriter firmWriter) {
        InputStream in = null;
        try {
            in = new FileInputStream(xmlFile);
            System.out.println("Processing: " + xmlFile.getCanonicalPath());
            
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(in, Config.ENCODING);
            
            while (reader.hasNext()) {
                reader.next();
                if (reader.getEventType() == XMLStreamReader.START_ELEMENT && 
                    "Firm".equals(reader.getLocalName())) {
                    if (!processNextFirm(reader, firmWriter)) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing XML: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    System.err.println("Error closing input stream: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Processes a single firm record from the XML
     */
    private boolean processNextFirm(XMLStreamReader reader, FileWriter firmWriter) throws Exception {
        String firmCrdNb = null;
        String filingDate = null;
        
        while (reader.hasNext()) {
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
                String elementName = reader.getLocalName();
                System.out.println("Processing element: " + elementName);
                
                switch (elementName) {
                    case "Info":
                        firmCrdNb = reader.getAttributeValue("", "FirmCrdNb");
                        writeInfoAttributes(reader, firmWriter);
                        break;
                    case "Rgstn":
                        writeRegistrationAttributes(reader, firmWriter);
                        break;
                    case "Filing":
                        filingDate = reader.getAttributeValue("", "Dt");
                        writeFilingAttributes(reader, firmWriter, filingDate);
                        break;
                    case "MainAddr":
                        writeAddressAttributes(reader, firmWriter);
                        break;
                    case "Item5A":
                        writeEmployeeCount(reader, firmWriter);
                        break;
                    case "Item5F":
                        writeAUMAndAccounts(reader, firmWriter);
                        break;
                }
            } else if (reader.getEventType() == XMLStreamReader.END_ELEMENT && 
                      "Firm".equalsIgnoreCase(reader.getLocalName())) {
                String brochureURL = getBrochureURL(firmCrdNb);
                if (brochureURL != null) {
                    firmWriter.write(brochureURL);
                }
                firmWriter.write(System.lineSeparator());
                Thread.sleep(2000); // Rate limiting
                return true;
            }
            reader.next();
        }
        return false;
    }
    
    /**
     * Writes firm info attributes to CSV
     */
    private void writeInfoAttributes(XMLStreamReader reader, FileWriter writer) throws Exception {
        writer.write(getAttributeValue(reader, "SECRgnCD") + ",");
        writer.write(getAttributeValue(reader, "FirmCrdNb") + ",");
        writer.write(getAttributeValue(reader, "SECNb") + ",");
        writer.write('"' + sanitizeValue(getAttributeValue(reader, "BusNm")) + '"' + ",");
        writer.write('"' + sanitizeValue(getAttributeValue(reader, "LegalNm")) + '"' + ",");
    }
    
    /**
     * Writes registration attributes to CSV
     */
    private void writeRegistrationAttributes(XMLStreamReader reader, FileWriter writer) throws Exception {
        writer.write(getAttributeValue(reader, "FirmType") + ",");
        writer.write(getAttributeValue(reader, "St") + ",");
        writer.write(getAttributeValue(reader, "Dt") + ",");
    }
    
    /**
     * Writes filing attributes to CSV
     */
    private void writeFilingAttributes(XMLStreamReader reader, FileWriter writer, String filingDate) throws Exception {
        writer.write((filingDate != null ? filingDate : "") + ",");
        writer.write(getAttributeValue(reader, "FormVrsn") + ",");
    }
    
    /**
     * Writes address attributes to CSV
     */
    private void writeAddressAttributes(XMLStreamReader reader, FileWriter writer) throws Exception {
        writer.write('"' + sanitizeValue(getAttributeValue(reader, "Strt1")) + '"' + ",");
        writer.write('"' + sanitizeValue(getAttributeValue(reader, "Strt2")) + '"' + ",");
        writer.write('"' + sanitizeValue(getAttributeValue(reader, "City")) + '"' + ",");
        writer.write(getAttributeValue(reader, "State") + ",");
        writer.write(getAttributeValue(reader, "Cntry") + ",");
        writer.write(getAttributeValue(reader, "PostlCd") + ",");
        writer.write(getAttributeValue(reader, "PhNb") + ",");
        writer.write(getAttributeValue(reader, "FaxNb") + ",");
    }
    
    /**
     * Writes employee count to CSV
     */
    private void writeEmployeeCount(XMLStreamReader reader, FileWriter writer) throws Exception {
        writer.write(getAttributeValue(reader, "TtlEmp") + ",");
    }
    
    /**
     * Writes AUM and account information to CSV
     */
    private void writeAUMAndAccounts(XMLStreamReader reader, FileWriter writer) throws Exception {
        writer.write(getAttributeValue(reader, "Q5F2C") + ","); // AUM
        writer.write(getAttributeValue(reader, "Q5F2F") + ","); // Total Accounts
    }
    
    /**
     * Helper method to get attribute value or empty string if null
     */
    private String getAttributeValue(XMLStreamReader reader, String attributeName) {
        String value = reader.getAttributeValue("", attributeName);
        return value != null ? value : "";
    }
    
    /**
     * Helper method to sanitize string values for CSV output
     */
    private String sanitizeValue(String value) {
        return value != null ? value.replaceAll("\"", "") : "";
    }
    
    /**
     * Retrieves the brochure URL for a given firm CRD number
     */
    private String getBrochureURL(String firmCrdNb) {
        try {
            String url = String.format(Config.FIRM_API_URL_FORMAT, firmCrdNb);
            String response = HttpUtils.getHTTPSResponse(url);
            
            if (response != null) {
                Matcher matcher = PatternMatchers.API_BRCHR_VERSION_ID_PATTERN.matcher(response);
                if (matcher.find()) {
                    String brochureURL = Config.BROCHURE_URL_BASE + matcher.group(1);
                    String fileName = firmCrdNb + "_" + matcher.group(1) + ".pdf";
                    HttpUtils.downloadHTTPSFile(brochureURL, fileName);
                    return brochureURL;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting brochure URL for firm " + firmCrdNb + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Processes downloaded brochures and extracts information
     * @param inputFilePath the path to the CSV file containing firm data
     */
    public void processBrochures(String inputFilePath) {
        Reader reader = null;
        FileWriter writer = null;
        try {
            reader = new FileReader(inputFilePath);
            writer = new FileWriter(new File(Config.BROCHURE_OUTPUT_PATH + "/" + "IAPD_Found.csv"));
            
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuote('"')
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            writer.write(Config.FOUND_FILE_HEADER + System.lineSeparator());
            
            for (CSVRecord csvRecord : records) {
                processSingleBrochure(csvRecord, writer);
            }
        } catch (Exception e) {
            System.err.println("Error processing brochures: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    /**
     * Processes downloaded brochures and extracts information using the default input file
     */
    public void processBrochures() {
        String inputFilePath = Config.BROCHURE_INPUT_PATH + "/" + Config.OUTPUT_FILE_NAME;
        processBrochures(inputFilePath);
    }
    
    /**
     * Processes a single brochure record
     */
    private void processSingleBrochure(CSVRecord csvRecord, FileWriter writer) throws Exception {
        Map<String, String> recordMap = csvRecord.toMap();
        String brochureURL = recordMap.get("BrochureURL");
        String firmCrdNb = recordMap.get("FirmCrdNb");
        
        if (brochureURL == null || brochureURL.isEmpty()) {
            return;
        }
        
        Matcher matcher = PatternMatchers.BRCHR_VERSION_ID_PATTERN.matcher(brochureURL);
        if (!matcher.find()) {
            return;
        }
        
        File brochureFile = new File(Config.BROCHURE_OUTPUT_PATH + "/" + firmCrdNb + "_" + matcher.group(1) + ".pdf");
        if (!brochureFile.exists()) {
            return;
        }
        
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(brochureFile);
            String text = PdfTextExtractor.getCleanedBrochureText(stream);
            BrochureAnalysis analysis = brochureAnalyzer.analyzeBrochureContent(text);
            writeBrochureAnalysis(writer, recordMap, analysis, brochureFile.getName(), brochureURL);
        } finally {
            if (stream != null) stream.close();
        }
    }
    
    /**
     * Writes brochure analysis results to CSV
     */
    private void writeBrochureAnalysis(FileWriter writer, Map<String, String> recordMap, 
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
     * Processes custodial services information from brochures
     */
    public void processCustodialServices() {
        Reader reader = null;
        FileWriter writer = null;
        try {
            reader = new FileReader(Config.BROCHURE_INPUT_PATH + "/" + Config.OUTPUT_FILE_NAME);
            writer = new FileWriter(new File(Config.BROCHURE_OUTPUT_PATH + "/" + Config.CUSTODIAL_SERVICES_FILE));
            
            Iterable<CSVRecord> records = CSVFormat.EXCEL
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setQuote('"')
                    .setQuoteMode(QuoteMode.MINIMAL)
                    .build()
                    .parse(reader);
            
            for (CSVRecord csvRecord : records) {
                processCustodialServicesRecord(csvRecord, writer);
            }
        } catch (Exception e) {
            System.err.println("Error processing custodial services: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }
    
    /**
     * Processes a single custodial services record
     */
    private void processCustodialServicesRecord(CSVRecord csvRecord, FileWriter writer) throws Exception {
        Map<String, String> recordMap = csvRecord.toMap();
        String brochureURL = recordMap.get("BrochureURL");
        String firmCrdNb = recordMap.get("FirmCrdNb");
        
        if (brochureURL == null || brochureURL.isEmpty()) {
            return;
        }
        
        Matcher matcher = PatternMatchers.BRCHR_VERSION_ID_PATTERN.matcher(brochureURL);
        if (!matcher.find()) {
            return;
        }
        
        File brochureFile = new File(Config.BROCHURE_OUTPUT_PATH + "/" + firmCrdNb + "_" + matcher.group(1) + ".pdf");
        if (!brochureFile.exists()) {
            return;
        }
        
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(brochureFile);
            String text = PdfTextExtractor.getHeavilyCleanedBrochureText(stream);
            
            Matcher custodialMatcher = PatternMatchers.CUSTODIAL_SERVICES_PATTERN.matcher(text);
            while (custodialMatcher.find()) {
                writeCustodialServicesRecord(writer, recordMap, custodialMatcher.group(0), brochureFile.getName(), brochureURL);
            }
        } finally {
            if (stream != null) stream.close();
        }
    }
    
    /**
     * Writes custodial services record to CSV
     */
    private void writeCustodialServicesRecord(FileWriter writer, Map<String, String> recordMap, 
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
     * Processes a single PDF brochure for testing purposes
     */
    public void processOnePDF() {
        String path = "C:/Users/stoctom/Work/IAPD/Output/283630_902136.pdf";
        File file = new File(path);
        
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            String text = PdfTextExtractor.getBrochureText(fis);
            System.out.println("Extracted text length: " + text.length());
            
            BrochureAnalysis analysis = brochureAnalyzer.analyzeBrochureContent(text);
            System.out.println("Proxy Providers: " + analysis.getProxyProvider().toString());
            System.out.println("Class Action Providers: " + analysis.getClassActionProvider().toString());
            System.out.println("ESG Providers: " + analysis.getEsgProvider().toString());
            System.out.println("Email addresses found: " + analysis.getEmailSet().size());
        } catch (Exception e) {
            System.err.println("Error processing single PDF: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    System.err.println("Error closing file input stream: " + e.getMessage());
                }
            }
        }
    }
}
